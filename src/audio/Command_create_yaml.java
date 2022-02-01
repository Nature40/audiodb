package audio;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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

public class Command_create_yaml implements Command {


	@Override
	public void execute(String command, String[] params) {
		if(params.length < 1) {
			throw new RuntimeException("missing parameter: folder");
		}
		if(params.length != 1) {
			throw new RuntimeException("only one paramer allowed: folder");
		}
		File folder = new File(params[0]);
		if(!folder.exists()) {
			throw new RuntimeException("folder does not exist");
		}
		if(!folder.isDirectory()) {
			throw new RuntimeException("is not a folder");
		}
		traverseFolder(folder.toPath());
	}

	private long traverseFolder(Path folder) {
		LocalDateTime start = LocalDateTime.now();
		long counter = 0;
		Logger.info("traverse " + folder);
		try {
			DirectoryStream<Path> dirStream = Files.newDirectoryStream(folder);
			for(Path path: dirStream) {
				File file = path.toFile();
				if(file.isDirectory()) {
					counter += traverseFolder(path);
				} else if(file.isFile()) {
					boolean ret = traverseFile(file);
					if(ret) {
						counter++;
					}
				} else {
					Logger.warn("unknown entry: " + file.toString());
				}
			}
		} catch (Exception e) {
			Logger.warn(e);
		}
		LocalDateTime end = LocalDateTime.now();
		Duration duration = Duration.between(start, end);
		if(counter > 0) {			
			Duration durationPerFile = duration.dividedBy(counter);
			Logger.info("traversed " + folder + "  " + counter + " files  in " + duration + "     " + durationPerFile + " per file");
		} else {
			Logger.info("traversed " + folder + "  " + counter + " files  in " + duration);
		}

		return counter;
	}

	protected boolean traverseFile(File file) {
		String path = file.getPath();
		if(path.endsWith(".wav") || path.endsWith(".WAV")) {
			String yamlPath = path + ".yaml";
			File yamlFile = new File(yamlPath);
			if(!yamlFile.exists()) {
				return createYaml(file, yamlFile.toPath());
			}
		}
		return false;		
	}

	private final static DateTimeFormatter AUDIO_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
	private final static String RECORDED_AT = "Recorded at ";
	private final static int RECORDED_AT_LEN = RECORDED_AT.length();
	private final static String AUDIOMOTH = "AudioMoth ";
	private final static int AUDIOMOTH_LEN = AUDIOMOTH.length();
	private final static DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	private final static Pattern DATETIME_PATTERN = Pattern.compile(".*([0-9][0-9]:[0-9][0-9]:[0-9][0-9] [0-9][0-9]/[0-9][0-9]/[0-9][0-9][0-9][0-9]).*");
	private final static Pattern UTC_OFFSET_PATTERN = Pattern.compile(".*\\(UTC([-+]?[0-9]*)\\).*");	
	private final static int HOUR_OFFSET = 60*60;
	private final static Pattern GAIN_SETTING_PATTERN = Pattern.compile(".*at (.*?) gain setting.*");
	private final static Pattern BATTERY_STATE_PATTERN = Pattern.compile(".*battery state was (.*?V).*");
	private final static Pattern TEMPERATURE_PATTERN = Pattern.compile(".*temperature was (.*?C).*");


