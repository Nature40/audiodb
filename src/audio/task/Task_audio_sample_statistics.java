package audio.task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Locale;

import org.tinylog.Logger;

import de.siegmar.fastcsv.writer.CsvWriter;
import task.Cancelable;
import task.Description;
import task.Descriptor.Param.Type;
import task.Param;
import task.Role;
import task.Tag;
import task.Task;
import task.TaskResult;
import util.AudioTimeUtil;
import util.collections.vec.Vec;

@Tag("audio")
@Description("Create statistics about sample time and location.")
@Cancelable
@Param(name = "col_location", type = Type.BOOLEAN, preset = "TRUE", description = "Include column 'location' in CSV output.")
@Param(name = "col_time", type = Type.BOOLEAN, preset = "TRUE", description = "Include column 'time' in CSV output.")
@Param(name = "col_path", type = Type.BOOLEAN, preset = "FALSE", description = "Include column 'path' in CSV output. Folder path excluding filename.")
@Param(name = "col_filename", type = Type.BOOLEAN, preset = "FALSE", description = "Include column 'filename' in CSV output. Filename excluding folder path.")
@Param(name = "col_sample", type = Type.BOOLEAN, preset = "TRUE", description = "Include column 'sample' in CSV output. Folder path including filename.")
@Param(name = "col_device", type = Type.BOOLEAN, preset = "TRUE", description = "Include column 'device' in CSV output.")
@Param(name = "col_duration", type = Type.BOOLEAN, preset = "FALSE", description = "Include column 'duration' in CSV output. (yaml read needed)")
@Param(name = "col_original_time_zone", type = Type.BOOLEAN, preset = "FALSE", description = "Include column 'original_time_zone' of recording in CSV output. This may not be identical to the time zone of the 'time' column. (yaml read needed)")
@Param(name = "col_temperature", type = Type.BOOLEAN, preset = "FALSE", description = "Include column 'temperature' in CSV output. (yaml read needed)")
@Param(name = "col_file_size", type = Type.BOOLEAN, preset = "FALSE", description = "Include column 'file_size' in CSV output and sum up total file size in log message.")
@Param(name = "filename", type = Type.STRING, preset = "samples.csv", description = "Filename of output CSV-file. (yaml read needed)")
@Param(name = "time_zone", type = Type.STRING, preset = "", description = "Set time zone of the 'time' column. e.g. UTC+1  If left empty, default time zone of project will be set. (Only meaningful if the audio files include a time zone.)")
@Param(name = "include_time_zone", type = Type.BOOLEAN, preset = "FALSE", description = "In 'time' column, include the time zone marker. If false, time zone marker is not included in output, but time zone conversions are still applied. (Only meaningful if the audio files include a time zone.)")
@Param(name = "filter_by_location", type = Type.STRING, preset = "", description = "(optional) Process the specified location only.")
@Param(name = "filter_by_device", type = Type.STRING, preset = "", description = "(optional) Process the specified device id only.")
@Param(name = "filter_by_time", type = Type.STRING, preset = "", description = "(optional) Process the specified __range__ of time only. Format: yyyy-MM-ddTHH:mm:ss  A shortened format leads to a range of time. e.g. 2022 means all samples from year 2022. e.g. 2022-02 means all samples from February at year 2022.  Time zone of 'time_zone' parameter will be used, or if empty, default time zone of project.")
@Param(name = "filter_by_time_start", type = Type.STRING, preset = "", description = "(optional) Process __starting__ with the specified time. Format: yyyy-MM-ddTHH:mm:ss  A shortened format is allowed. e.g. 2022 means all samples, starting with with 2022-01-01T00:00:00.  Time zone of 'time_zone' parameter will be used, or if empty, default time zone of project.")
@Param(name = "filter_by_time_end", type = Type.STRING, preset = "", description = "(optional) Process __ending__ with the specified time. Format: yyyy-MM-ddTHH:mm:ss  A shortened format is allowed. e.g. 2022 means all samples, ending with with 2022-12-31T23:59:59.  Time zone of 'time_zone' parameter will be used, or if empty, default time zone of project.")

