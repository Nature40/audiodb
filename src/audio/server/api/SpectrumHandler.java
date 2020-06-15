package audio.server.api;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.jtransforms.fft.FloatFFT_1D;

import audio.Broker;
import audio.Sample;
import audio.server.Renderer;
import util.Web;
import util.image.ImageRGBA;
import util.image.Lut;

public class SpectrumHandler {
	static final Logger log = LogManager.getLogger();
	
	private final Broker broker;

	public SpectrumHandler(Broker broker) {
		this.broker = broker;
	}

	public void handle(Sample sample, Request request, HttpServletResponse response) throws IOException {
		try {
			
			
			
			AudioInputStream in = AudioSystem.getAudioInputStream(sample.file());
			AudioFormat audioFormat = in.getFormat();
			log.info("Format: " + audioFormat);
			log.info("FrameLength: " + in.getFrameLength());
			log.info("rate: " + audioFormat.getFrameRate() + "   " + audioFormat.getSampleRate());
			
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
			
			float threshold = (float) Web.getDouble(request, "threshold", 12);
			int n = 1024;
			int step = 256;
			
			int cutoff = Web.getInt(request, "cutoff", 320);
			if(cutoff > n/2) {
				log.warn("cutoff out of bounds: " + cutoff + "  set to " + n/2);
				cutoff = n/2;
			}
			
			int frameLength = (int) in.getFrameLength();				
			int cols = (int) (frameLength / step);

			byte[] fullBytes = new byte[(int) (frameLength*2) + (n*2)];
			in.read(fullBytes, 0, (int) (frameLength*2));
			short[] fullShorts = new short[fullBytes.length / 2];
			
			ByteBuffer byteBuffer = ByteBuffer.wrap(fullBytes);
			if(audioFormat.isBigEndian()) {
				byteBuffer.order(ByteOrder.BIG_ENDIAN);
			} else {
				byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			}
			byteBuffer.asShortBuffer().get(fullShorts);				

			FloatFFT_1D fft = new FloatFFT_1D(n);
			float[] weight = getGaussianWeights(n);

			float[][] transformed = new float[cols][n];
			float minv = Float.MAX_VALUE;
			float maxv = -Float.MAX_VALUE;
			for (int pos = 0; pos < cols; pos++) {
				float[] a = transformed[pos];
				for (int i = 0; i < n; i++) {
					a[i] = fullShorts[pos*step + i] * weight[i];
				}
				fft.realForward(a);
				for (int i = 0; i < cutoff; i++) {
					float v = (float) Math.log(a[i*2]*a[i*2]+a[i*2+1]*a[i*2+1] + Float.MIN_VALUE);
					if(v >= threshold) {
						if(v < minv) {
							minv = v;
						}
						if(maxv < v) {
							maxv = v;
						}
					}					
				}
			}
			
			float[] lut = Lut.getGammaLUT256f(minv, maxv, 1.2);			
			ImageRGBA image = new ImageRGBA(cols, cutoff);
			int[] dst = image.getRawArray();
			for (int pos = 0; pos < cols; pos++) {
				float[] a = transformed[pos];
				for (int i = 0; i < cutoff; i++) {
					float v = (float) Math.log(a[i*2]*a[i*2]+a[i*2+1]*a[i*2+1] + Float.MIN_VALUE);
					int c = Renderer.colInferno[Lut.match256f(lut, v)];
					dst[(cutoff - i - 1) * cols + pos] = c;
				}
			}
			image.writePngCompressed(response.getOutputStream());

		} catch (UnsupportedAudioFileException e) {
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
}