package audio;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import org.tinylog.Logger;

import audio.LabelStoreConnector.TlLabelStoreConnector;
import de.siegmar.fastcsv.writer.CsvWriter;
import util.AudioTimeUtil;

public class LabelStore {

	private final Broker broker;
	public final TlLabelStoreConnector tlLabelStoreConnector;

	public LabelStore(Broker broker) {
		this.broker = broker;
		tlLabelStoreConnector = new TlLabelStoreConnector("jdbc:h2:./label_store");
		tlLabelStoreConnector.get().init(false);
	}

	public void rebuild() {
		LabelStoreConnector conn = tlLabelStoreConnector.get();
		conn.init(true);
		
		Path root_data_path = broker.config().audioConfig.root_data_path;
		
		broker.sampleStorage().forEachOrderedSample(sample -> {
		//broker.sampleManager().forEach(sample -> {
			Path path = root_data_path.relativize(sample.samplePath);
			int sampleMapId = conn.getOrCreateIdBySample(path.toString());
			Logger.info(sample.id);
			int location = sample.hasLocation() ? conn.getOrCreateIdByLocation(sample.location) : sample.hasDevice() ? conn.getOrCreateIdByLocation(sample.device) : conn.getOrCreateIdByLocation("");
			int time = sample.hasTimestamp() ? (int) sample.timestamp : 0;
			for(Label label : sample.getLabels()) {
				float start = (float) label.start;
				float end = (float) label.end;
				for(GeneratorLabel lbl : label.generatorLabels) {
					int labelMapId = conn.getOrCreateIdByLabel(lbl.name);
					float reliability = (float) lbl.reliability;					
					conn.insertGeneratorLabel(sampleMapId, labelMapId, reliability, location, time, start, end);
				}
				for(UserLabel lbl : label.userLabels) {
					int labelMapId = conn.getOrCreateIdByLabel(lbl.name);
					int creatorMapId = conn.getOrCreateIdByCreator(lbl.creator);
					int creationTime = 0;
					if(lbl.creation_date != null && !lbl.creation_date.isEmpty()) {
						try {
							LocalDateTime creation_datetime = LocalDateTime.parse(lbl.creation_date);
							creationTime = (int) AudioTimeUtil.toAudiotime(creation_datetime);
						} catch(Exception e) {
							Logger.warn(e);
						}
					}
					conn.insertUserLabel(sampleMapId, labelMapId, location, time, start, end, creatorMapId, creationTime);
				}
			}
			try {
				String rawMeta = sample.getRawMeta();
				conn.insertMeta(sampleMapId, rawMeta);
				Logger.info("inserted raw");
			} catch (Exception e) {
				Logger.warn(e);
			}
		});

		String output_folder = "output";
		File output_file = new File(output_folder);
		if(!output_file.mkdirs()) {
			if(!output_file.exists()) {
				throw new RuntimeException("output folder not created");
			}
		}
		Path output_path = output_file.toPath();

		try (CsvWriter csv = CsvWriter.builder().build(output_path.resolve("generator_label.csv"))) {
			csv.writeRow("location", "time", "label", "reliability", "start", "end", "sample");		    
			conn.forEachGeneratorLabel((int id, int label, float reliability, int location, int time, float start, float end) -> {				
				String labelName = conn.getLabelById(label);
				String reliabilityName = "" + reliability;
				String locationName = conn.getLocationById(location);
				String timeName = AudioTimeUtil.ofAudiotime(time).toString();
				String startName = "" + start;
				String endName = "" + end;
				String sampleName = conn.getSampleById(id);
				csv.writeRow(locationName, timeName, labelName, reliabilityName , startName, endName, sampleName);
			});		    
		} catch (IOException e) {
			Logger.warn(e);
		}

		try (CsvWriter csv = CsvWriter.builder().build(output_path.resolve("user_label.csv"))) {
			csv.writeRow("location", "time", "label", "start", "end", "creator", "creation_time", "sample");		    
			conn.forEachUserLabel((int id, int label, int location, int time, float start, float end, int creator, int creation_time) -> {
				String labelName = conn.getLabelById(label);
				String locationName = conn.getLocationById(location);
				String timeName = AudioTimeUtil.ofAudiotime(time).toString();
				String startName = "" + start;
				String endName = "" + end;
				String creatorName = conn.getCreatorById(creator);
				String creationTimeName = AudioTimeUtil.ofAudiotime(creation_time).toString();
				String sampleName = conn.getSampleById(id);
				csv.writeRow(locationName, timeName, labelName, startName, endName, creatorName, creationTimeName, sampleName);
			});		    
		} catch (IOException e) {
			Logger.warn(e);
		}

		try (CsvWriter csv = CsvWriter.builder().build(output_path.resolve("location.csv"))) {
			csv.writeRow("location");		    
			conn.forEachLocation((int id, String location) -> {
				csv.writeRow(location);
			});		    
		} catch (IOException e) {
			Logger.warn(e);
		}

		try (CsvWriter csv = CsvWriter.builder().build(output_path.resolve("label.csv"))) {
			csv.writeRow("label");		    
			conn.forEachLabel((int id, String label) -> {
				csv.writeRow(label);
			});		    
		} catch (IOException e) {
			Logger.warn(e);
		}

		try (CsvWriter csv = CsvWriter.builder().build(output_path.resolve("creator.csv"))) {
			csv.writeRow("creator");		    
			conn.forEachCreator((int id, String creator) -> {
				csv.writeRow(creator);
			});		    
		} catch (IOException e) {
			Logger.warn(e);
		}
		
		try (CsvWriter csv = CsvWriter.builder().build(output_path.resolve("user_label_label.csv"))) {
			csv.writeRow("label", "count");
			ResultSet res = conn.conn.createStatement().executeQuery("SELECT LABEL, COUNT(*) FROM USER_LABEL_STORE GROUP BY LABEL ORDER BY COUNT(*) DESC");
			while(res.next()) {
				int label = res.getInt(1);
				int count = res.getInt(2);
				String labelName = conn.getLabelById(label);
				csv.writeRow(labelName, ""+count);
			}
		} catch (SQLException | IOException e) {
			Logger.warn(e);
		}
		
		try (CsvWriter csv = CsvWriter.builder().build(output_path.resolve("user_label_location.csv"))) {
			csv.writeRow("location", "count");
			ResultSet res = conn.conn.createStatement().executeQuery("SELECT LOCATION, COUNT(*) FROM USER_LABEL_STORE GROUP BY LOCATION ORDER BY COUNT(*) DESC");
			while(res.next()) {
				int location = res.getInt(1);
				int count = res.getInt(2);
				String locationName = conn.getLocationById(location);
				csv.writeRow(locationName, ""+count);
			}
		} catch (SQLException | IOException e) {
			Logger.warn(e);
		}
		
		try (CsvWriter csv = CsvWriter.builder().build(output_path.resolve("user_label_creator.csv"))) {
			csv.writeRow("creator", "count");
			ResultSet res = conn.conn.createStatement().executeQuery("SELECT CREATOR, COUNT(*) FROM USER_LABEL_STORE GROUP BY CREATOR ORDER BY COUNT(*) DESC");
			while(res.next()) {
				int creator = res.getInt(1);
				int count = res.getInt(2);
				String creatorName = conn.getCreatorById(creator);
				csv.writeRow(creatorName, ""+count);
			}
		} catch (SQLException | IOException e) {
			Logger.warn(e);
		}
		
		try (CsvWriter csv = CsvWriter.builder().build(output_path.resolve("user_label_location_label.csv"))) {
			csv.writeRow("location", "label", "count");
			ResultSet res = conn.conn.createStatement().executeQuery("SELECT LOCATION, LABEL, COUNT(*) FROM USER_LABEL_STORE GROUP BY LOCATION, LABEL ORDER BY LOCATION ASC, COUNT(*) DESC, LABEL ASC");
			while(res.next()) {
				int location = res.getInt(1);
				int label = res.getInt(2);
				int count = res.getInt(3);
				String locationName = conn.getLocationById(location);
				String labelName = conn.getLabelById(label);
				csv.writeRow(locationName, labelName, ""+count);
			}
		} catch (SQLException | IOException e) {
			Logger.warn(e);
		}
		
		try (CsvWriter csv = CsvWriter.builder().build(output_path.resolve("user_label_label_location.csv"))) {
			csv.writeRow("label", "location", "count");
			ResultSet res = conn.conn.createStatement().executeQuery("SELECT LABEL, LOCATION, COUNT(*) FROM USER_LABEL_STORE GROUP BY LABEL, LOCATION ORDER BY LABEL ASC, COUNT(*) DESC, LOCATION ASC");
			while(res.next()) {
				int label = res.getInt(1);
				int location = res.getInt(2);
				int count = res.getInt(3);
				String labelName = conn.getLabelById(label);
				String locationName = conn.getLocationById(location);
				csv.writeRow(labelName, locationName, ""+count);
			}
		} catch (SQLException | IOException e) {
			Logger.warn(e);
		}
		
		try (CsvWriter csv = CsvWriter.builder().build(output_path.resolve("user_label_sample.csv"))) {
			csv.writeRow("sample", "count");
			ResultSet res = conn.conn.createStatement().executeQuery("SELECT SAMPLE, COUNT(*) FROM USER_LABEL_STORE GROUP BY SAMPLE ORDER BY COUNT(*) DESC");
			while(res.next()) {
				int sample = res.getInt(1);
				int count = res.getInt(2);
				String sampleName = conn.getSampleById(sample);
				csv.writeRow(sampleName, ""+count);
			}
		} catch (SQLException | IOException e) {
			Logger.warn(e);
		}
	}
}
