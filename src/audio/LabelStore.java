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
			for(Label label : sample.getLabels()) {
				for(GeneratorLabel lbl : label.generatorLabels) {
					int labelMapId = conn.getOrCreateIdByLabel(lbl.name);
					double rel = lbl.reliability;
					int reliability = Double.isFinite(rel) ? rel >= 0 ? rel <= 1 ? (int) Math.round(rel * 100): 100 : 0: -1;
					conn.insert(sampleMapId, labelMapId, reliability);
				}
			}
		});

		try (CsvWriter csv = CsvWriter.builder().build(Path.of("csv_out.csv"))) {
			csv.writeRow("location", "time", "label", "reliability");		    
			conn.forEach((int id, int label, byte reliability) -> {
				String sampleID = conn.getSampleById(id);
				Sample2 sample = sampleManager.getById(sampleID);
				String location = sample.hasLocation() ? sample.location : "";
				//String time = sample.hasTimestamp() ? AudioTimeUtil.ofAudiotime(sample.timestamp).toString() : "";
				String time = sample.hasTimestamp() ? "" + (sample.timestamp / 60) : "";
				//String labelName = conn.getLabelById(label);
				String labelName = "" + label;
				String rel = "" + reliability;
				csv.writeRow(location, time, labelName, rel);
			});		    
		} catch (IOException e) {
			Logger.warn(e);
		}		
	}
}
