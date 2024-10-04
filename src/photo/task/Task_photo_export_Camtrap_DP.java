package photo.task;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONWriter;
import org.tinylog.Logger;

import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.CsvWriter.CsvWriterBuilder;
import de.siegmar.fastcsv.writer.LineDelimiter;
import photo.Photo2;
import task.Description;
import task.Role;
import task.Tag;
import task.Task;
import util.yaml.YamlMap;

@Tag("photo")
@Description("Export photo meta data to CSV files in structure of Camtrap DP.")
@Role("admin")
public class Task_photo_export_Camtrap_DP extends Task {
	
	private static final DateTimeFormatter CAMTRAP_DP_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ssXXX");
	private final ZoneId TIMEZONE = ZoneId.systemDefault();	
	
	private static final LocalDateTime MAX_DATE = LocalDateTime.parse("3000-12-31T23:59:59");
	private static final LocalDateTime MIN_DATE = LocalDateTime.parse("1000-01-01T00:00:00");

	private String localDateTimeToString(LocalDateTime localDateTime) {		
		//return localDateTime.toString()+"Z";
		//return ZonedDateTime.of(localDateTime, ZoneId.systemDefault()).toString();
		return CAMTRAP_DP_DATETIME_FORMATTER.format(ZonedDateTime.of(localDateTime, TIMEZONE));
	}

	public static String classificatorToClassificationMethod(String classificator) {
		if(classificator == null) {
			return "";
		}
		if(classificator.isBlank()) {
			return "";
		}
		switch(classificator) {
		case "expert":
			return "human";
		default:
			return "machine";
		}
	}
	
	public String getClassifiedBy(YamlMap cmap) {		
		String pre = "";
		if(cmap.contains("classificator")) {
			String c = cmap.getString("classificator");
			if(!c.equals("expert")) {
				pre = c;
			}
		}
		return pre + " " + (cmap.contains("identity") ? cmap.getString("identity") : "");			
	}

	private static final CsvWriterBuilder CSV_BUILDER = CsvWriter.builder()
			.fieldSeparator(',')
			.quoteCharacter('"')
			.quoteStrategy(null) // quote when needed only
			.commentCharacter('#')
			.lineDelimiter(LineDelimiter.CRLF);

