package audio.processing;

public class Metric_max_intensity_frequency extends Metric {
	public final static Metric_max_intensity_frequency INSATNCE = new Metric_max_intensity_frequency();
	
	public double calc(SampleProcessor sampleProcessor, int start, int end, int colStart, int colEnd) {		
		float[][] data = sampleProcessor.fq;
		int cutoff = SampleProcessor.n2;
		double[] sum = new double[cutoff];
		for(int pos = colStart; pos < colEnd; pos++) {
			float[] col = data[pos];
			for(int i = 0; i < cutoff; i++) {				
				float v = col[i];
				sum[i] += v < 0 ? (v == Short.MIN_VALUE ? Short.MAX_VALUE : -v ) : v;
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
		return sampleProcessor.indexToFrequency(fmax);
	}

}
