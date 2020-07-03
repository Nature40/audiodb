package audio.processing;

public class Metric_biophonic_intensity extends Metric {
	public final static Metric_biophonic_intensity INSATNCE = new Metric_biophonic_intensity();
	
	public double calc(SampleProcessor sampleProcessor, int start, int end, int colStart, int colEnd) {
		int bMin = sampleProcessor.frequencyToIndex(2000);
		int bMax = sampleProcessor.frequencyToIndex(11000);
		float[][] data = sampleProcessor.fq;
		double sum = 0;
		for(int pos = colStart; pos < colEnd; pos++) {
			float[] col = data[pos];
			for(int i = bMin; i < bMax; i++) {
				sum += col[i];
			}
		}

		double timeInterval = (colEnd - colStart + 1);
		double fqInterval = (bMax - bMin +1);
		return sum / timeInterval / fqInterval;
	}

}
