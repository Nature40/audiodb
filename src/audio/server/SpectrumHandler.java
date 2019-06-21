package audio.server;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.jtransforms.fft.FloatFFT_1D;

public class SpectrumHandler extends AbstractHandler {
	static final Logger log = LogManager.getLogger();

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		baseRequest.setHandled(true);
		target = target.replaceAll("/", "");
		target = target.replaceAll("\\\\", "");
		log.info("spectrum " + target);

		try {
			AudioInputStream in = AudioSystem.getAudioInputStream(new File("data/" + target));
			log.info("Format: " + in.getFormat());
			log.info("FrameLength: " + in.getFrameLength());

			int cutoff = 320;

			int n = 1024;
			int step = 256;
			int frameLength = (int) in.getFrameLength();				
			int cols = (int) (frameLength / step);

			byte[] fullBytes = new byte[(int) (frameLength*2) + (n*2)];
			in.read(fullBytes, 0, (int) (frameLength*2));
			short[] fullShorts = new short[fullBytes.length / 2];
			ByteBuffer.wrap(fullBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(fullShorts);				

			FloatFFT_1D fft = new FloatFFT_1D(n);
			float[] weight = getGaussianWeights(n);

			BufferedImage bi = new BufferedImage(cols, cutoff, 1);
			Graphics2D g = bi.createGraphics();
			for (int pos = 0; pos < cols; pos++) {
				float[] a = new float[n];
				for (int i = 0; i < n; i++) {
					//a[i] = fullShorts[pos*step + i];
					a[i] = fullShorts[pos*step + i] * weight[i];
				}
				fft.realForward(a);
				for (int i = 0; i < cutoff; i++) {
					//float v = Math.abs(a[i*2+1]) / 40000f;
					float v = (float) Math.pow(Math.abs(a[i*2+1]), 0.5d) / 200f;
					if(v < 0f) {
						v = 0f;
					}
					if(v > 1f) {
						v = 1f;
					}
					g.setColor(new Color(v, v, v));				
					g.drawLine(pos , cutoff - i - 1, pos, cutoff - i);
				}
			}
			g.dispose();
			ImageIO.write(bi, "png", response.getOutputStream());

			/*log.info(Arrays.toString(a));
			fft.realForward(a);
			log.info(Arrays.toString(a));
			fft.realInverse(a, true);
			log.info(Arrays.toString(a));*/

		} catch (UnsupportedAudioFileException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static float[] getGaussianWeights(int n) {
		double upper = n;
		double mean = (upper - 1d) / 2d;
		
		Gaussian gaussian = new Gaussian(mean, upper / 4d);
		
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