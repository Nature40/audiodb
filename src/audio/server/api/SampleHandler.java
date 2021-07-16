package audio.server.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.BitSet;
import java.util.LinkedHashMap;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.IO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.yaml.snakeyaml.Yaml;

import audio.Account;
import audio.Broker;
import audio.Sample;
import audio.SampleUserLocked;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import util.Web;

public class SampleHandler {
	static final Logger log = LogManager.getLogger();

	private final LabelsHandler labelsHandler;
	private final SpectrumHandler spectrumHandler;

	private final Broker broker;

	public SampleHandler(Broker broker) {
		this.broker = broker;
		labelsHandler = new LabelsHandler(broker);
		spectrumHandler = new SpectrumHandler(broker);
	}

	public void handle(String sampleText, String target, Request request, HttpServletResponse response) throws IOException {
		Sample sample = broker.samples().getThrow(sampleText);
		if(target.equals("/")) {
			handleRoot(sample, request, response);
		} else {
			int i = target.indexOf('/', 1);
			if(i == 1) {
				throw new RuntimeException("no name: "+target);
			}			
			String name = i < 0 ? target.substring(1) : target.substring(1, i);
			String next = i < 0 ? "/" : target.substring(i);
			switch(name) {
			case "labels":
				labelsHandler.handle(sample, next, request, response);
				break;
			case "spectrum":
				if(sample.isSampleUserLocked()) {
					return;
				}
				spectrumHandler.handle(sample, request, response);
				break;
			case "data":
				if(sample.isSampleUserLocked()) {
					return;
				}
				handleData(sample, request, response);
				break;
			case "meta":
				handleMeta(sample, request, response);
				break;
			default:
				throw new RuntimeException("no call");
			}			
		}		
	}

	private void handleRoot(Sample sample, Request request, HttpServletResponse response) throws IOException {
		switch(request.getMethod()) {
		case "POST":
			handleRoot_POST(sample, request, response);
			break;
		default:
			throw new RuntimeException("no call");
		}
	}

	private void handleRoot_POST(Sample sample, Request request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession(false);
		Account account = (Account) session.getAttribute("account");
		BitSet roleBits = (BitSet) session.getAttribute("roles");
		broker.roleManager().role_readOnly.checkHasNot(roleBits);

		JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
		JSONArray jsonActions = jsonReq.getJSONArray("actions");
		int jsonActionsLen = jsonActions.length();
		for (int i = 0; i < jsonActionsLen; i++) {
			JSONObject jsonAction = jsonActions.getJSONObject(i);
			String actionName = jsonAction.getString("action");
			switch(actionName) {
			case "set_locked": {					
				String username = account.username;
				long timestamp = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
				SampleUserLocked sampleUserLocked = new SampleUserLocked(username, timestamp);
				sample.setSampleUserLocked(sampleUserLocked);
				break;
			}
			default:
				throw new RuntimeException("unknown action:" + actionName);
			}
		}		

		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("result");
		json.value("OK");
		json.endObject();	
	}

	private void handleData(Sample sample, Request request, HttpServletResponse response) throws IOException {
		File file = sample.getAudioFile();
		String rangeText = request.getHeader("Range");

		double overwrite_sampling_rate = Web.getDouble(request, "overwrite_sampling_rate", Double.NaN);	

		if(!Double.isFinite(overwrite_sampling_rate) && !isAbove(file, 48000)) {
			sendFile(file, rangeText, response, "audio/wave");
		} else {
			//if(overwrite_sampling_rate <= 0d || overwrite_sampling_rate > 192000d) {
			if(overwrite_sampling_rate <= 0d || overwrite_sampling_rate > 1000000d) {
				throw new RuntimeException("invalid overwrite_sampling_rate");
			}
			File tempFile = File.createTempFile("audio_", ".wav");
			tempFile.deleteOnExit();
			try {	
				log.info(tempFile);	
				createOverwriteSamplingRate(sample.getAudioFile(), tempFile, (float) overwrite_sampling_rate);
				sendFile(tempFile, rangeText, response, "audio/wave");
			} catch (UnsupportedAudioFileException e) {
				throw new RuntimeException(e);
			} finally {
				tempFile.delete();
			}	
		}
	}

