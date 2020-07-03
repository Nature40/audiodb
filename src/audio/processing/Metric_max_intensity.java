package audio.processing;

public class Metric_max_intensity extends Metric {
	public final static Metric_max_intensity INSATNCE = new Metric_max_intensity();
	
	public double calc(SampleProcessor sampleProcessor, int start, int end, int colStart, int colEnd) {		
		short[] data = sampleProcessor.data;		
		double intensity = 0;
		for(int i = start; i <= end; i++) {
			short v = data[i];
			int q = v < 0 ? (v == Short.MIN_VALUE ? Short.MAX_VALUE : -v ) : v;
			if(intensity < q) {
				intensity = q;
			}
		}		
		return (intensity / Short.MAX_VALUE) * 100;
	}

}
