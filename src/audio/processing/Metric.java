package audio.processing;

public abstract class Metric {
	
	public final String name;
	
	public Metric() {
		String s = this.getClass().getSimpleName();
		this.name = s.startsWith("Metric_") ? s.substring(7) : s; 
	}
	
	public abstract double calc(SampleProcessor sampleProcessor, int start, int end);

}