	private void sendFile(File file, String rangeText, HttpServletResponse response, String conentType) throws FileNotFoundException, IOException {
		long fileLen = file.length();

		if(rangeText == null) {
			if(conentType != null) {
				response.setContentType(conentType);
			}
			response.setContentLengthLong(fileLen);
			try(FileInputStream in = new FileInputStream(file)) {
				IO.copy(in, response.getOutputStream());
			}
		} else {
			if(rangeText.startsWith("bytes=")) {
				String rangeIntervalText = rangeText.substring(6);
				//log.info("rangeIntervalText |" + rangeIntervalText + "|");
				if(rangeIntervalText.contains(",")) {
					throw new RuntimeException("unknown Range header, multiple ranges not supported: " + rangeText);
				}
				int rangeIntervalTextSeperatorIndex = rangeIntervalText.indexOf("-");
				if(rangeIntervalTextSeperatorIndex < 0) {
					throw new RuntimeException("unknown Range header: " + rangeText);
				}
				String rangeStartText = rangeIntervalText.substring(0, rangeIntervalTextSeperatorIndex);
				String rangeEndText = rangeIntervalText.substring(rangeIntervalTextSeperatorIndex + 1);
				//log.info("rangeIntervalText |" + rangeStartText + "|" + rangeEndText + "|");
				if(rangeStartText.isEmpty()) {
					throw new RuntimeException("unknown Range header, suffix-length not supported: " + rangeText);
				}
				long rangeStart = Long.parseLong(rangeStartText);
				if(rangeStart < 0) {
					throw new RuntimeException("unknown Range header: " + rangeText);
				}
				long rangeEnd = rangeEndText.isEmpty() ? (fileLen - 1) : Long.parseLong(rangeEndText);
				if(rangeEnd < rangeStart) {
					throw new RuntimeException("unknown Range header: " + rangeText);
				}
				if(rangeEnd >= fileLen) {
					throw new RuntimeException("unknown Range header: " + rangeText);
				}
				long rangeLen = rangeEnd - rangeStart + 1;
				try(FileInputStream in = new FileInputStream(file)) {
					if(rangeStart != 0) {					
						in.skip(rangeStart);
					}
					response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
					if(conentType != null) {
						response.setContentType(conentType);
					}
					response.setContentLengthLong(fileLen);
					response.setHeader("Content-Range", "bytes "+ rangeStart +"-" + rangeEnd + "/" + fileLen);
					IO.copy(in, response.getOutputStream(), rangeLen);
				}
			} else {
				throw new RuntimeException("unknown Range header: " + rangeText);
			}
		}
	}
	
	private static float getSamplingRate(File file) {
		try(AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file)) {
			return audioInputStream.getFormat().getSampleRate();
		} catch (Exception e) {
			log.warn(e);
			return Float.NaN;
		}
	}
	
	public static boolean isAbove(File file, float samplingRate) {
		float sr = getSamplingRate(file);
		//log.info("sr " + sr);
		return Float.isFinite(sr) && sr > samplingRate;
	}

	private static void createOverwriteSamplingRate(File inFile, File outFile, float overwrite_sampling_rate) throws IOException, UnsupportedAudioFileException {		
		try(AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(inFile)) {
			AudioFormat originalAudioFormat = originalAudioInputStream.getFormat();
			Encoding encoding = originalAudioFormat.getEncoding();
			int sampleSizeInBits = originalAudioFormat.getSampleSizeInBits();
			int channels = originalAudioFormat.getChannels();
			int frameSize = originalAudioFormat.getFrameSize();
			float frameRate = originalAudioFormat.getFrameRate();
			boolean bigEndian = originalAudioFormat.isBigEndian();
			if(Float.isFinite(overwrite_sampling_rate)) {				
				float sampleRate = overwrite_sampling_rate;				
				AudioFormat audioFormat = new AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian);
				long sampleFrameLen = originalAudioInputStream.getFrameLength();
				try(AudioInputStream audioInputStream = new AudioInputStream(originalAudioInputStream, audioFormat, sampleFrameLen)) {
					if(sampleRate > 48000) {
						float resampledSampleRate = 48000;
						AudioFormat resampledAudioFormat = new AudioFormat(encoding, resampledSampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian);
						try(AudioInputStream resampledAudioInputStream = AudioSystem.getAudioInputStream(resampledAudioFormat, audioInputStream)) {
							AudioSystem.write(resampledAudioInputStream, Type.WAVE, outFile);
						}
					} else {
						AudioSystem.write(audioInputStream, Type.WAVE, outFile);
					}				
				}
			} else {
				float originalSampleRate = originalAudioFormat.getSampleRate();
				if(originalSampleRate > 48000) {
					float resampledSampleRate = 48000;
					AudioFormat resampledAudioFormat = new AudioFormat(encoding, resampledSampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian);
					try(AudioInputStream resampledAudioInputStream = AudioSystem.getAudioInputStream(resampledAudioFormat, originalAudioInputStream)) {
						AudioSystem.write(resampledAudioInputStream, Type.WAVE, outFile);
					}
				} else {
					AudioSystem.write(originalAudioInputStream, Type.WAVE, outFile);
				}
			}
		}	
	}

	private void handleMeta(Sample sample, Request request, HttpServletResponse response) throws IOException {
		//response.setContentType("text/yaml; charset=utf-8");
		response.setContentType("text/plain; charset=utf-8");

		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();

		LinkedHashMap<String, Object> yamlMapSample = new LinkedHashMap<String, Object>();
		yamlMap.put("sample", yamlMapSample);
		yamlMapSample.put("id", sample.id);
		yamlMapSample.put("directory", sample.directoryPath.toString());
		yamlMapSample.put("audio_file_name", sample.getAudioFileName());
		yamlMapSample.put("audio_file_size", sample.getAudioFile().length());

		yamlMap.put("meta", sample.getMetaMap().getInternalMap());

		new Yaml().dump(yamlMap, response.getWriter());

	}
}
