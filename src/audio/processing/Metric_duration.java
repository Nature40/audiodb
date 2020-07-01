package audio.processing;

public class Metric_duration extends Metric {
	public final static Metric_duration INSATNCE = new Metric_duration();
	
	public double calc(SampleProcessor sampleProcessor, int start, int end) {
		return sampleProcessor.posToSeconds(end - start + 1);
	}

}
