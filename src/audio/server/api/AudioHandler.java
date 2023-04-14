package audio.server.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;


import org.tinylog.Logger;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.IO;

import audio.AudioCache;
import audio.Broker;
import audio.GeneralSample;
import jakarta.servlet.http.HttpServletResponse;
import util.Web;

public class AudioHandler {
	

	private final Broker broker;
	private final AudioCache audioCache;

	public AudioHandler(Broker broker) {
		this.broker = broker;
		this.audioCache = new AudioCache(broker.config().audioConfig.audio_cache_max_files);
	}

	public void handle(GeneralSample sample, Request request, HttpServletResponse response) throws IOException {
		File file = sample.getAudioFile();
		String rangeText = request.getHeader("Range");
		
		boolean fOriginal = Web.getFlagBoolean(request, "original");

		double overwrite_sampling_rate = Web.getDouble(request, "overwrite_sampling_rate", Double.NaN);	

		if(fOriginal || (!Double.isFinite(overwrite_sampling_rate) && !isAbove(file, 48000))) {
			sendFile(file, rangeText, response, Web.MIME_WAVE);
		} else {
			//if(overwrite_sampling_rate <= 0d || overwrite_sampling_rate > 192000d) {
			if(overwrite_sampling_rate <= 0d || overwrite_sampling_rate > 1000000d) {
				throw new RuntimeException("invalid overwrite_sampling_rate");
			}
			/*File tempFile = File.createTempFile("audio_", ".wav");
			tempFile.deleteOnExit();
			try {	
				Logger.info(tempFile);	
				createOverwriteSamplingRate(sample.getAudioFile(), tempFile, (float) overwrite_sampling_rate);
				sendFile(tempFile, rangeText, response, "audio/wave");
			} catch (UnsupportedAudioFileException e) {
				throw new RuntimeException(e);
			} finally {
				tempFile.delete();
			}*/
			audioCache.run(sample.getAudioFile(), (float) overwrite_sampling_rate, rangeText, response);
		}
	}

	public static void sendFile(File file, String rangeText, HttpServletResponse response, String conentType) throws FileNotFoundException, IOException {
		long fileLen = file.length();
		//Logger.info("FILE "+ file.getPath() + "   " + fileLen);
		if(rangeText == null || fileLen == 0) {
			if(conentType != null) {
				response.setContentType(conentType);
			}
			response.setContentLengthLong(fileLen);
			try(FileInputStream in = new FileInputStream(file)) {
				Logger.info("send full  " + file);
				IO.copy(in, response.getOutputStream());
			} catch(EofException e) {
				Logger.info("remote connection closed");
			}
		} else {
			if(rangeText.startsWith("bytes=")) {
				String rangeIntervalText = rangeText.substring(6);
				//Logger.info("rangeIntervalText |" + rangeIntervalText + "|");
				if(rangeIntervalText.contains(",")) {
					throw new RuntimeException("unknown Range header, multiple ranges not supported: " + rangeText);
				}
				int rangeIntervalTextSeperatorIndex = rangeIntervalText.indexOf("-");
				if(rangeIntervalTextSeperatorIndex < 0) {
					throw new RuntimeException("unknown Range header: " + rangeText);
				}
				String rangeStartText = rangeIntervalText.substring(0, rangeIntervalTextSeperatorIndex);
				String rangeEndText = rangeIntervalText.substring(rangeIntervalTextSeperatorIndex + 1);
				//Logger.info("rangeIntervalText |" + rangeStartText + "|" + rangeEndText + "|");
				if(rangeStartText.isEmpty()) {
					throw new RuntimeException("unknown Range header, suffix-length not supported: " + rangeText);
				}
				long rangeStart = Long.parseLong(rangeStartText);
				if(rangeStart < 0) {
					throw new RuntimeException("unknown Range header: " + rangeText);
				}
				long rangeEnd = rangeEndText.isEmpty() ? (fileLen - 1) : Long.parseLong(rangeEndText);
				if(rangeEnd < rangeStart) {
					throw new RuntimeException("unknown Range header: |" + rangeText + "|      " + rangeStart + "   " + rangeEnd);
				}
				if(rangeEnd >= fileLen) {
					throw new RuntimeException("unknown Range header: " + rangeText);
				}
				long rangeLen = rangeEnd - rangeStart + 1;
				try(FileInputStream in = new FileInputStream(file)) {
					Logger.info("send range " + rangeStart + " .. " + rangeEnd + "  " + file);
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
				} catch(EofException e) {
					Logger.trace("remote connection closed");
				} catch (IOException e) {
					Throwable cause = e.getCause();
					if(cause != null && cause instanceof TimeoutException) {
						Logger.trace(cause.getMessage());
					} else {
						throw e;
					}
				}
			} else {
				throw new RuntimeException("unknown Range header: " + rangeText);
			}
		}
	}

	public static void createOverwriteSamplingRate(File inFile, File outFile, float overwrite_sampling_rate) throws IOException, UnsupportedAudioFileException {		
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

	private static boolean isAbove(File file, float samplingRate) {
		float sr = getSamplingRate(file);
		//Logger.info("sr " + sr);
		return Float.isFinite(sr) && sr > samplingRate;
	}

	private static float getSamplingRate(File file) {
		try(AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file)) {
			return audioInputStream.getFormat().getSampleRate();
		} catch (Exception e) {
			Logger.warn(e);
			return Float.NaN;
		}
	}
}
