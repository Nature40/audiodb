package audio.server.api;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.IO;
import org.tinylog.Logger;

import audio.AudioCache;
import audio.Broker;
import audio.GeneralSample;
import audio.RiffWriter;
import audio.task.Task_audio_create_yaml;
import jakarta.servlet.http.HttpServletResponse;
import util.Web;

public class AudioHandler {

	private final Broker broker;
	private final AudioCache audioCache;

	public AudioHandler(Broker broker) {
		this.broker = broker;
		this.audioCache = broker.audioCache();
	}

	public void handle(GeneralSample sample, Request request, HttpServletResponse response) throws IOException {
		File file = sample.getAudioFile();

		if(Task_audio_create_yaml.isQoa(file.getName())){
			file = audioCache.runDecode(file);
		}

		String rangeText = request.getHeader("Range");

		boolean fOriginal = Web.getFlagBoolean(request, "original");

		double overwrite_sampling_rate = Web.getDouble(request, "overwrite_sampling_rate", Double.NaN);	

		if(Task_audio_create_yaml.isWav(file.getName()) && (fOriginal || (!Double.isFinite(overwrite_sampling_rate) && !isAbove(file, 48000)))) {
			sendFile(file, rangeText, response, Web.MIME_WAVE);
		} else {
			//if(overwrite_sampling_rate <= 0d || overwrite_sampling_rate > 192000d) {
			if(overwrite_sampling_rate <= 0d || overwrite_sampling_rate > 1000000d) {
				throw new RuntimeException("invalid overwrite_sampling_rate");
			}
			audioCache.run(file, (float) overwrite_sampling_rate, rangeText, response);
		}
	}

	public static void sendFile(File file, String rangeText, HttpServletResponse response, String conentType) throws IOException {
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

	public static void decodeQoa(File inFile, File outFile, int overwriteSampleRate) throws FileNotFoundException, IOException {
		try(FileInputStream rawIn = new FileInputStream(inFile)) {
			try(BufferedInputStream in = new BufferedInputStream(rawIn)) {
			StreamQOADecoder dec = new StreamQOADecoder(in);
			if(dec.readHeader()) {
				try(FileOutputStream rawOut = new FileOutputStream(outFile)) {
					try(BufferedOutputStream out = new BufferedOutputStream(rawOut)) {
					int samplesLen = dec.getTotalSamples();
					int sampleRate = overwriteSampleRate > 0 ? overwriteSampleRate : dec.getSampleRate();					
					RiffWriter riffWriter = new RiffWriter(samplesLen, sampleRate, dec.getChannels(), out);
					riffWriter.writeHeader();
					short[] samples = new short[5120];
					for(;;) {
						if(riffWriter.hasWrittenAllSamples()) {
							break;
						}
						int frameSamples = dec.readFrame(samples);
						if(frameSamples < 0) {
							Logger.warn("decode error");
							break;
						}
						riffWriter.writeSamples(samples, frameSamples);
					}
					if(!riffWriter.hasWrittenAllSamples()) {
						throw new RuntimeException("not all samples processed");
					}
				}
			}
			} else {
				throw new RuntimeException("not valid QOA file");
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
		if(Task_audio_create_yaml.isWav(file.getName())) {
			try(AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file)) {
				return audioInputStream.getFormat().getSampleRate();
			} catch (Exception e) {
				Logger.warn(e);
				return Float.NaN;
			}
		} else if(Task_audio_create_yaml.isQoa(file.getName())) {
			try(FileInputStream in = new FileInputStream(file)) {
				StreamQOADecoder dec = new StreamQOADecoder(in);
				if(dec.readHeader()) {
					return dec.getSampleRate();
				} else {
					Logger.warn("not valid QOA file: " + file.getName());
					return Float.NaN;
				}
			} catch (FileNotFoundException e) {
				Logger.warn(e.getMessage());
				return Float.NaN;
			} catch (IOException e) {
				Logger.warn(e.getMessage());
				return Float.NaN;
			}
		} else {
			Logger.warn("unknown audio file format: " + file.getName());
			return Float.NaN;
		}
	}
}
