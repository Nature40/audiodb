package audio.server.api;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.jtransforms.fft.FloatFFT_1D;
import org.tinylog.Logger;

import audio.Broker;
import audio.GeneralSample;
import audio.processing.SampleProcessor;
import audio.server.Renderer;
import jakarta.servlet.http.HttpServletResponse;
import util.Timer;
import util.Web;
import util.image.ImageRGBA;
import util.image.Lut;

public class SpectrumHandler {
	

	private final Broker broker;

	public SpectrumHandler(Broker broker) {
		this.broker = broker;
	}

	public void handle(GeneralSample sample, Request request, HttpServletResponse response) throws IOException {
		int window = 1024;
		//int n = 2048;
		//int n = 512;

		window = Web.getInt(request, "window", window);

		int cutoff_lower_max = window/2 - 1;
		int cutoff_lower = Web.getInt(request, "cutoff_lower", 0);
		if(cutoff_lower < 0) {
			Logger.warn("cutoff_lower out of bounds: " + cutoff_lower + "  set to " + 0);
			cutoff_lower = 0;
		}
		if(cutoff_lower > cutoff_lower_max) {
			Logger.warn("cutoff_lower out of bounds: " + cutoff_lower + "  set to " + cutoff_lower_max);
			cutoff_lower = cutoff_lower_max;
		}
		
		int cutoff_upper_max = window / 2;
		int cutoff_upper = Web.getInt(request, "cutoff", 320);
		if(cutoff_upper < 1) {
			Logger.warn("cutoff out of bounds: " + cutoff_upper + "  set to " + 1);
			cutoff_upper = 1;
		}
		if(cutoff_upper > cutoff_upper_max) {
			Logger.warn("cutoff out of bounds: " + cutoff_upper + "  set to " + cutoff_upper_max);
			cutoff_upper = cutoff_upper_max;
		}
		float threshold = (float) Web.getDouble(request, "threshold", 12);

		int shrink_factor = Web.getInt(request, "shrink_factor", 1);
		int width = Web.getInt(request, "width", 0);
		int max_width = Web.getInt(request, "max_width", 0);

		int startSample = Web.getInt(request, "start_sample", -1);
		int endSample = Web.getInt(request, "end_sample", -1);	

		//Logger.info("spectrum " + startSample + " to " + endSample);

		SampleProcessor sampleProcessor = new SampleProcessor(sample);
		if(startSample < 0) {
			double startSecond = Web.getDouble(request, "start", Double.NaN);
			startSample = Double.isFinite(startSecond) ? sampleProcessor.secondsToPos(startSecond) : 0;
		}
		if(endSample < 0) {
			double endSecond = Web.getDouble(request, "end", Double.NaN);
			endSample = Double.isFinite(endSecond) ? sampleProcessor.secondsToPos(endSecond) : sampleProcessor.getFrameLength() - 1;
		}
		
		int step = 256;
		//int step = 512;
		//int step = 1024;
		step = Web.getInt(request, "step", step);
		
		if(endSample - startSample + 1 < step * (shrink_factor - 1) + window) {
			throw new RuntimeException("endSample - startSample + 1 < step * (shrink_factor - 1) + window   " + (endSample - startSample + 1) + "   " + (step * (shrink_factor - 1) + window));
		}
		
		//Logger.info("start " + startSample + "  end " + endSample);
		Timer.start("load audio");
		sampleProcessor.loadData(0, startSample, endSample);
		Logger.info(Timer.stop("load audio"));
		short[] fullShorts = sampleProcessor.data;
		/*Logger.info(fullShorts.length + "  " + window);
		for (int i = 0; i < fullShorts.length; i++) {
			fullShorts[i] = i%2 == 0 ? Short.MAX_VALUE : -Short.MAX_VALUE;
		}*/		

		float intensity_max = 23f;
		intensity_max = Web.getFloat(request, "intensity_max", intensity_max);
		
		if(sampleProcessor.dataLength < step * (shrink_factor - 1) + window) {
			throw new RuntimeException("sampleProcessor.dataLength < step * (shrink_factor - 1) + window " + sampleProcessor.dataLength + "   " + (step * (shrink_factor - 1) + window));
		}

		//int cols = ((sampleProcessor.dataLength - window) / (step * shrink_factor)) + 1;
		//int cols = ((sampleProcessor.dataLength - (window * shrink_factor)) / (step * shrink_factor)) + 1;
		int cols = ((sampleProcessor.dataLength - step * (shrink_factor - 1) - window) / (step * shrink_factor)) + 1;

		ImageRGBA image;
		int renderWidth = 0;
		boolean isMaxWidth = false; 
		if(width > 0) {
			renderWidth = width;
		} else if(max_width > 0 && cols > max_width) {
			renderWidth = max_width;
			isMaxWidth = true;
		}

		Timer.start("render image");
		if(shrink_factor > 1) {
			Logger.info("render3Shrink");
			//image = render3Shrink(fullShorts, window, step, cols, cutoff_upper, threshold, intensity_max, shrink_factor);
			image = render3Shrink(fullShorts, window, step, cols, cutoff_lower, cutoff_upper, threshold, intensity_max, shrink_factor);
		} else if(renderWidth <= 0) {
			//Logger.info("render3");
			//image = render3(fullShorts, window, step, cols, cutoff_upper, threshold, intensity_max);
			image = render3(fullShorts, window, step, cols, cutoff_lower, cutoff_upper, threshold, intensity_max);
			//image = render3denoise(fullShorts, window, step, cols, cutoff, threshold, intensity_max);
		} else if(isMaxWidth){
			step = window;
			cols = ((sampleProcessor.dataLength - window) / step) + 1;
			if(cols <= renderWidth) {
				Logger.info("render3");
				//image = render3(fullShorts, window, step, cols, cutoff_upper, threshold, intensity_max);
				image = render3(fullShorts, window, step, cols, cutoff_lower, cutoff_upper, threshold, intensity_max);
			} else {
				Logger.info("render3width");
				//image = render3width(fullShorts, window, step, cols, cutoff_upper, threshold, intensity_max, renderWidth);
				image = render3width(fullShorts, window, step, cols, cutoff_lower, cutoff_upper, threshold, intensity_max, renderWidth);
			}
		} else {
			Logger.info("render3width");
			//image = render3width(fullShorts, window, step, cols, cutoff_upper, threshold, intensity_max, renderWidth);
			image = render3width(fullShorts, window, step, cols, cutoff_lower, cutoff_upper, threshold, intensity_max, renderWidth);
		}
		Logger.info(Timer.stop("render image"));

		Timer.start("send image");
		image.writePngCompressed(response.getOutputStream());
		Logger.info(Timer.stop("send image"));
	}

