package audio.processing;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.math3.analysis.function.Gaussian;
import org.jtransforms.fft.FloatFFT_1D;
import org.tinylog.Logger;

import audio.GeneralSample;
import fr.delthas.javamp3.Sound;
import io.nayuki.flac.common.StreamInfo;
import io.nayuki.flac.decode.FlacDecoder;

public class SampleProcessor {

	private static final int MAX_SAMPLES = 512 * 1024 * 1024;

	public static final int n = 1024;
	public static final int n2 = n / 2;
	public static final int step = 256;

	private final GeneralSample sample;
	public short[] data;
	public int dataLength;
	public double sampleRate;
	public float[][] fq;	
	public float[][] bins;

	private int binRows;

	public int fqCols;
	private double sampleRateN2;
	private double binFactor;

	public SampleProcessor(GeneralSample sample) {
		this.sample = sample;
	}

	public int secondsToPos(double seconds) {
		return (int) Math.floor(seconds * sampleRate);
	}

	public double posToSeconds(int pos) {
		return pos / sampleRate;
	}

	public int frequencyToIndex(double frequency) {
		return (int) (frequency / sampleRateN2);
	}

	public double indexToFrequency(int index) {
		return index * sampleRateN2;
	}

	public int indexToBin(int index) {
		return (int) (index * binFactor);
	}

	public int timeToCol(int t) {
		return Math.floorDiv(t, step);
	}

	public int colToTime(int col) {
		return col * step;
	}

	public int getFrameLength() {
		try(AudioInputStream in = AudioSystem.getAudioInputStream(sample.getAudioFile())) {	
			int frameLength = (int) in.getFrameLength();			
			return frameLength;
		} catch (UnsupportedAudioFileException | IOException e) {
			throw new RuntimeException(e);
		}		
	}

	public void loadData(int additionalSpace) {
		int start = 0;
		int end = getFrameLength() - 1;
		loadData(0, start, end);
	}

	public void loadData(int additionalSpace, int start, int end) {
		File audioFile = sample.getAudioFile();
		String filename = audioFile.getName();
		boolean unsupportedAudioFile = false;
		try(AudioInputStream in = AudioSystem.getAudioInputStream(audioFile)) {			
			loadDataAudioInputStream(in, additionalSpace, start, end);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedAudioFileException e) {
			unsupportedAudioFile = true;
		}
		if(unsupportedAudioFile && filename.toLowerCase().endsWith("mp3")) {
			loadDataMp3(additionalSpace, start, end);
		} else if(unsupportedAudioFile && filename.toLowerCase().endsWith("flac")) {
			loadDataFlac(additionalSpace, start, end);
		} else if(unsupportedAudioFile) {
			throw new RuntimeException("could not read audio data");
		}
	}

	public void loadDataAudioInputStream(AudioInputStream in, int additionalSpace, int start, int end) throws IOException {
		AudioFormat audioFormat = in.getFormat();
		//Logger.info("Format: " + audioFormat);
		//Logger.info("FrameLength: " + in.getFrameLength());
		//Logger.info("rate: " + audioFormat.getFrameRate() + "   " + audioFormat.getSampleRate());

		this.sampleRate = audioFormat.getSampleRate();

		if(audioFormat.getChannels() != 1) {
			throw new RuntimeException("currently for audio only one channel is supported (mono).");
		}

		Encoding audioEncoding = audioFormat.getEncoding();			
		if(audioEncoding != Encoding.PCM_SIGNED) {
			throw new RuntimeException("currently audio in PCM_SIGNED encoding is supported.");
			//Logger.warn("currently audio in PCM_SIGNED encoding is supported.");
		}

		if(audioFormat.getSampleSizeInBits() != 16) {
			throw new RuntimeException("currently for audio only samples of 16 bit are supported: " + audioFormat.getSampleSizeInBits());
			//Logger.warn("currently for audio only samples of 16 bit are supported: " + audioFormat.getSampleSizeInBits());
		}

		if(audioFormat.getFrameSize() != 2) {
			throw new RuntimeException("currently for audio only frame size of 2 bytes is supported (PCM_SIGNED 16 bit mono) : " + audioFormat.getFrameSize());
			//Logger.warn("currently for audio only frame size of 2 bytes is supported (PCM_SIGNED 16 bit mono) : " + audioFormat.getFrameSize());
		}

		int frameLength = (int) in.getFrameLength();
		//Logger.info("frameLength " + frameLength);

		if(end < start) {
			throw new RuntimeException("invalid interval  " + start + "    " + end);
		}

		this.dataLength = end - start + 1;		

		if(dataLength > MAX_SAMPLES) {
			throw new RuntimeException("interval is too large: " + dataLength + "    max allowed " + MAX_SAMPLES);
		}

		if(start > Integer.MAX_VALUE - MAX_SAMPLES || end > start + MAX_SAMPLES) {
			throw new RuntimeException("invalid interval: " + start +  " " + end + "   " + dataLength);
		}

		int readStart = start < 0 ? 0 : start;
		int readEnd = end >= frameLength ? frameLength - 1 : end;
		int readLen = readEnd - readStart + 1;

		int off = start < 0 ? -start : 0;

		int fullBytesLen = (int) ((dataLength + additionalSpace) * 2);
		//Logger.info("allocate data array for spectrum: " + fullBytesLen);
		byte[] fullBytes = new byte[fullBytesLen];
		if(readStart > 0) {
			in.skip(readStart * 2);
		}
		in.read(fullBytes, off * 2, readLen * 2);
		short[] fullShorts = new short[fullBytes.length / 2];

		ByteBuffer byteBuffer = ByteBuffer.wrap(fullBytes);
		if(audioFormat.isBigEndian()) {
			byteBuffer.order(ByteOrder.BIG_ENDIAN);
		} else {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		}
		byteBuffer.asShortBuffer().get(fullShorts);				
		this.data = fullShorts;			

		sampleRateN2 = sampleRate / n / 2d;
		binFactor = sampleRateN2 / 1000d;		
	}