@Role("admin")
public class Task_audio_sample_statistics extends Task {

	@Override
	public void run() {
		if(isSoftCanceled()) {
			throw new RuntimeException("canceled");
		}

		String output_folder = "output" + "/" + this.ctx.id;
		File output_file = new File(output_folder);
		if(!output_file.mkdirs()) {
			if(!output_file.exists()) {
				throw new RuntimeException("output folder not created");
			}
		}
		Path output_path = output_file.toPath();

		boolean col_location = this.ctx.getParamBoolean("col_location");
		boolean col_time = this.ctx.getParamBoolean("col_time");
		boolean col_path = this.ctx.getParamBoolean("col_path");
		boolean col_filename = this.ctx.getParamBoolean("col_filename");
		boolean col_sample = this.ctx.getParamBoolean("col_sample");
		boolean col_device = this.ctx.getParamBoolean("col_device");
		boolean col_duration = this.ctx.getParamBoolean("col_duration");
		boolean col_original_time_zone = this.ctx.getParamBoolean("col_original_time_zone");
		boolean col_temperature = this.ctx.getParamBoolean("col_temperature");
		boolean col_file_size = this.ctx.getParamBoolean("col_file_size");
		String filename = this.ctx.getParamString("filename");
		String reqTimeZone = this.ctx.getParamString("time_zone");
		boolean include_time_zone = this.ctx.getParamBoolean("include_time_zone");
		String tz = reqTimeZone.isBlank() ? ctx.broker.config().audioConfig.time_zone : reqTimeZone;
		int timeZoneOffsetSeconds = AudioTimeUtil.getTimeZoneOffsetSeconds(tz);
		ZoneOffset timeZoneOffset = include_time_zone ? ZoneOffset.ofTotalSeconds(timeZoneOffsetSeconds) : null;
		String filter_by_location = this.ctx.getParamString("filter_by_location");
		String filter_by_device = this.ctx.getParamString("filter_by_device");
		String filter_by_time = this.ctx.getParamString("filter_by_time");
		String filter_by_time_start = this.ctx.getParamString("filter_by_time_start");
		String filter_by_time_end = this.ctx.getParamString("filter_by_time_end");
		validateFilenameThrow(filename);
		boolean hasFilter_by_location = !filter_by_location.isBlank();
		boolean hasFilter_by_device = !filter_by_device.isBlank();
		
		long time_min = Long.MIN_VALUE;
		long time_max = Long.MAX_VALUE;
		
		if(!filter_by_time.isBlank()) {
			long tmin = AudioTimeUtil.toAudiotimeStart(filter_by_time);
			long tmax = AudioTimeUtil.toAudiotimeEnd(filter_by_time);
			if(tmin != Long.MIN_VALUE) {
				tmin -= timeZoneOffsetSeconds;
				if(tmin > time_min) {
					time_min = tmin;
				}
			}
			if(tmax != Long.MAX_VALUE) {
				tmax -= timeZoneOffsetSeconds;
				if(tmax < time_max) {
					time_max = tmax;
				}
			}
		}
		
		if(!filter_by_time_start.isBlank()) {
			long tmin = AudioTimeUtil.toAudiotimeStart(filter_by_time_start);
			if(tmin != Long.MIN_VALUE) {
				tmin -= timeZoneOffsetSeconds;
				if(tmin > time_min) {
					time_min = tmin;
				}
			}
		}
		
		if(!filter_by_time_end.isBlank()) {
			long tmax = AudioTimeUtil.toAudiotimeEnd(filter_by_time_end);
			if(tmax != Long.MAX_VALUE) {
				tmax -= timeZoneOffsetSeconds;
				if(tmax < time_max) {
					time_max = tmax;
				}
			}
		}
		
		long time_minf = time_min;
		long time_maxf = time_max;

		Path output_target = output_path.resolve(filename);
		try (CsvWriter csv = CsvWriter.builder().build(output_target)) {
			Vec<String> cols = new Vec<String>();

			if(col_location) {
				cols.add("location");	
			}
			if(col_time) {
				cols.add("time");	
			}
			if(col_path) {
				cols.add("path");	
			}
			if(col_filename) {
				cols.add("filename");	
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
			if(col_original_time_zone) {
				cols.add("original_time_zone");	
			}			
			if(col_temperature) {
				cols.add("temperature");	
			}
			if(col_file_size) {
				cols.add("file_size");	
			}
			csv.writeRow(cols);
			Vec<String> row = new Vec<String>();
			final long[] fileSizeCounter = new long[] {0};
			final double[] durationCounter = new double[] {0};
			final long[] fileCounter = new long[] {0};
			DecimalFormat doubleFormat = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			doubleFormat.setMaximumFractionDigits(340);
			Path root_data_path = ctx.broker.config().audioConfig.root_data_path;
			setMessage("start traversing audio files");
			ctx.broker.sampleManager().forEach(sample -> {
				if(isSoftCanceled()) {
					throw new RuntimeException("canceled");
				}
				if((!hasFilter_by_location || (hasFilter_by_location && filter_by_location.equals(sample.location)))
						&& (!hasFilter_by_device || (hasFilter_by_device && filter_by_device.equals(sample.device)))
						&& (time_minf <= sample.timestamp && sample.timestamp <= time_maxf)) {
					row.clear();
					if(col_location) {
						String location = sample.location;
						row.add(location);
					}
					if(col_time) {
						LocalDateTime dt = AudioTimeUtil.ofAudiotime(sample.timestamp, timeZoneOffsetSeconds);
						if(include_time_zone) {
							ZonedDateTime zdt = ZonedDateTime.of(dt, timeZoneOffset);
							String timeName = zdt.toString();
							row.add(timeName);
						} else {
							String timeName = dt.toString();
							row.add(timeName);
						}
					}
					if(col_path || col_filename || col_sample) {
						Path path = root_data_path.relativize(sample.samplePath);
						if(col_path) {
							Path parent = path.getParent();
							String samplePath = parent == null ? "" : parent.toString();
							row.add(samplePath);
						}
						if(col_filename) {
							String samplePath = path.getFileName().toString();
							row.add(samplePath);
						}
						if(col_sample) {
							String samplePath = path.toString();
							row.add(samplePath);
						}
					}
					if(col_device) {
						String device = sample.device;
						row.add(device);
					}
					if(col_duration) {
						double d = sample.duration();
						if(Double.isFinite(d)) {
							row.add(doubleFormat.format(d));
							durationCounter[0] += d;
						} else {
							row.add("NA");
						}					
					}
					if(col_original_time_zone) {
						String utc_ = sample.getUTC();
						String utc = utc_ == null ? "" : utc_;
						row.add(utc);
					}
					if(col_temperature) {
						double temperature_ = sample.getTemperature();
						String temperature = Double.isFinite(temperature_) ? Double.toString(temperature_) : "";
						row.add(temperature);
					}
					if(col_file_size) {
						long filesize = sample.getFileSize();
						if(filesize >= 0) {
							row.add(Long.toString(filesize));
							fileSizeCounter[0] += filesize; 
						} else {
							row.add("");
						}					
					}
					csv.writeRow(row);
				}
				fileCounter[0]++;
				if(fileCounter[0] % 1000 == 0) {
					setMessage(fileCounter[0] + " audio files passed");
				}
			});
			setResult(
					TaskResult.ofText("CSV-file produced."),
					TaskResult.ofFile(output_target)
					);
			if(col_file_size) {
				Duration totalDuration = Duration.ofSeconds((long) durationCounter[0]);
				String durationText = String.format("%d days %02d:%02d:%02d", totalDuration.toDays(), totalDuration.toHoursPart(), totalDuration.toMinutesPart(), totalDuration.toSecondsPart());
				setMessage(fileCounter[0] + " total audio files");
				setMessage(durationText + " total audio data duration (" + doubleFormat.format(durationCounter[0]) + " seconds)");	
				setMessage(fileSizeCounter[0] + " bytes total audio data file size");				
			}
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