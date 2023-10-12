package photo.task;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.tinylog.Logger;

import photo.Photo2;
import photo.PhotoDB2;
import photo.PhotoProjectConfig;
import task.Cancelable;
import task.Description;
import task.Descriptor.Param.Type;
import task.Param;
import task.Role;
import task.Tag;
import task.Task;
import util.JsonUtil;

@Tag("photo")
@Description("Insert MegaDetector detections. Detections ")
@Param(name = "filename", type = Type.STRING, preset = "megadetector_output.json", description = "Location of MegaDetector detections result JSON file at the server.")
@Cancelable
@Role("admin")
public class Task_photo_insert_megadetector_detections extends Task {

	@Override
	public void run() throws IOException {
		String filename = this.ctx.getParamString("filename");
		File file = new File(filename);
		if(!file.exists()) {
			throw new RuntimeException("JSON file not found");
		}
		JSONObject json = JsonUtil.read(file);
		JSONObject jsonDetectionCategories = json.getJSONObject("detection_categories");
		HashMap<String, String> detectionCategoryMap = new HashMap<String, String>();
		for(String key : jsonDetectionCategories.keySet()) {
			String value = jsonDetectionCategories.getString(key);
			detectionCategoryMap.put(key, value);
		}
		JSONObject jsonInfo = json.getJSONObject("info");
		String detectionDate = jsonInfo.getString("detection_completion_time");
		JSONObject jsonDetectorMetadata = jsonInfo.getJSONObject("detector_metadata");
		String detectionVersion = jsonDetectorMetadata.getString("megadetector_version");

		Collection<PhotoProjectConfig> projects = ctx.broker.config().photoConfig.projectMap.values();
		if(projects.isEmpty()) {
			throw new RuntimeException("No photo project config in PhotoDB");
		}
		if(projects.size() > 1) {
			throw new RuntimeException("More than one photo project config.");
		}
		String project = projects.iterator().next().project;
		
		PhotoDB2 photodb2 = ctx.broker.photodb2();

		JSONArray jsonImages = json.getJSONArray("images");
		int jsonImagesLen = jsonImages.length();
		long imagesProcessedCounter = 0;
		long imagesErrorCounter = 0;
		long classificationsProcessedCounter = 0;
		long classificationsDuplicateCounter = 0;
		for (int i = 0; i < jsonImagesLen; i++) {
			try {
				JSONObject jsonImage = jsonImages.getJSONObject(i);
				String imageFile = jsonImage.getString("file");	
				try {
					String photoID = PhotoDB2.imageRelPathToID(project, imageFile);			
					setMessage(imageFile + " -> " + photoID);
					Photo2 photo = photodb2.getPhoto2(photoID, false);
					if(photo != null) {
						JSONArray jsonDetections = jsonImage.getJSONArray("detections");
						int jsonDetectionsLen = jsonDetections.length();
						for (int j = 0; j < jsonDetectionsLen; j++) {
							JSONObject jsonDetection = jsonDetections.getJSONObject(j);
							String category = jsonDetection.getString("category");
							float conf = jsonDetection.getFloat("conf");
							JSONArray jsonBbox = jsonDetection.getJSONArray("bbox");
							float bbox0 = jsonBbox.getFloat(0);
							float bbox1 = jsonBbox.getFloat(1);
							float bbox2 = jsonBbox.getFloat(2);
							float bbox3 = jsonBbox.getFloat(3);
							float[] bbox = new float[] {bbox0, bbox1, bbox2, bbox3};
							String classification = detectionCategoryMap.getOrDefault(category, "unknown");
							String classificator = "MegaDetector";
							String identity = detectionVersion;
							String date = detectionDate;
							if(photo.setClassification(bbox, classification, classificator, identity, date, conf)) {
								classificationsProcessedCounter++;
							} else {
								classificationsDuplicateCounter++;
							}
						}
						photodb2.refreshPhotoDBentry(photo, null);
						imagesProcessedCounter++;
					} else {
						setMessage("Image not found: " + imageFile);
						imagesErrorCounter++;
					}
				} catch(Exception e) {
					setMessage("Error in reading JSON image entry : " + i + "   " + imageFile);
					Logger.warn(e);
					imagesErrorCounter++;
				}
			} catch(Exception e) {
				setMessage("Error in reading JSON image entry : " + i);
				Logger.warn(e);
				imagesErrorCounter++;
			}
		}
		setMessage(imagesProcessedCounter + " images processed,   " + imagesErrorCounter + " images error,   " + classificationsProcessedCounter + " classifications processed,   " + classificationsDuplicateCounter + " classification duplicates");
	}
}
