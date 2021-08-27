package audio;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.AudioTimeUtil;
import util.collections.vec.Vec;
import util.yaml.YamlUtil;

public class Command_create_yaml implements Command {
	static final Logger log = LogManager.getLogger();

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

	private void traverseFolder(Path folder) {
		log.info("traverse " + folder);
		try {
			DirectoryStream<Path> dirStream = Files.newDirectoryStream(folder);
			for(Path path: dirStream) {
				File file = path.toFile();
				if(file.isDirectory()) {
					traverseFolder(path);
				} else if(file.isFile()) {
					traverseFile(file);
				} else {
					log.warn("unknown entry");
				}
			}
		} catch (Exception e) {
			log.warn(e);
		}
	}

	private void traverseFile(File file) {
		String path = file.getPath();
		if(path.endsWith(".wav") || path.endsWith(".WAV")) {
			String yamlPath = path + ".yaml";
			File yamlFile = new File(yamlPath);
			if(!yamlFile.exists()) {
				createYaml(file, yamlFile.toPath());
			}
		}		
	}

	private final static DateTimeFormatter AUDIO_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
	private final static String RECORDED_AT = "Recorded at ";
	private final static int RECORDED_AT_LEN = RECORDED_AT.length();
	private final static String AUDIOMOTH = "AudioMoth ";
	private final static int AUDIOMOTH_LEN = AUDIOMOTH.length();
	private final static DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	private void createYaml(File file, Path yamlPath) {
		try {
			Riff riff = new Riff(file);
			LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
			m.put("AudioSens", "v1.1");
			m.put("file", file.getName());
			if(riff.comments != null && riff.comments.startsWith(RECORDED_AT)) {
				try {
					String timeText = riff.comments.substring(RECORDED_AT_LEN, RECORDED_AT_LEN + 19);
					//log.info("timeText |" + timeText + "|");
					LocalDateTime localDateTime = LocalDateTime.parse(timeText, AUDIO_FORMATTER);
					//log.info("dateTime |" + localDateTime + "|");
					long timestamp = AudioTimeUtil.toAudiotime(localDateTime);
					m.put("timestamp", timestamp);
				} catch(Exception e) {
					log.warn(e);
				}
			}
			if(riff.artist != null && riff.artist.startsWith(AUDIOMOTH)) {
				try {
					String idText = riff.artist.substring(AUDIOMOTH_LEN);
					//log.info("idText |" + idText + "|");						
					m.put("device_id", idText);
				} catch(Exception e) {
					log.warn(e);
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
			Vec<Object> logList = new Vec<Object>();
			LinkedHashMap<String, Object> logO = new LinkedHashMap<String, Object>();
			logO.put("action", "create_yaml");
			logO.put("date", LocalDateTime.now().format(ISO_FORMATTER));
			logList.add(logO);
			m.put("log", logList);
			YamlUtil.writeSafeYamlMap(yamlPath, m);
		} catch (Exception e) {
			log.warn(e);
		}
	}
}
