package audio.task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.tinylog.Logger;

import de.siegmar.fastcsv.writer.CsvWriter;
import task.Cancelable;
import task.Description;
import task.Descriptor.Param.Type;
import task.Param;
import task.Tag;
import task.Task;
import util.AudioTimeUtil;
import util.collections.vec.Vec;

@Tag("audio")
@Description("Create statistics about sample time and location.")
@Cancelable
@Param(name = "col_location", type = Type.BOOLEAN, preset = "TRUE", description = "Include column 'location' in CSV output.")
@Param(name = "col_time", type = Type.BOOLEAN, preset = "TRUE", description = "Include column 'time' in CSV output.")
@Param(name = "col_sample", type = Type.BOOLEAN, preset = "TRUE", description = "Include column 'sample' in CSV output.")
@Param(name = "col_device", type = Type.BOOLEAN, preset = "TRUE", description = "Include column 'device' in CSV output.")
@Param(name = "col_duration", type = Type.BOOLEAN, preset = "FALSE", description = "Include column 'duration' in CSV output.")
@Param(name = "col_time_zone", type = Type.BOOLEAN, preset = "FALSE", description = "Include column 'time_zone' in CSV output.")
@Param(name = "col_temperature", type = Type.BOOLEAN, preset = "FALSE", description = "Include column 'temperature' in CSV output.")
@Param(name = "filename", type = Type.STRING, preset = "samples.csv", description = "Filename of output CSV-file.")
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

		boolean col_location = this.ctx.getParamBoolean("col_location");
		boolean col_time = this.ctx.getParamBoolean("col_time");
		boolean col_sample = this.ctx.getParamBoolean("col_sample");
		boolean col_device = this.ctx.getParamBoolean("col_device");
		boolean col_duration = this.ctx.getParamBoolean("col_duration");
		boolean col_time_zone = this.ctx.getParamBoolean("col_time_zone");
		boolean col_temperature = this.ctx.getParamBoolean("col_temperature");
		String filename = this.ctx.getParamString("filename");
		validateFilenameThrow(filename);

		try (CsvWriter csv = CsvWriter.builder().build(output_path.resolve(filename))) {
			Vec<String> cols = new Vec<String>();

			if(col_location) {
				cols.add("location");	
			}
			if(col_time) {
				cols.add("time");	
			}
			if(col_sample) {
				cols.add("sample");	
			}
			if(col_device) {
				cols.add("device");	
			}
			if(col_duration) {
				cols.add("duration");	
			}
			if(col_time_zone) {
				cols.add("time_zone");	
			}			
			if(col_temperature) {
				cols.add("temperature");	
			}
			csv.writeRow(cols);
			Vec<String> row = new Vec<String>();
			ctx.broker.sampleManager().forEach(sample -> {
				row.clear();
				if(isSoftCanceled()) {
					throw new RuntimeException("canceled");
				}
				if(col_location) {
					String location = sample.location;
					row.add(location);
				}
				if(col_time) {
					String timeName = AudioTimeUtil.ofAudiotime(sample.timestamp).toString();
					row.add(timeName);
				}
				if(col_sample) {
					String samplePath = sample.samplePath.toString();
					row.add(samplePath);
				}
				if(col_device) {
					String device = sample.device;
					row.add(device);
				}
				if(col_duration) {
					double d = sample.duration();
					String duration = Double.isFinite(d) ? "" + d : "NA";
					row.add(duration);
				}
				if(col_time_zone) {
					String utc_ = sample.getUTC();
					String utc = utc_ == null ? "" : utc_;
					row.add(utc);
				}
				if(col_temperature) {
					double temperature_ = sample.getTemperature();
					String temperature = Double.isFinite(temperature_) ? Double.toString(temperature_) : "";
					row.add(temperature);
				}
				csv.writeRow(row);
			});		    
		} catch (IOException e) {
			Logger.warn(e);
		}
	}
	
	public static void validateFilenameThrow(String filename) {
		if(filename == null) {
			throw new RuntimeException("filename null");
		}
		if(filename.isBlank()) {
			throw new RuntimeException("filename empty");
		}
		if(filename.contains("/")) {
			throw new RuntimeException("filename chracter now allowed: slash");
		}
		if(filename.contains("\\")) {
			throw new RuntimeException("filename chracter now allowed: backslash");
		}
		if(filename.contains(":")) {
			throw new RuntimeException("filename chracter now allowed: colon");
		}
		if(filename.contains(";")) {
			throw new RuntimeException("filename chracter now allowed: semicolon");
		}
		if(filename.contains("*")) {
			throw new RuntimeException("filename chracter now allowed: *");
		}
		if(filename.contains("+")) {
			throw new RuntimeException("filename chracter now allowed: +");
		}
		if(filename.contains("?")) {
			throw new RuntimeException("filename chracter now allowed: ?");
		}
		if(filename.contains("!")) {
			throw new RuntimeException("filename chracter now allowed: !");
		}
		if(filename.contains("#")) {
			throw new RuntimeException("filename chracter now allowed: #");
		}
		if(filename.contains("~")) {
			throw new RuntimeException("filename chracter now allowed: ~");
		}
		if(filename.contains("$")) {
			throw new RuntimeException("filename chracter now allowed: $");
		}
		if(filename.contains("%")) {
			throw new RuntimeException("filename chracter now allowed: %");
		}
		if(filename.contains("§")) {
			throw new RuntimeException("filename chracter now allowed: §");
		}
		if(filename.contains("&")) {
			throw new RuntimeException("filename chracter now allowed: &");
		}
		if(filename.contains("|")) {
			throw new RuntimeException("filename chracter now allowed: |");
		}
		if(filename.contains("=")) {
			throw new RuntimeException("filename chracter now allowed: =");
		}
		if(filename.contains("\"")) {
			throw new RuntimeException("filename chracter now allowed: \"");
		}
		if(filename.contains("\n")) {
			throw new RuntimeException("filename chracter now allowed: new line");
		}
		if(filename.contains("\t")) {
			throw new RuntimeException("filename chracter now allowed: tab");
		}
		if(filename.charAt(0) == '.') {
			throw new RuntimeException("filename chracter now allowed: start with dot");
		}
	}
}