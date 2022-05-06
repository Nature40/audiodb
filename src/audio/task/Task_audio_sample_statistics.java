package audio.task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.tinylog.Logger;

import de.siegmar.fastcsv.writer.CsvWriter;
import task.Cancelable;
import task.Description;
import task.Tag;
import task.Task;
import util.AudioTimeUtil;

@Tag("audio")
@Description("Create statistics about sample time and location.")
@Cancelable
public class Task_audio_sample_statistics extends Task {

	@Override
	public void run() {
		if(isSoftCanceled()) {
			throw new RuntimeException("canceled");
		}
		
		String output_folder = "output";
		File output_file = new File(output_folder);
		if(!output_file.mkdirs()) {
			if(!output_file.exists()) {
				throw new RuntimeException("output folder not created");
			}
		}
		Path output_path = output_file.toPath();

		try (CsvWriter csv = CsvWriter.builder().build(output_path.resolve("samples.csv"))) {
			csv.writeRow("location", "time", "sample", "device", "duration", "time_zone");	
			ctx.broker.sampleManager().forEach(sample -> {
				if(isSoftCanceled()) {
					throw new RuntimeException("canceled");
				}
				String location = sample.location;
				String timeName = AudioTimeUtil.ofAudiotime(sample.timestamp).toString();
				String samplePath = sample.samplePath.toString();
				String device = sample.device;
				double d = sample.duration();
				String duration = Double.isFinite(d) ? "" + d : "NA";
				String utc_ = sample.getUTC();
				String utc = utc_ == null ? "" : utc_;
				csv.writeRow(location, timeName, samplePath, device, duration, utc);
			});		    
		} catch (IOException e) {
			Logger.warn(e);
		}
	}
}
