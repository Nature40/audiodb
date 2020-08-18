package audio.processing;

public class Metric_bin2 extends Metric {
	public final static Metric_bin2 INSATNCE = new Metric_bin2();
	
	public double calc(SampleProcessor sampleProcessor, int start, int end, int colStart, int colEnd) {		
		float[][] data = sampleProcessor.bins;
		double sum = 0;
		for(int pos = colStart; pos < colEnd; pos++) {
			float[] col = data[pos];
				sum += col[2];
		}

		double timeInterval = (colEnd - colStart + 1);
		double fqInterval = 1;
		return sum / timeInterval / fqInterval;
	}

}
