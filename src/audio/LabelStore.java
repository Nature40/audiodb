package audio;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashSet;

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
		SampleManager sampleManager = broker.sampleManager();
		LabelStoreConnector conn = tlLabelStoreConnector.get();
		conn.init(true);
		HashSet<String> labelSet = new HashSet<String>();
		sampleManager.forEach(sample -> {
			Logger.info(sample.id);
			labelSet.clear();
			for(Label label : sample.getLabels()) {
				for(GeneratorLabel lbl : label.generatorLabels) {
					labelSet.add(lbl.name);
				}
			}
			for (String name : labelSet) {
				conn.insert(sample.id, name);
			}
		});		

		try (CsvWriter csv = CsvWriter.builder().build(Path.of("csv_out.csv"))) {
			csv.writeRow("location", "time", "label");		    
			conn.forEach((id, label) -> {
				Sample2 sample = sampleManager.getById(id);
				String location = sample.hasLocation() ? sample.location : "";
				String time = sample.hasTimestamp() ? AudioTimeUtil.ofAudiotime(sample.timestamp).toString() : "";
				csv.writeRow(location, time, label);
			});		    
		} catch (IOException e) {
			Logger.warn(e);
		}		
	}
}
