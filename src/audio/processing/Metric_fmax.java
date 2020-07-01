package audio.processing;

public class Metric_fmax extends Metric {
	public final static Metric_fmax INSATNCE = new Metric_fmax();
	
	public double calc(SampleProcessor sampleProcessor, int start, int end) {		
		float[][] data = sampleProcessor.fq;
		int cutoff = sampleProcessor.n / 2;
		double[] sum = new double[cutoff];
		for(int pos = 0; pos < data.length; pos++) {
			float[] col = data[pos];
			for(int i = 0; i < cutoff; i++) {
				sum[i] += col[i];
			}
		}
		int fmax = -1;
		double fmaxV = 0;
		for(int i = 0; i < cutoff; i++) {
			double v = sum[i];
			if(v > fmaxV) {
				fmaxV = v;
				fmax = i;
			}
		}		
		return (((double)fmax) * sampleProcessor.sampleRate) / sampleProcessor.n;
	}

}
