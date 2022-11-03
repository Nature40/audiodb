package audio.task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.tinylog.Logger;

import de.siegmar.fastcsv.writer.CsvWriter;
import task.Cancelable;
import task.Description;
import task.Role;
import task.Tag;
import task.Task;
import util.AudioTimeUtil;

@Tag("audio")
@Description("Create audio recording timeseries per location.")
@Cancelable
@Role("admin")
public class Task_audio_location_timeseries extends Task {

	@Override
	public void run() {
		if(isSoftCanceled()) {
			throw new RuntimeException("canceled");
		}

		Path output_folder_path = Paths.get("output", "timeseries");
		File output_folder_file = output_folder_path.toFile();
		if(!output_folder_file.mkdirs()) {
			if(!output_folder_file.exists()) {
				throw new RuntimeException("output folder not created");
			}
		}
		
		if(isSoftCanceled()) {
			throw new RuntimeException("canceled");
		}

		ctx.broker.sampleManager().tlSampleManagerConnector.get().forEachLocation(location -> {
			if(isSoftCanceled()) {
				throw new RuntimeException("canceled");
			}
			if(location != null && !location.isBlank()) {
				setMessage("processing location: " + location);
				try (CsvWriter csv = CsvWriter.builder().build(output_folder_path.resolve(location + ".csv"))) {
					csv.writeRow("plotID", "datetime", "audio");
					ctx.broker.sampleManager().forEachAtLocation(location, sample -> {
						if(isSoftCanceled()) {
							throw new RuntimeException("canceled");
						}
						String timeName = AudioTimeUtil.toTextMinutes(AudioTimeUtil.ofAudiotime(sample.timestamp));
						csv.writeRow(location, timeName, "1");
					});
				} catch (IOException e) {
					Logger.warn(e);
				}
			}
		});
	}
}
