package audio.server.api;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.jtransforms.fft.FloatFFT_1D;

import audio.Broker;
import audio.GeneralSample;
import audio.Sample;
import audio.processing.SampleProcessor;
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

	public void handle(GeneralSample sample, Request request, HttpServletResponse response) throws IOException {
		int window = 1024;
		//int n = 2048;
		//int n = 512;

		window = Web.getInt(request, "window", window);

		int cutoff = Web.getInt(request, "cutoff", 320);
		if(cutoff > window/2) {
			log.warn("cutoff out of bounds: " + cutoff + "  set to " + window/2);
			cutoff = window/2;
		}
		float threshold = (float) Web.getDouble(request, "threshold", 12);

		int shrink_factor = Web.getInt(request, "shrink_factor", 1);
		int width = Web.getInt(request, "width", 0);
		int max_width = Web.getInt(request, "max_width", 0);

		int startSample = Web.getInt(request, "start_sample", -1);
		int endSample = Web.getInt(request, "end_sample", -1);	

		//log.info("spectrum " + startSample + " to " + endSample);

		SampleProcessor sampleProcessor = new SampleProcessor(sample);
		if(startSample < 0) {
			double startSecond = Web.getDouble(request, "start", Double.NaN);
			startSample = Double.isFinite(startSecond) ? sampleProcessor.secondsToPos(startSecond) : 0;
		}
		if(endSample < 0) {
			double endSecond = Web.getDouble(request, "end", Double.NaN);
			endSample = Double.isFinite(endSecond) ? sampleProcessor.secondsToPos(endSecond) : sampleProcessor.getFrameLength() - 1;
		}
		//log.info("start " + startSample + "  end " + endSample);
		sampleProcessor.loadData(0, startSample, endSample);
		short[] fullShorts = sampleProcessor.data;
		/*log.info(fullShorts.length + "  " + window);
		for (int i = 0; i < fullShorts.length; i++) {
			fullShorts[i] = i%2 == 0 ? Short.MAX_VALUE : -Short.MAX_VALUE;
		}*/

		int step = 256;
		//int step = 512;
		//int step = 1024;
		step = Web.getInt(request, "step", step);

		float intensity_max = 23f;
		intensity_max = Web.getFloat(request, "intensity_max", intensity_max);

		int cols = ((sampleProcessor.dataLength - window) / (step * shrink_factor)) + 1;

		ImageRGBA image;
		int renderWidth = 0;
		boolean isMaxWidth = false; 
		if(width > 0) {
			renderWidth = width;
		} else if(max_width > 0 && cols > max_width) {
			renderWidth = max_width;
			isMaxWidth = true;
		}

		if(shrink_factor > 1) {
			image = render3Shrink(fullShorts, window, step, cols, cutoff, threshold, intensity_max, shrink_factor);
		} else if(renderWidth <= 0) {
			image = render3(fullShorts, window, step, cols, cutoff, threshold, intensity_max);
			//image = render3denoise(fullShorts, window, step, cols, cutoff, threshold, intensity_max);
		} else if(isMaxWidth){
			step = window;
			cols = ((sampleProcessor.dataLength - window) / step) + 1;
			if(cols <= renderWidth) {
				image = render3(fullShorts, window, step, cols, cutoff, threshold, intensity_max);
			} else {
				image = render3width(fullShorts, window, step, cols, cutoff, threshold, intensity_max, renderWidth);
			}
		} else {
			image = render3width(fullShorts, window, step, cols, cutoff, threshold, intensity_max, renderWidth);
		}

		image.writePngCompressed(response.getOutputStream());
	}

	private ImageRGBA render2(short[] fullShorts, int n, int step, int cols, int cutoff, float threshold) {
		log.info("render2 step " + step + "  cols " + cols);
		FloatFFT_1D fft = new FloatFFT_1D(n);
		float[] weight = SampleProcessor.getGaussianWeights(n);
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
				a[i] = v;
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

		log.info(minv + "  " + maxv);

		float[] lut = Lut.getGammaLUT256f(minv, maxv, 1.2);			
		ImageRGBA image = new ImageRGBA(cols, cutoff);
		int[] dst = image.getRawArray();
		for (int pos = 0; pos < cols; pos++) {
			float[] a = transformed[pos];
			for (int i = 0; i < cutoff; i++) {
				float v = a[i];
				int c = Renderer.colInferno[Lut.match256f(lut, v)];
				dst[(cutoff - i - 1) * cols + pos] = c;
			}
		}

		return image;
	}

	private ImageRGBA render3(short[] fullShorts, int n, int step, int cols, int cutoff, float threshold, float maxv) {
		//log.info("render3 step " + step + "  cols " + cols);
		FloatFFT_1D fft = new FloatFFT_1D(n);
		float[] weight = SampleProcessor.getGaussianWeights(n);

		/*float energy = 0;
		for (int i = 0; i < n; i++) {
			float v = weight[i] * Short.MAX_VALUE;
			energy += v*v;
		}
		float energy_per_sample = energy / n;
		log.info("energy " +  energy + "   " + Math.log(energy));
		log.info("energy sample " +  energy_per_sample + "   " + Math.log(energy_per_sample));*/

		float[] lut = Lut.getLogLUT256fLogMinMax(threshold, maxv);
		ImageRGBA image = new ImageRGBA(cols, cutoff);
		int[] dst = image.getRawArray();
		float[] a = new float[n];
		for (int pos = 0; pos < cols; pos++) {			
			for (int i = 0; i < n; i++) {
				a[i] = fullShorts[pos*step + i] * weight[i];
			}
			fft.realForward(a);
			/*float vmax = 0;
			for (int i = 0; i < n/2; i++) {
				float v = a[i*2]*a[i*2]+a[i*2+1]*a[i*2+1];
				if(vmax < v) {
					vmax = v;
				}
			}
			log.info("vmax " + vmax + "   " + Math.log(vmax) + "vmax " + vmax/n + "   " + Math.log(vmax/n));*/
			for (int i = 0; i < cutoff; i++) {
				float v = a[i*2]*a[i*2]+a[i*2+1]*a[i*2+1];
				int c = Renderer.colInferno[Lut.match256f(lut, v)];
				dst[(cutoff - i - 1) * cols + pos] = c;
			}
		}

		return image;
	}

	private ImageRGBA render3denoise(short[] fullShorts, int n, int step, int cols, int cutoff, float threshold, float maxv) {
		//log.info("render3 step " + step + "  cols " + cols);
		FloatFFT_1D fft = new FloatFFT_1D(n);
		float[] weight = SampleProcessor.getGaussianWeights(n);

		float[] lut = Lut.getLogLUT256fLogMinMax(threshold, maxv);
		ImageRGBA image = new ImageRGBA(cols, cutoff);
		int[] dst = image.getRawArray();
		float[] a = new float[n];
		float[][] bb = new float[cols][cutoff];
		float[] sum = new float[cutoff];
		for (int pos = 0; pos < cols; pos++) {
			for (int i = 0; i < n; i++) {
				a[i] = fullShorts[pos*step + i] * weight[i];
			}
			fft.realForward(a);
			float[] b = bb[pos];
			for (int i = 0; i < cutoff; i++) {
				float v = a[i*2]*a[i*2]+a[i*2+1]*a[i*2+1];
				b[i] = v;
				sum[i] += v;
			}
		}
		for (int i = 0; i < cutoff; i++) {
			sum[i] = (sum[i] / cutoff) * 1.1f;
		}
		for (int pos = 0; pos < cols; pos++) {
			float[] b = bb[pos];
			for (int i = 0; i < cutoff; i++) {
				float v = b[i];
				if(sum[i] <= v) {
					int c = Renderer.colInferno[Lut.match256f(lut, v)];
					dst[(cutoff - i - 1) * cols + pos] = c;
				}
			}
		}

		return image;
	}

	private ImageRGBA render3width(short[] fullShorts, int n, int step, int cols, int cutoff, float threshold, float maxv, int width) {
		log.info("render3width step " + step + "  cols " + cols);
		FloatFFT_1D fft = new FloatFFT_1D(n);
		float[] weight = SampleProcessor.getGaussianWeights(n);

		float[][] target = new float[width][cutoff];

		float[] lut = Lut.getLogLUT256fLogMinMax(threshold, maxv);
		ImageRGBA image = new ImageRGBA(width, cutoff);
		int[] dst = image.getRawArray();
		float[] a = new float[n];
		for (int pos = 0; pos < cols; pos++) {			
			for (int i = 0; i < n; i++) {
				a[i] = fullShorts[pos*step + i] * weight[i];
			}
			fft.realForward(a);
			float[] t = target[(int) Math.floor(((pos + 0.5d) * width) / cols)];
			for (int i = 0; i < cutoff; i++) {
				float v = a[i*2]*a[i*2]+a[i*2+1]*a[i*2+1];
				t[i] = Math.max(t[i], v);
				/*if(t[i] < v) { // slower
					t[i] = v;
				}*/
			}
		}

		for (int pos = 0; pos < width; pos++) {			
			float[] t = target[pos];
			for (int i = 0; i < cutoff; i++) {
				float v = t[i];
				int c = Renderer.colInferno[Lut.match256f(lut, v)];
				dst[(cutoff - i - 1) * width + pos] = c;
			}
		}

		return image;
	}

	private ImageRGBA render3Shrink(short[] fullShorts, int n, int step, int cols, int cutoff, float threshold, float maxv, int shrinkFactor) {
		//log.info("render3 step " + step + "  cols " + cols);
		FloatFFT_1D fft = new FloatFFT_1D(n);
		float[] weight = SampleProcessor.getGaussianWeights(n);

		float[] lut = Lut.getLogLUT256fLogMinMax(threshold, maxv);
		ImageRGBA image = new ImageRGBA(cols, cutoff);
		int[] dst = image.getRawArray();
		float[] a = new float[n];
		float[] b = new float[cutoff];
		for (int colPos = 0; colPos < cols; colPos++) {
			for (int i = 0; i < cutoff; i++) {
				b[i] = 0f;
			}
			for(int shrinkPos = 0; shrinkPos < shrinkFactor; shrinkPos++) {
				int fftPos = colPos * step * shrinkFactor + shrinkPos * step;
				for (int i = 0; i < n; i++) {
					a[i] = fullShorts[fftPos + i] * weight[i];
				}
				fft.realForward(a);
				for (int i = 0; i < cutoff; i++) {
					float v = a[i*2]*a[i*2]+a[i*2+1]*a[i*2+1];
					b[i] = Math.max(b[i], v);
				}
			}
			for (int i = 0; i < cutoff; i++) {
				float v = b[i];
				int c = Renderer.colInferno[Lut.match256f(lut, v)];
				dst[(cutoff - i - 1) * cols + colPos] = c;
			}
		}
		return image;
	}
}