	protected boolean createYaml(File file, Path yamlPath) {
		try {
			Riff riff = new Riff(file);
			LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
			m.put("AudioSens", "v1.1");
			m.put("file", file.getName());
			if(riff.comments != null) {
				try {
					final Matcher datetimeMatcher = DATETIME_PATTERN.matcher(riff.comments);
					if(datetimeMatcher.matches() && datetimeMatcher.groupCount() == 1) {
						String datetimeText = datetimeMatcher.group(1);
						LocalDateTime localDateTime = LocalDateTime.parse(datetimeText, AUDIO_FORMATTER);
						long timestamp = AudioTimeUtil.toAudiotime(localDateTime);
						try {
							final Matcher offsetMatcher = UTC_OFFSET_PATTERN.matcher(riff.comments);
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
					final Matcher matcher = GAIN_SETTING_PATTERN.matcher(riff.comments);
					if(matcher.matches() && matcher.groupCount() == 1) {
						String text = matcher.group(1);						
						m.put("gain_setting", text);
					}
				} catch(Exception e) {
					Logger.warn(e);
				}

				try {
					final Matcher matcher = BATTERY_STATE_PATTERN.matcher(riff.comments);
					if(matcher.matches() && matcher.groupCount() == 1) {
						String text = matcher.group(1);						
						m.put("battery_state", text);
					}
				} catch(Exception e) {
					Logger.warn(e);
				}
				
				try {
					final Matcher matcher = TEMPERATURE_PATTERN.matcher(riff.comments);
					if(matcher.matches() && matcher.groupCount() == 1) {
						String text = matcher.group(1);						
						m.put("temperature", text);
					}
				} catch(Exception e) {
					Logger.warn(e);
				}
			}
			if(riff.artist != null && riff.artist.startsWith(AUDIOMOTH)) {
				try {
					String idText = riff.artist.substring(AUDIOMOTH_LEN);
					//Logger.info("idText |" + idText + "|");						
					m.put("device_id", idText);
				} catch(Exception e) {
					Logger.warn(e);
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
			if(riff.comments != null) {
				m.put("Comment", riff.comments);
			}
			if(riff.artist != null) {
				m.put("Artist", riff.artist);				
			}
			if(riff.artist != null && riff.artist.startsWith(AUDIOMOTH)) {
				m.put("device_type", "AudioMoth");
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
			Vec<Object> logList = new Vec<Object>();
			LinkedHashMap<String, Object> logO = new LinkedHashMap<String, Object>();
			logO.put("action", "create_yaml");
			logO.put("date", LocalDateTime.now().format(ISO_FORMATTER));
			logList.add(logO);
			m.put("log", logList);
			YamlUtil.writeSafeYamlMap(yamlPath, m);
			return true;
		} catch (Exception e) {
			Logger.warn(e);
			return false;
		}
	}

	public static boolean supplementYaml(Map<String, Object> map, Path samplePath) {
		try {
			Riff riff = new Riff(samplePath.toFile());
			if(riff.comments != null && riff.comments.startsWith(RECORDED_AT)) {
				try {
					String timeText = riff.comments.substring(RECORDED_AT_LEN, RECORDED_AT_LEN + 19);
					//Logger.info("timeText |" + timeText + "|");
					LocalDateTime localDateTime = LocalDateTime.parse(timeText, AUDIO_FORMATTER);
					//Logger.info("dateTime |" + localDateTime + "|");
					long timestamp = AudioTimeUtil.toAudiotime(localDateTime);
					map.put("timestamp", timestamp);
				} catch(Exception e) {
					Logger.warn(e);
				}
			}
			if(riff.artist != null && riff.artist.startsWith(AUDIOMOTH)) {
				try {
					String idText = riff.artist.substring(AUDIOMOTH_LEN);
					//Logger.info("idText |" + idText + "|");						
					map.put("device_id", idText);
				} catch(Exception e) {
					Logger.warn(e);
				}
			}				
			if(riff.sample_rate > 0) {
				map.put("SampleRate", riff.sample_rate);
			}
			if(riff.avg_bytes_per_sec > 0) {
				map.put("AvgBytesPerSec", riff.avg_bytes_per_sec);
			}
			if(riff.bits_per_sample > 0) {
				map.put("BitsPerSample", riff.bits_per_sample);
			}
			if(riff.comments != null) {
				map.put("Comment", riff.comments);
			}
			if(riff.artist != null) {
				map.put("Artist", riff.artist);				
			}
			if(riff.artist != null && riff.artist.startsWith(AUDIOMOTH)) {
				map.put("device_type", "AudioMoth");
			}				
			if(riff.samples > 0) {
				map.put("Samples", riff.samples);
			}
			if(riff.samples > 0 && riff.sample_rate > 0) {
				double duration = ((double) riff.samples) / ((double) riff.sample_rate);
				if(Double.isFinite(duration) && duration > 0) {
					map.put("Duration", duration);
				}
			}
			return true;
		} catch (Exception e) {
			Logger.warn(e);
			return false;
		}
	}
}
