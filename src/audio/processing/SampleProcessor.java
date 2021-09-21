package audio.processing;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jtransforms.fft.FloatFFT_1D;

import audio.Sample;

public class SampleProcessor {
	static final Logger log = LogManager.getLogger();

	public static final int n = 1024;
	public static final int n2 = n / 2;
	public static final int step = 256;

	private final Sample sample;
	public short[] data;
	public int dataLength;
	public double sampleRate;
	public float[][] fq;	
	public float[][] bins;

	private int binRows;

	public int fqCols;
	private double sampleRateN2;
	private double binFactor;

	public SampleProcessor(Sample sample) {
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

	public void loadData(int additionalSpace) {
		loadData(additionalSpace, Double.NaN, Double.NaN);
	}

	public void loadData(int additionalSpace, double startSecond, double endSecond) {
		try(AudioInputStream in = AudioSystem.getAudioInputStream(sample.getAudioFile())) {			
			AudioFormat audioFormat = in.getFormat();
			//log.info("Format: " + audioFormat);
			//log.info("FrameLength: " + in.getFrameLength());
			//log.info("rate: " + audioFormat.getFrameRate() + "   " + audioFormat.getSampleRate());

			this.sampleRate = audioFormat.getSampleRate();

			if(audioFormat.getChannels() != 1) {
				throw new RuntimeException("currently for audio only one channel is supported (mono).");
			}

			Encoding audioEncoding = audioFormat.getEncoding();			
			if(audioEncoding != Encoding.PCM_SIGNED) {
				throw new RuntimeException("currently audio in PCM_SIGNED encoding is supported.");
			}

			if(audioFormat.getSampleSizeInBits() != 16) {
				throw new RuntimeException("currently for audio only samples of 16 bit are supported.");
			}

			if(audioFormat.getFrameSize() != 2) {
				throw new RuntimeException("currently for audio only frame size of 2 bytes is supported (PCM_SIGNED 16 bit mono).");
			}

			int frameLength = (int) in.getFrameLength();

			int start = Double.isFinite(startSecond) ? secondsToPos(startSecond) : 0;
			int end = Double.isFinite(endSecond) ? secondsToPos(endSecond) : frameLength - 1;

			if(end < start) {
				throw new RuntimeException("invalid interval");
			}
			
			this.dataLength = end - start + 1;

			int MAX_SAMPLES = 512 * 1024 * 1024;
			
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
			log.info("allocate data array for spectrum: " + fullBytesLen);
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

		} catch (UnsupportedAudioFileException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static float[] getGaussianWeights(int n) {
		double upper = n;
		double mean = (upper - 1d) / 2d;

		Gaussian gaussian = new Gaussian(mean, upper / 5d);

		int len = (int) upper;
		int mid = len / 2;
		float[] weight = new float[len];

		double vmid = gaussian.value(mid);

		for (int i = 0; i < len; i++) {
			weight[i] = (float) (gaussian.value(i) / vmid);
		}

		return weight;
	}

	public void transform() {
		fqCols = ((dataLength - n) / step) + 1;
		float[][] fq = new float[fqCols][n];
		FloatFFT_1D fft = new FloatFFT_1D(n);
		float[] weight = SampleProcessor.getGaussianWeights(n);		
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