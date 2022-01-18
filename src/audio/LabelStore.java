package audio;

import java.io.IOException;
import java.nio.file.Path;

import org.tinylog.Logger;

import audio.LabelStoreConnector.TlLabelStoreConnector;
import de.siegmar.fastcsv.writer.CsvWriter;

public class LabelStore {

	private final Broker broker;
	public final TlLabelStoreConnector tlLabelStoreConnector;

	public LabelStore(Broker broker) {
		this.broker = broker;
		tlLabelStoreConnector = new TlLabelStoreConnector("jdbc:h2:./label_store");
		tlLabelStoreConnector.get().init(false);
	}

	public void rebuild() {
		SampleManager sampleManager = broker.sampleManager();
		LabelStoreConnector conn = tlLabelStoreConnector.get();
		conn.init(true);
		
		sampleManager.forEach(sample -> {
			int sampleMapId = conn.getOrCreateIdBySample(sample.id);
			Logger.info(sample.id);
			int location = sample.hasLocation() ? conn.getOrCreateIdByLocation(sample.location) : 0;
			int time = sample.hasTimestamp() ? (int) sample.timestamp : 0;
			for(Label label : sample.getLabels()) {
				for(GeneratorLabel lbl : label.generatorLabels) {
					int labelMapId = conn.getOrCreateIdByLabel(lbl.name);
					double rel = lbl.reliability;
					int reliability = Double.isFinite(rel) ? rel >= 0 ? rel <= 1 ? (int) Math.round(rel * 1000): 1000 : 0: 0;					
					conn.insert(sampleMapId, labelMapId, reliability, location, time);
				}
			}
		});

		try (CsvWriter csv = CsvWriter.builder().build(Path.of("data.csv"))) {
			csv.writeRow("location", "time", "label", "reliability");		    
			conn.forEach((int id, int label, short reliability, int location, int time) -> {
				//String sampleID = conn.getSampleById(id);
				//Sample2 sample = sampleManager.getById(sampleID);
				//String location = sample.hasLocation() ? sample.location : "";
				//String time = sample.hasTimestamp() ? AudioTimeUtil.ofAudiotime(sample.timestamp).toString() : "";
				//String time = sample.hasTimestamp() ? "" + (sample.timestamp / 60) : "";
				//String labelName = conn.getLabelById(label);
				String labelName = "" + label;
				String rel = "" + reliability;
				csv.writeRow("" + location, "" + time, labelName, rel);
			});		    
		} catch (IOException e) {
			Logger.warn(e);
		}
		
		try (CsvWriter csv = CsvWriter.builder().build(Path.of("location.csv"))) {
			csv.writeRow("id", "location");		    
			conn.forEachLocation((int id, String location) -> {
				csv.writeRow("" + id, location);
			});		    
		} catch (IOException e) {
			Logger.warn(e);
		}
		
		try (CsvWriter csv = CsvWriter.builder().build(Path.of("label.csv"))) {
			csv.writeRow("id", "label");		    
			conn.forEachLabel((int id, String location) -> {
				csv.writeRow("" + id, location);
			});		    
		} catch (IOException e) {
			Logger.warn(e);
		}
	}
}
