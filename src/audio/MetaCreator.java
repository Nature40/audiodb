package audio;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tinylog.Logger;

import util.AudioTimeUtil;
import util.collections.vec.Vec;
import util.yaml.YamlUtil;

public class MetaCreator {

	private final static DateTimeFormatter AUDIO_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
	private final static String AUDIOMOTH = "AudioMoth ";
	private final static int AUDIOMOTH_LEN = AUDIOMOTH.length();
	private final static DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	private final static Pattern DATETIME_PATTERN = Pattern.compile(".*([0-9][0-9]:[0-9][0-9]:[0-9][0-9] [0-9][0-9]/[0-9][0-9]/[0-9][0-9][0-9][0-9]).*");
	public final static Pattern UTC_OFFSET_PATTERN = Pattern.compile(".*\\(UTC([-+]?[0-9]*)\\).*");	
	private final static int HOUR_OFFSET = 60*60;
	private final static Pattern GAIN_SETTING_PATTERN = Pattern.compile(".*at (.*?) gain setting.*");
	private final static Pattern BATTERY_STATE_PATTERN = Pattern.compile(".*battery state was (.*?V).*");
	private final static Pattern TEMPERATURE_PATTERN = Pattern.compile(".*temperature was (.*?)C.*");

	public static boolean createYaml(File file, Path yamlPath) {
		try {
			LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
			Riff riff = new Riff(file);
			m.put("AudioSens", "v1.1");
			m.put("file", file.getName());
			try {
				m.put("file_size", file.length());
			} catch(Exception e) {
				Logger.warn(e);
			}
			addRiffMeta(riff, m);
			Vec<Object> logList = new Vec<Object>();
			LinkedHashMap<String, Object> logO = new LinkedHashMap<String, Object>();
			logO.put("action", "create_yaml");
			logO.put("date", AudioTimeUtil.timeTextOfNow());
			logList.add(logO);
			m.put("log", logList);
			YamlUtil.writeSafeYamlMap(yamlPath, m);
			return true;
		} catch (Exception e) {
			Logger.warn(e);
			return false;
		}
	}

	public static boolean supplementYaml(Map<String, Object> m, Path samplePath) {
		try {
			Riff riff = new Riff(samplePath.toFile());
			addRiffMeta(riff, m);
			return true;
		} catch (Exception e) {
			Logger.warn(e);
			return false;
		}
	}

	private static void addRiffMeta(Riff riff, Map<String, Object> m) {
		addCommentMeta(riff, m);
		if(riff.artist != null) {			
			m.put("Artist", riff.artist);
			if(riff.artist.startsWith(AUDIOMOTH)) {
				try {
					m.put("device_type", "AudioMoth");
					String idText = riff.artist.substring(AUDIOMOTH_LEN);
					m.put("device_id", idText);
				} catch(Exception e) {
					Logger.warn(e);
				}
			}
		}
		if(riff.sample_rate > 0) {
			m.put("SampleRate", riff.sample_rate);
		}
		if(riff.avg_bytes_per_sec > 0) {
			m.put("AvgBytesPerSec", riff.avg_bytes_per_sec);
		}
		if(riff.bits_per_sample > 0) {
			m.put("BitsPerSample", riff.bits_per_sample);
		}				
		if(riff.samples > 0) {
			m.put("Samples", riff.samples);
		}
		if(riff.samples > 0 && riff.sample_rate > 0) {
			double duration = ((double) riff.samples) / ((double) riff.sample_rate);
			if(Double.isFinite(duration) && duration > 0) {
				m.put("Duration", duration);
			}
		}
	}
	
	private static void addCommentMeta(Riff riff, Map<String, Object> m) {
		addCommentMeta(riff.comments, m);
	}

	private static void addCommentMeta(String comments, Map<String, Object> m) {
		if(comments != null) {
			try {
				final Matcher datetimeMatcher = DATETIME_PATTERN.matcher(comments);
				if(datetimeMatcher.matches() && datetimeMatcher.groupCount() == 1) {
					String datetimeText = datetimeMatcher.group(1);
					LocalDateTime localDateTime = LocalDateTime.parse(datetimeText, AUDIO_FORMATTER);
					long timestamp = AudioTimeUtil.toAudiotime(localDateTime);
					try {
						final Matcher offsetMatcher = UTC_OFFSET_PATTERN.matcher(comments);
						if(offsetMatcher.matches() && offsetMatcher.groupCount() == 1) {
							String offsetText = offsetMatcher.group(1);
							if(!offsetText.isEmpty()) {
								int offset = Integer.parseInt(offsetText);
								timestamp -= offset * HOUR_OFFSET; 
							}
							m.put("recording_time_zone", "UTC" + offsetText);
						} else {
							Logger.warn("no timestamp UTC offset marker found");
						}
					} catch(Exception e) {
						Logger.warn(e);
					}
					m.put("timestamp", timestamp);
				}
			} catch(Exception e) {
				Logger.warn(e);
			}

			try {
				final Matcher matcher = GAIN_SETTING_PATTERN.matcher(comments);
				if(matcher.matches() && matcher.groupCount() == 1) {
					String text = matcher.group(1);						
					m.put("gain_setting", text);
				}
			} catch(Exception e) {
				Logger.warn(e);
			}

			try {
				final Matcher matcher = BATTERY_STATE_PATTERN.matcher(comments);
				if(matcher.matches() && matcher.groupCount() == 1) {
					String text = matcher.group(1);						
					m.put("battery_state", text);
				}
			} catch(Exception e) {
				Logger.warn(e);
			}
			
			double temperature = getTemperature(comments);
			if(Double.isFinite(temperature)) {
				m.put("temperature", temperature);
			}

			m.put("Comment", comments);
		}		
	}
	
	public static double getTemperature(String comments) {
		if(comments == null) {
			return Double.NaN;
		}
		try {
			final Matcher matcher = TEMPERATURE_PATTERN.matcher(comments);
			if(matcher.matches() && matcher.groupCount() == 1) {
				String text = matcher.group(1);
				double value = Double.parseDouble(text);
				return value;
			} else {
				return Double.NaN;
			}
		} catch(Exception e) {
			Logger.warn(e);
			return Double.NaN;
		}
	}
}