	private ImageRGBA render2(short[] fullShorts, int n, int step, int cols, int cutoff_upper, float threshold) {
		Logger.info("render2 step " + step + "  cols " + cols);
		FloatFFT_1D fft = new FloatFFT_1D(n);
		float[] weight = SampleProcessor.getGaussianWeights(n, step);
		float[][] transformed = new float[cols][n];
		float minv = Float.MAX_VALUE;
		float maxv = -Float.MAX_VALUE;
		for (int pos = 0; pos < cols; pos++) {
			float[] a = transformed[pos];
			for (int i = 0; i < n; i++) {
				a[i] = fullShorts[pos*step + i] * weight[i];
			}
			fft.realForward(a);
			for (int i = 0; i < cutoff_upper; i++) {
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

		Logger.info(minv + "  " + maxv);

		float[] lut = Lut.getGammaLUT256f(minv, maxv, 1.2);			
		ImageRGBA image = new ImageRGBA(cols, cutoff_upper);
		int[] dst = image.getRawArray();
		for (int pos = 0; pos < cols; pos++) {
			float[] a = transformed[pos];
			for (int i = 0; i < cutoff_upper; i++) {
				float v = a[i];
				int c = Renderer.colInferno[Lut.match256f(lut, v)];
				dst[(cutoff_upper - i - 1) * cols + pos] = c;
			}
		}

		return image;
	}

	private ImageRGBA render3(short[] fullShorts, int n, int step, int cols, int cutoff_upper, float threshold, float maxv) {
		//Logger.info("render3 step " + step + "  cols " + cols);
		FloatFFT_1D fft = new FloatFFT_1D(n);
		float[] weight = SampleProcessor.getGaussianWeights(n, step);

		/*float energy = 0;
		for (int i = 0; i < n; i++) {
			float v = weight[i] * Short.MAX_VALUE;
			energy += v*v;
		}
		float energy_per_sample = energy / n;
		Logger.info("energy " +  energy + "   " + Math.log(energy));
		Logger.info("energy sample " +  energy_per_sample + "   " + Math.log(energy_per_sample));*/

		float[] lut = Lut.getLogLUT256fLogMinMax(threshold, maxv);
		ImageRGBA image = new ImageRGBA(cols, cutoff_upper);
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
			Logger.info("vmax " + vmax + "   " + Math.log(vmax) + "vmax " + vmax/n + "   " + Math.log(vmax/n));*/
			for (int i = 0; i < cutoff_upper; i++) {
				float v = a[i*2]*a[i*2]+a[i*2+1]*a[i*2+1];
				int c = Renderer.colInferno[Lut.match256f(lut, v)];
				dst[(cutoff_upper - i - 1) * cols + pos] = c;
			}
		}

		return image;
	}
	
	private ImageRGBA render3(short[] fullShorts, int n, int step, int cols, int cutoff_lower, int cutoff_upper, float threshold, float maxv) {
		int cutoff_range = cutoff_upper - cutoff_lower;
		//Logger.info("render3 step " + step + "  cols " + cols);
		FloatFFT_1D fft = new FloatFFT_1D(n);
		float[] weight = SampleProcessor.getGaussianWeights(n, step);

		/*float energy = 0;
		for (int i = 0; i < n; i++) {
			float v = weight[i] * Short.MAX_VALUE;
			energy += v*v;
		}
		float energy_per_sample = energy / n;
		Logger.info("energy " +  energy + "   " + Math.log(energy));
		Logger.info("energy sample " +  energy_per_sample + "   " + Math.log(energy_per_sample));*/

		float[] lut = Lut.getLogLUT256fLogMinMax(threshold, maxv);
		ImageRGBA image = new ImageRGBA(cols, cutoff_range);
		int[] dst = image.getRawArray();
		float[] a = new float[n];
		for (int pos = 0; pos < cols; pos++) {
			/*int min = Short.MAX_VALUE;
			int max = Short.MIN_VALUE;*/
			for (int i = 0; i < n; i++) {
				a[i] = fullShorts[pos*step + i] * weight[i];
				/*if(fullShorts[pos*step + i] < min) {
					min = fullShorts[pos*step + i];
				}
				if(fullShorts[pos*step + i] > max) {
					max = fullShorts[pos*step + i];
				}*/
			}
			fft.realForward(a);
			/*float vmax = 0;
			for (int i = 0; i < n/2; i++) {
				float v = a[i*2]*a[i*2]+a[i*2+1]*a[i*2+1];
				if(vmax < v) {
					vmax = v;
				}
			}
			Logger.info("vmax " + vmax + "   " + Math.log(vmax) + "vmax " + vmax/n + "   " + Math.log(vmax/n));*/
			float vmax = 0;
			for (int i = cutoff_lower; i < cutoff_upper; i++) {
				float v = a[i*2]*a[i*2]+a[i*2+1]*a[i*2+1];
				int c = Renderer.colInferno[Lut.match256f(lut, v)];
				dst[(cutoff_upper - i - 1) * cols + pos] = c;
				vmax += v;
			}
			
//			int ivmax = (int) (vmax / 100000000f);
//			int max = ivmax >= cutoff_range ? cutoff_range - 1 : ivmax;
//			///Logger.info(vmax  + "  " + ivmax + "  " + max);
//			for (int i = 0; i <= max; i++) {		
//				int p = dst[i * cols + pos];
//				int r = (((p >> 16) & 0xff) >> 1) + (((p >> 16) & 0xff) >> 2) + (((p >> 16) & 0xff) >> 3) + 34;
//				int g = (((p >> 8) & 0xff) >> 1) + (((p >> 8) & 0xff) >> 2) + (((p >> 8) & 0xff) >> 3) + 34;
//				int b = ((p & 0xff) >> 1) + ((p & 0xff) >> 2) + ((p & 0xff) >> 3) + 34;
//				dst[i * cols + pos] = 0xff000000 | (r<<16) | (g<<8) | b;
//			}
//			
//			/*min -= Short.MIN_VALUE;
//			max -= Short.MIN_VALUE;
//			int range = ((int) Short.MAX_VALUE) - ((int) Short.MIN_VALUE);
//			int spectrum_range = cutoff_upper - cutoff_lower;
//			min = (min * spectrum_range) / range;
//			max = (max * spectrum_range) / range;
//			//Logger.info("min " + min + " max " + max + "  range " + range);
//			for (int i = min; i < max; i++) {		
//				int p = dst[i * cols + pos];
//				int r = (((p >> 16) & 0xff) >> 1) + (((p >> 16) & 0xff) >> 2) + (((p >> 16) & 0xff) >> 3) + 34;
//				int g = (((p >> 8) & 0xff) >> 1) + (((p >> 8) & 0xff) >> 2) + (((p >> 8) & 0xff) >> 3) + 34;
//				int b = ((p & 0xff) >> 1) + ((p & 0xff) >> 2) + ((p & 0xff) >> 3) + 34;
//				dst[i * cols + pos] = 0xff000000 | (r<<16) | (g<<8) | b;
//			}*/
		}

		return image;
	}

	private ImageRGBA render3denoise(short[] fullShorts, int n, int step, int cols, int cutoff_upper, float threshold, float maxv) {
		//Logger.info("render3 step " + step + "  cols " + cols);
		FloatFFT_1D fft = new FloatFFT_1D(n);
		float[] weight = SampleProcessor.getGaussianWeights(n, step);

		float[] lut = Lut.getLogLUT256fLogMinMax(threshold, maxv);
		ImageRGBA image = new ImageRGBA(cols, cutoff_upper);
		int[] dst = image.getRawArray();
		float[] a = new float[n];
		float[][] bb = new float[cols][cutoff_upper];
		float[] sum = new float[cutoff_upper];
		for (int pos = 0; pos < cols; pos++) {
			for (int i = 0; i < n; i++) {
				a[i] = fullShorts[pos*step + i] * weight[i];
			}
			fft.realForward(a);
			float[] b = bb[pos];
			for (int i = 0; i < cutoff_upper; i++) {
				float v = a[i*2]*a[i*2]+a[i*2+1]*a[i*2+1];
				b[i] = v;
				sum[i] += v;
			}
		}
		for (int i = 0; i < cutoff_upper; i++) {
			sum[i] = (sum[i] / cutoff_upper) * 1.1f;
		}
		for (int pos = 0; pos < cols; pos++) {
			float[] b = bb[pos];
			for (int i = 0; i < cutoff_upper; i++) {
				float v = b[i];
				if(sum[i] <= v) {
					int c = Renderer.colInferno[Lut.match256f(lut, v)];
					dst[(cutoff_upper - i - 1) * cols + pos] = c;
				}
			}
		}

		return image;
	}

	private ImageRGBA render3width(short[] fullShorts, int n, int step, int cols, int cutoff_upper, float threshold, float maxv, int width) {
		Logger.info("render3width step " + step + "  cols " + cols);
		FloatFFT_1D fft = new FloatFFT_1D(n);
		float[] weight = SampleProcessor.getGaussianWeights(n, step);

		float[][] target = new float[width][cutoff_upper];

		float[] lut = Lut.getLogLUT256fLogMinMax(threshold, maxv);
		ImageRGBA image = new ImageRGBA(width, cutoff_upper);
		int[] dst = image.getRawArray();
		float[] a = new float[n];
		for (int pos = 0; pos < cols; pos++) {			
			for (int i = 0; i < n; i++) {
				a[i] = fullShorts[pos*step + i] * weight[i];
			}
			fft.realForward(a);
			float[] t = target[(int) Math.floor(((pos + 0.5d) * width) / cols)];
			for (int i = 0; i < cutoff_upper; i++) {
				float v = a[i*2]*a[i*2]+a[i*2+1]*a[i*2+1];
				t[i] = Math.max(t[i], v);
				/*if(t[i] < v) { // slower
					t[i] = v;
				}*/
			}
		}

		for (int pos = 0; pos < width; pos++) {			
			float[] t = target[pos];
			for (int i = 0; i < cutoff_upper; i++) {
				float v = t[i];
				int c = Renderer.colInferno[Lut.match256f(lut, v)];
				dst[(cutoff_upper - i - 1) * width + pos] = c;
			}
		}

		return image;
	}
	
	private ImageRGBA render3width(short[] fullShorts, int n, int step, int cols, int cutoff_lower, int cutoff_upper, float threshold, float maxv, int width) {
		int cutoff_range = cutoff_upper - cutoff_lower;
		Logger.info("render3width step " + step + "  cols " + cols);
		FloatFFT_1D fft = new FloatFFT_1D(n);
		float[] weight = SampleProcessor.getGaussianWeights(n, step);

		float[][] target = new float[width][cutoff_range];

		float[] lut = Lut.getLogLUT256fLogMinMax(threshold, maxv);
		ImageRGBA image = new ImageRGBA(width, cutoff_range);
		int[] dst = image.getRawArray();
		float[] a = new float[n];
		for (int pos = 0; pos < cols; pos++) {			
			for (int i = 0; i < n; i++) {
				a[i] = fullShorts[pos*step + i] * weight[i];
			}
			fft.realForward(a);
			float[] t = target[(int) Math.floor(((pos + 0.5d) * width) / cols)];
			for (int i = cutoff_lower; i < cutoff_upper; i++) {
				float v = a[i*2]*a[i*2]+a[i*2+1]*a[i*2+1];
				int bIndex = i - cutoff_lower;
				t[bIndex] = Math.max(t[bIndex], v);
				/*if(t[i] < v) { // slower
					t[i] = v;
				}*/
			}
		}

		for (int pos = 0; pos < width; pos++) {			
			float[] t = target[pos];
			for (int i = 0; i < cutoff_range; i++) {
				float v = t[i];
				int c = Renderer.colInferno[Lut.match256f(lut, v)];
				dst[(cutoff_range - i - 1) * width + pos] = c;
			}
		}

		return image;
	}

	private ImageRGBA render3Shrink(short[] fullShorts, int n, int step, int cols, int cutoff_upper, float threshold, float maxv, int shrinkFactor) {
		//Logger.info("render3 step " + step + "  cols " + cols);
		FloatFFT_1D fft = new FloatFFT_1D(n);
		float[] weight = SampleProcessor.getGaussianWeights(n, step);

		float[] lut = Lut.getLogLUT256fLogMinMax(threshold, maxv);
		ImageRGBA image = new ImageRGBA(cols, cutoff_upper);
		int[] dst = image.getRawArray();
		float[] a = new float[n];
		float[] b = new float[cutoff_upper];
		for (int colPos = 0; colPos < cols; colPos++) {
			for (int i = 0; i < cutoff_upper; i++) {
				b[i] = 0f;
			}
			for(int shrinkPos = 0; shrinkPos < shrinkFactor; shrinkPos++) {
				int fftPos = colPos * step * shrinkFactor + shrinkPos * step;
				for (int i = 0; i < n; i++) {
					a[i] = fullShorts[fftPos + i] * weight[i];
				}
				fft.realForward(a);
				for (int i = 0; i < cutoff_upper; i++) {
					float v = a[i*2]*a[i*2]+a[i*2+1]*a[i*2+1];
					b[i] = Math.max(b[i], v);
				}
			}
			for (int i = 0; i < cutoff_upper; i++) {
				float v = b[i];
				int c = Renderer.colInferno[Lut.match256f(lut, v)];
				dst[(cutoff_upper - i - 1) * cols + colPos] = c;
			}
		}
		return image;
	}
	
	private ImageRGBA render3Shrink(short[] fullShorts, int n, int step, int cols, int cutoff_lower, int cutoff_upper, float threshold, float maxv, int shrinkFactor) {
		int cutoff_range = cutoff_upper - cutoff_lower;
		//Logger.info("render3 step " + step + "  cols " + cols);
		FloatFFT_1D fft = new FloatFFT_1D(n);
		float[] weight = SampleProcessor.getGaussianWeights(n, step);

		float[] lut = Lut.getLogLUT256fLogMinMax(threshold, maxv);
		ImageRGBA image = new ImageRGBA(cols, cutoff_range);
		int[] dst = image.getRawArray();
		float[] a = new float[n];
		float[] b = new float[cutoff_range];
		for (int colPos = 0; colPos < cols; colPos++) {
			for (int i = 0; i < cutoff_range; i++) {
				b[i] = 0f;
			}
			for(int shrinkPos = 0; shrinkPos < shrinkFactor; shrinkPos++) {
				int fftPos = colPos * step * shrinkFactor + shrinkPos * step;
				for (int i = 0; i < n; i++) {
					a[i] = fullShorts[fftPos + i] * weight[i];
				}
				fft.realForward(a);
				for (int i = cutoff_lower; i < cutoff_upper; i++) {
					float v = a[i*2]*a[i*2]+a[i*2+1]*a[i*2+1];
					int bIndex = i - cutoff_lower;
					b[bIndex] = Math.max(b[bIndex], v);
				}
			}
			for (int i = 0; i < cutoff_range; i++) {
				float v = b[i];
				int c = Renderer.colInferno[Lut.match256f(lut, v)];
				dst[(cutoff_range - i - 1) * cols + colPos] = c;
			}
		}
		return image;
	}
}