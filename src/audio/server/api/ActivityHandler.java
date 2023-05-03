package audio.server.api;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.jetty.server.Request;
import org.json.JSONWriter;
import org.jtransforms.fft.FloatFFT_1D;
import org.tinylog.Logger;

import audio.AudioCache;
import audio.Broker;
import audio.GeneralSample;
import audio.processing.SampleProcessor;
import jakarta.servlet.http.HttpServletResponse;
import util.Timer;
import util.Web;

public class ActivityHandler {
	private final Broker broker;
	private final AudioCache audioCache;

	public ActivityHandler(Broker broker) {
		this.broker = broker;		
		this.audioCache = broker.audioCache();
	}

	public void handle(GeneralSample sample, Request request, HttpServletResponse response) throws IOException {
		SampleProcessor sampleProcessor = new SampleProcessor(sample, audioCache);
		int startSample = 0;
		int endSample = sampleProcessor.getFrameLength() - 1;	
		sampleProcessor.loadData(0, startSample, endSample);
		Logger.info(Timer.stop("load audio"));
		short[] data = sampleProcessor.data;
		int n = 1024;
		int nHalf = n / 2;
		int step = n / 2;
		int cols = ((sampleProcessor.dataLength - n) / step) + 1;

		float[] weight = SampleProcessor.getGaussianWeights(n, step);
		FloatFFT_1D fft = new FloatFFT_1D(n);
		float[] a = new float[n];
		float[] b = new float[nHalf];
		float[] c = new float[nHalf];
		float[] r = new float[nHalf];
		for (int i = 0; i < nHalf; i++) {
			c[i] = Float.MAX_VALUE;
		}
		int mf = 16;
		Timer.start("fft");
		for (int pos = 0; pos < cols; pos++) {
			Logger.info("pos " + pos);
			for (int i = 0; i < n; i++) {
				a[i] = data[pos*step + i] * weight[i];
			}
			fft.realForward(a);
			for (int i = 0; i < nHalf; i++) {
				float v = (float) Math.sqrt(a[i*2]*a[i*2]+a[i*2+1]*a[i*2+1]);
				b[i] = v > b[i] ? v : b[i];
				r[i] = v > r[i] ? v : r[i];
			}
			if(pos % mf == mf - 1) {
				Logger.info("split " + pos/mf);
				for (int i = 0; i < nHalf; i++) {
					c[i] = b[i] < c[i] ? b[i] : c[i];
					b[i] = 0f;
				}
			}
		}
		for (int i = 0; i < nHalf; i++) {
			r[i] = r[i] / c[i];
		}
		Timer.stop("fft");

		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("c");
		json.value(Arrays.toString(c));
		json.key("result");
		json.value(Arrays.toString(r));
		json.key("time");
		json.value(Timer.get("fft"));
		json.endObject();
	}
}