	@Override
	public void run() {

		Path output_folder_path = Paths.get("photo_output", "metadata");
		File output_folder_file = output_folder_path.toFile();
		if(!output_folder_file.mkdirs()) {
			if(!output_folder_file.exists()) {
				throw new RuntimeException("output folder not created");
			}
		}		

		HashSet<String> scientificNameSet = new HashSet<String>();
		LocalDateTime[] deplMin = new LocalDateTime[] {MAX_DATE};
		LocalDateTime[] deplMax = new LocalDateTime[] {MIN_DATE};		
		HashMap<String, LocalDateTime[]> rangeMap = new HashMap<String, LocalDateTime[]>();

		try (
				CsvWriter mediaCSV = CSV_BUILDER.build(output_folder_path.resolve("media" + ".csv"));
				CsvWriter observationsCSV = CSV_BUILDER.build(output_folder_path.resolve("observations" + ".csv"));
				) {
			mediaCSV.writeRecord(
					"mediaID", 
					"deploymentID",
					"captureMethod",
					"timestamp", 
					"filePath", 
					"filePublic",
					"fileName",
					"fileMediatype",
					"exifData",
					"favorite",
					"mediaComments"
					);
			observationsCSV.writeRecord(
					"observationID", 
					"deploymentID", 
					"mediaID", 
					"eventID",
					"eventStart", 
					"eventEnd", 
					"observationLevel", 
					"observationType",
					"cameraSetupType",
					"scientificName",
					"count",
					"lifeStage",
					"sex",
					"behavior",
					"individualID",
					"individualPositionRadius",
					"individualPositionAngle",
					"individualSpeed",					
					"bboxX",
					"bboxY",
					"bboxWidth",
					"bboxHeight",
					"classificationMethod",
					"classifiedBy",
					"classificationTimestamp",
					"classificationProbability",
					"observationTags",
					"observationComments"
					);
			
			ctx.broker.photodb2().foreachIdNotLocked(photo_id -> {
				Photo2 photo = ctx.broker.photodb2().getPhoto2(photo_id, true);
				if(photo != null) {
					Logger.info(photo.id);
					String mediaID = photo.id;
					String deploymentID = photo.location;
					String captureMethod = ""; // optional
					String timestamp = localDateTimeToString(photo.date);
					String filePath = photo.imagePath.toString();
					String filePublic = false ? "true" : "false";
					String fileName = ""; // optional
					String fileMediatype = "image/jpeg";
					String exifData = ""; // optional
					String favorite = ""; // optional
					String mediaComments = ""; // optional
					
					if(deplMin[0].isAfter(photo.date)) {
						deplMin[0] = photo.date; 
					}
					if(deplMax[0].isBefore(photo.date)) {
						deplMax[0] = photo.date; 
					}
					
					LocalDateTime[] range = rangeMap.get(photo.location);
					if(range == null) {
						range = new LocalDateTime[] {MAX_DATE, MIN_DATE};
						rangeMap.put(photo.location, range);
					}
					if(range[0].isAfter(photo.date)) {
						range[0] = photo.date; 
					}
					if(range[1].isBefore(photo.date)) {
						range[1] = photo.date; 
					}

					mediaCSV.writeRecord(
							mediaID, 
							deploymentID, 
							captureMethod,
							timestamp, 
							filePath, 
							filePublic, 
							fileName,
							fileMediatype,
							exifData,
							favorite,
							mediaComments
							);


					int[] obsCnt = new int[]{0};
					String eventStart = timestamp;
					String eventEnd = eventStart;
					String observationLevel = "media";
					photo.foreachDetection(map -> {

						String bboxX = ""; // optional
						String bboxY = ""; // optional
						String bboxWidth = ""; // optional
						String bboxHeight = ""; // optional

						if(map.contains("bbox")) {
							float[] bbox = map.getList("bbox").asFloatArray();
							if(bbox.length == 4) {
								bboxX = Float.toString(bbox[0]);
								bboxY = Float.toString(bbox[1]);
								bboxWidth = Float.toString(bbox[2]);
								bboxHeight = Float.toString(bbox[3]);
							}
						}

						String _bboxX = bboxX;
						String _bboxY = bboxY;
						String _bboxWidth = bboxWidth;
						String _bboxHeight = bboxHeight;

						map.optList("classifications").asMaps().forEach(cmap -> {

							String observationID = mediaID + "__" + obsCnt[0]++;
							String eventID = ""; // optional							
							String observationType = "unknown"; // TODO	
							String cameraSetupType = ""; // optional
							String scientificName = cmap.contains("classification") ? cmap.getString("classification") : ""; // optional
							String count = ""; // optional
							String lifeStage = ""; // optional
							String sex = ""; // optional
							String behavior = ""; // optional
							String individualID = ""; // optional
							String individualPositionRadius = ""; // optional
							String individualPositionAngle = ""; // optional
							String individualSpeed = ""; // optional							
							String classificationMethod = cmap.contains("classificator") ? classificatorToClassificationMethod(cmap.getString("classificator")) : ""; // optional							
							String classifiedBy = getClassifiedBy(cmap); // optional							
							String classificationTimestamp = cmap.contains("date") ? localDateTimeToString(cmap.getLocalDateTime("date")) : ""; // optional							
							String classificationProbability = cmap.contains("conf") ? cmap.getString("conf") : ""; // optional							
							String observationTags = ""; // optional
							String observationComments = ""; // optional

							observationsCSV.writeRecord(
									observationID, 
									deploymentID, 
									mediaID, // optional
									eventID,
									eventStart, 
									eventEnd, 
									observationLevel, 
									observationType,
									cameraSetupType,
									scientificName,
									count,
									lifeStage,
									sex,
									behavior,
									individualID,
									individualPositionRadius,
									individualPositionAngle,
									individualSpeed,
									_bboxX,
									_bboxY,
									_bboxWidth,
									_bboxHeight,
									classificationMethod,
									classifiedBy,
									classificationTimestamp,
									classificationProbability,
									observationTags,
									observationComments									
									);

							scientificNameSet.add(scientificName);
						});


					});

				} else {
					Logger.warn("photo not found: " + photo_id);
				}
			});
		} catch (IOException e) {
			Logger.warn(e);
		}


		try (				
				CsvWriter deploymentsCSV = CSV_BUILDER.build(output_folder_path.resolve("deployments" + ".csv"));				
				) {
			deploymentsCSV.writeRecord(
					"deploymentID",
					"locationID",
					"locationName",
					"latitude",
					"longitude",
					"coordinateUncertainty",
					"deploymentStart",
					"deploymentEnd",
					"setupBy",
					"cameraID",
					"cameraModel",
					"cameraDelay",
					"cameraHeight",
					"cameraDepth",
					"cameraTilt",
					"cameraHeading",
					"detectionDistance",
					"timestampIssues",
					"baitUse",
					"featureType",
					"habitat",
					"deploymentGroups",
					"deploymentTags",
					"deploymentComments"
					);
			ctx.broker.photodb2().foreachProject(projectConfig -> {
				ctx.broker.photodb2().foreachLocation(projectConfig.project, location -> {
					String deploymentID = location;
					String locationID = location; // optional
					String locationName = location; // optional
					String latitude = Double.toString(0);
					String longitude = Double.toString(0);
					String coordinateUncertainty = Integer.toString(1); // optional
					String deploymentStart = localDateTimeToString(rangeMap.get(location)[0]);
					String deploymentEnd = localDateTimeToString(rangeMap.get(location)[1]);
					String setupBy = ""; // optional
					String cameraID = ""; // optional
					String cameraModel = ""; // optional
					String cameraDelay = ""; // optional
					String cameraHeight = ""; // optional
					String cameraDepth = ""; // optional
					String cameraTilt = ""; // optional
					String cameraHeading = ""; // optional
					String detectionDistance = ""; // optional
					String timestampIssues = ""; // optional
					String baitUse = ""; // optional
					String featureType = ""; // optional
					String habitat = ""; // optional
					String deploymentGroups = ""; // optional
					String deploymentTags = ""; // optional
					String deploymentComments = ""; // optional
					deploymentsCSV.writeRecord(
							deploymentID,
							locationID,
							locationName,
							latitude,
							longitude,
							coordinateUncertainty,
							deploymentStart,
							deploymentEnd,
							setupBy,
							cameraID,
							cameraModel,
							cameraDelay,
							cameraHeight,
							cameraDepth,
							cameraTilt,
							cameraHeading,
							detectionDistance,
							timestampIssues,
							baitUse,
							featureType,
							habitat,
							deploymentGroups,
							deploymentTags,
							deploymentComments
							);
				});
			});			
		} catch (IOException e) {
			Logger.warn(e);
		}


		try (				
				FileWriter fileWriter = new FileWriter(output_folder_path.resolve("datapackage" + ".json").toFile(), StandardCharsets.UTF_8)
				) {
			JSONWriter json = new JSONWriter(fileWriter);
			json.object(); // main object			
			json.key("resources");
			json.array();
			json.object();
			json.key("name");
			json.value("deployments");
			json.key("path");
			json.value("deployments.csv");
			json.key("profile");
			json.value("tabular-data-resource");
			json.key("format");
			json.value("csv");
			json.key("mediatype");
			json.value("text/csv");
			json.key("encoding");
			json.value("utf-8");
			json.key("schema");
			json.value("https://raw.githubusercontent.com/tdwg/camtrap-dp/1.0.1/deployments-table-schema.json");
			json.endObject();
			json.object();
			json.key("name");
			json.value("media");
			json.key("path");
			json.value("media.csv");
			json.key("profile");
			json.value("tabular-data-resource");
			json.key("format");
			json.value("csv");
			json.key("mediatype");
			json.value("text/csv");
			json.key("encoding");
			json.value("utf-8");
			json.key("schema");
			json.value("https://raw.githubusercontent.com/tdwg/camtrap-dp/1.0.1/media-table-schema.json");
			json.endObject();
			json.object();
			json.key("name");
			json.value("observations");
			json.key("path");
			json.value("observations.csv");
			json.key("profile");
			json.value("tabular-data-resource");
			json.key("format");
			json.value("csv");
			json.key("mediatype");
			json.value("text/csv");
			json.key("encoding");
			json.value("utf-8");
			json.key("schema");
			json.value("https://raw.githubusercontent.com/tdwg/camtrap-dp/1.0.1/observations-table-schema.json");
			json.endObject();
			json.endArray();
			json.key("profile");
			json.value("https://raw.githubusercontent.com/tdwg/camtrap-dp/1.0.1/camtrap-dp-profile.json");
			json.key("created");
			json.value(LocalDateTime.now().toString());
			json.key("contributors");
			json.array();
			json.object();
			json.key("title");
			json.value("PhotoDB");
			json.endObject();
			json.endArray();
			json.key("project");
			json.object();
			json.key("title");
			json.value("PhotoDB");
			json.key("samplingDesign");
			json.value("opportunistic");
			json.key("captureMethod");
			json.array();
			json.value("activityDetection");
			json.endArray();
			json.key("individualAnimals");
			json.value(false);
			json.key("observationLevel");
			json.array();
			json.value("media");
			json.endArray();
			json.endObject();
			json.key("spatial");
			json.object();		
			json.key("type");
			json.value("Point"); // TODO
			json.key("coordinates");
			json.array();
			json.value(0); // TODO
			json.value(0); // TODO
			json.endArray();
			json.endObject();
			json.key("temporal");
			json.object();
			json.key("start");
			json.value(localDateTimeToString(deplMin[0]));
			json.key("end");
			json.value(localDateTimeToString(deplMax[0]));
			json.endObject();
			json.key("taxonomic");
			json.array();
			for(String scientificName : scientificNameSet) {
				json.object();	
				json.key("scientificName");
				json.value(scientificName);
				json.endObject();
			}			
			json.endArray();
			json.endObject(); // main object
		} catch (IOException e) {
			Logger.warn(e);
		}
	}
}