	public void loadDataMp3(int additionalSpace, int start, int end) {		
		try(Sound sound = new Sound(new BufferedInputStream(new FileInputStream(sample.getAudioFile())))) {	
			AudioFormat audioFormat = sound.getAudioFormat();
			Logger.info(audioFormat);
			ByteArrayOutputStream tmpOut = new ByteArrayOutputStream();							
			sound.decodeFullyInto(tmpOut);
			byte[] tmpArray = tmpOut.toByteArray();				
			Logger.info(tmpArray.length);
			ByteArrayInputStream tmpIn = new ByteArrayInputStream(tmpArray);							
			AudioInputStream audioInputStream = new AudioInputStream(tmpIn, audioFormat, tmpArray.length);
			File tmpFile = new File("tmpOut.wav");
			AudioSystem.write(audioInputStream, Type.WAVE, tmpFile);
			try(AudioInputStream in = AudioSystem.getAudioInputStream(tmpFile)) {			
				loadDataAudioInputStream(in, additionalSpace, start, end);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (UnsupportedAudioFileException e) {
				throw new RuntimeException(e);
			}
		} catch (FileNotFoundException e1) {
			throw new RuntimeException(e1);
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}		
	}

	public void loadDataFlac(int additionalSpace, int start, int end) {
		try (FlacDecoder dec = new FlacDecoder(sample.getAudioFile())) {			
			while (true) {
				Object[] mb = dec.readAndHandleMetadataBlock();
				if(mb == null) {
					break;
				}
				int mbType = (int) mb[0];
				byte[] mbData = (byte[]) mb[1];
				
				switch(mbType) {
				case 0:
					Logger.info("mmType " + mbType + " STREAMINFO" + "  len " + mbData.length);
					break;
				case 1:
					Logger.info("mmType " + mbType + " APPLICATION" + "  len " + mbData.length);
					break;
				case 2:
					Logger.info("mmType " + mbType + " PADDING"  + "  len " + mbData.length);
					break;					
				case 3:
					Logger.info("mmType " + mbType + " SEEKTABLE" + "  len "  + mbData.length);
					break;
				case 4:
					Logger.info("mmType " + mbType + " VORBIS_COMMENT" + "  len " + mbData.length);
					break;
				case 5:
					Logger.info("mmType " + mbType + " CUESHEET" + "  len " + mbData.length);
					break;
				case 6:
					Logger.info("mmType " + mbType + " PICTURE" + "  len " + mbData.length);
					break;
				default:
					Logger.info("mmType " + mbType + "  len " + mbData.length);
				}
				
				Logger.info(new String(mbData, StandardCharsets.UTF_8));
				Logger.info(Arrays.toString(mbData));
			}
			StreamInfo streamInfo = dec.streamInfo;
			if (streamInfo.sampleDepth != 16) {
				throw new RuntimeException("Only 16 bit samples supported.");
			}
			if (streamInfo.numChannels != 1) {
				throw new RuntimeException("Only 1 channel supported.");
			}
			Logger.info("streamInfo.numSamples " + streamInfo.numSamples);

			int[][] samples = new int[1][(int)streamInfo.numSamples];
			{
				int pos = 0;
				while(true) {
					int len = dec.readAudioBlock(samples, pos);
					if (len == 0) {
						break;
					}
					pos += len;
				}
				if(streamInfo.numSamples != pos) {
					Logger.info("read error " + pos + "   " + streamInfo.numSamples);
				}
			}

			int readStart = start < 0 ? 0 : start;
			int readEnd = (int) (end >= streamInfo.numSamples ? streamInfo.numSamples - 1 : end);
			this.dataLength = readEnd - readStart + 1;		

			if(this.dataLength > MAX_SAMPLES) {
				throw new RuntimeException("interval is too large: " + dataLength + "    max allowed " + MAX_SAMPLES);
			}

			if(readStart > Integer.MAX_VALUE - MAX_SAMPLES) {
				throw new RuntimeException("invalid interval: " + readStart +  " " + readEnd + "   " + dataLength);
			}

			short[] fullShorts = new short[dataLength + additionalSpace];			
			int[] src = samples[0];
			for(int pos = 0, i = readStart; i <= readEnd; i++) {
				fullShorts[pos++] = (short) src[i];
			}

			this.data = fullShorts;			

			sampleRateN2 = sampleRate / n / 2d;
			binFactor = sampleRateN2 / 1000d;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static float[] getGaussianWeights(int n) {
		double upper = n;
		double mean = (upper - 1d) / 2d;
		double sigma = upper / 5d;

		Gaussian gaussian = new Gaussian(mean, sigma);

		int len = (int) upper;
		int mid = len / 2;
		float[] weight = new float[len];

		double vmid = gaussian.value(mid);

		for (int i = 0; i < len; i++) {
			weight[i] = (float) (gaussian.value(i) / vmid);
			//weight[i] *= weight[i];
		}
		//Logger.info(Arrays.toString(weight));
		/*try(CsvWriter csv = CsvWriter.builder().build(Paths.get("output","weights.csv"))) {
			csv.writeRow("weight");
			for(float w : weight) {
				csv.writeRow(Float.toString(w));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		return weight;
	}

	public static float[] getGaussianWeights(int n, int step) {
		double upper = n;
		double mean = (upper - 1d) / 2d;
		//double sigma = upper / 5d;
		//double sigma = step;
		double f = ((double)n) / ((double)step);
		double sigma = f >= 16d ? (step * 2d) : f > 4d ? step : (step / 2d);

		Gaussian gaussian = new Gaussian(mean, sigma);

		int len = (int) upper;
		int mid = len / 2;
		float[] weight = new float[len];

		double vmid = gaussian.value(mid);

		for (int i = 0; i < len; i++) {
			weight[i] = (float) (gaussian.value(i) / vmid);
			//weight[i] *= weight[i];
		}
		//Logger.info(Arrays.toString(weight));
		/*try(CsvWriter csv = CsvWriter.builder().build(Paths.get("output","weights.csv"))) {
			csv.writeRow("weight");
			for(float w : weight) {
				csv.writeRow(Float.toString(w));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		return weight;
	}

	public void transform() {
		fqCols = ((dataLength - n) / step) + 1;
		float[][] fq = new float[fqCols][n];
		FloatFFT_1D fft = new FloatFFT_1D(n);
		float[] weight = SampleProcessor.getGaussianWeights(n, step);		
		float[] a = new float[n];
		for (int pos = 0; pos < fqCols; pos++) {			
			for (int i = 0; i < n; i++) {
				a[i] = data[pos*step + i] * weight[i];
			}
			fft.realForward(a);
			float[] f = fq[pos];
			for (int i = 0; i < n2; i++) {
				f[i] = a[i*2]*a[i*2]+a[i*2+1]*a[i*2+1];
			}
		}
		this.fq = fq;
	}

	public void calcBins() {
		binRows = indexToBin(n2 - 1) + 1;
		bins = new float[fqCols][binRows];
		for (int pos = 0; pos < fqCols; pos++) {			
			float[] f = fq[pos];
			float[] b = bins[pos];
			for (int i = 0; i < n2; i++) {
				int bin = indexToBin(i);
				b[bin] += f[i];
			}
		}
	}

}