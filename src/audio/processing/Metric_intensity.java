package audio.processing;

public class Metric_intensity extends Metric {
	public final static Metric_intensity INSATNCE = new Metric_intensity();
	
	public double calc(SampleProcessor sampleProcessor, int start, int end, int colStart, int colEnd) {		
		short[] data = sampleProcessor.data;		
		double sum = 0;
		for(int i = start; i <= end; i++) {
			short v = data[i];
			sum += v < 0 ? (v == Short.MIN_VALUE ? Short.MAX_VALUE : -v ) : v;
		}		
		return ((sum / (end - start +1)) / Short.MAX_VALUE) * 100;
	}

}
