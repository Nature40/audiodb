package photo.task;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
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

@Tag("photo")
@Description("Export photo meta data to CSV files in structure of Camtrap DP.")
@Role("admin")
public class Task_photo_export_Camtrap_DP extends Task {

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

		try (
				CsvWriter mediaCSV = CSV_BUILDER.build(output_folder_path.resolve("media" + ".csv"));
				CsvWriter observationsCSV = CSV_BUILDER.build(output_folder_path.resolve("observations" + ".csv"));
				) {
			mediaCSV.writeRecord(
					"mediaID", 
					"deploymentID", 
					"timestamp", 
					"filePath", 
					"filePublic", 
					"fileMediatype"
					);
			observationsCSV.writeRecord(
					"observationID", 
					"deploymentID", 
					"mediaID", 
					"eventStart", 
					"eventEnd", 
					"observationLevel", 
					"observationType",
					"scientificName",
					"bboxX",
					"bboxY",
					"bboxWidth",
					"bboxHeight",
					"classificationMethod",
					"classifiedBy",
					"classificationTimestamp",
					"classificationProbability",
					"observationComments"
					);
			ctx.broker.photodb2().foreachIdNotLocked(photo_id -> {
				Photo2 photo = ctx.broker.photodb2().getPhoto2(photo_id, true);
				if(photo != null) {
					Logger.info(photo.id);
					String mediaID = photo.id;
					String deploymentID = photo.location;
					String timestamp = photo.date.toString();
					String filePath = photo.imagePath.toString();
					String filePublic = false ? "true" : "false";
					String fileMediatype = "image/jpeg";
					String observationComments = "comment"; // TODO
					mediaCSV.writeRecord(
							mediaID, 
							deploymentID, 
							timestamp, 
							filePath, 
							filePublic, 
							fileMediatype
							);

					int[] obsCnt = new int[]{0};
					String eventStart = timestamp;
					String eventEnd = eventStart;
					String observationLevel = "media";
					photo.foreachDetection(map -> {

						String bboxX = "";
						String bboxY = "";
						String bboxWidth = "";
						String bboxHeight = "";

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
							String observationType = "unknown"; // TODO							
							String scientificName = cmap.contains("classification") ? cmap.getString("classification") : "";							
							String classificationMethod = cmap.contains("classificator") ? cmap.getString("classificator") : "";							
							String classifiedBy = cmap.contains("identity") ? cmap.getString("identity") : "";							
							String classificationTimestamp = cmap.contains("date") ? cmap.getLocalDateTime("date").toString() : "";							
							String classificationProbability = cmap.contains("conf") ? cmap.getString("conf") : "";

							observationsCSV.writeRecord(
									observationID, 
									deploymentID, 
									mediaID, 
									eventStart, 
									eventEnd, 
									observationLevel, 
									observationType,
									scientificName,
									_bboxX,
									_bboxY,
									_bboxWidth,
									_bboxHeight,
									classificationMethod,
									classifiedBy,
									classificationTimestamp,
									classificationProbability,
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
					"deploymentStart",
					"deploymentEnd"
					);
			ctx.broker.photodb2().foreachProject(projectConfig -> {
				ctx.broker.photodb2().foreachLocation(projectConfig.project, location -> {
					String deploymentID = location;
					String locationID = location;
					String locationName = location;
					String latitude = Double.toString(0);
					String longitude = Double.toString(0);
					String deploymentStart = "2000-01-01T00:00:00Z"; // TODO
					String deploymentEnd = "2000-01-01T00:00:00Z"; // TODO
					deploymentsCSV.writeRecord(
							deploymentID,
							locationID,
							locationName,
							latitude,
							longitude,
							deploymentStart,
							deploymentEnd
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
			json.value("2000-01-01"); // TODO
			json.key("end");
			json.value("2000-01-01"); // TODO
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
