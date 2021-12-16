package audio.processing;


import org.tinylog.Logger;

public abstract class Metric {
	
	
	public final String name;
	
	public Metric() {
		String s = this.getClass().getSimpleName();
		this.name = s.startsWith("Metric_") ? s.substring(7) : s; 
	}
	
	public final double apply(SampleProcessor sampleProcessor, int start, int end, int colStart, int colEnd) {
		try {
			return calc(sampleProcessor, start, end, colStart, colEnd);
		} catch(RuntimeException e) {
			String s = "Error at metric " + name + "  " + e.getClass().getSimpleName() + ": " + e.getMessage();
			StackTraceElement[] st = e.getStackTrace();
			if(st.length > 0) {
				s += "  " + st[0];
			}
			Logger.warn(s);
			throw e;
		}
	}
	
	public abstract double calc(SampleProcessor sampleProcessor, int start, int end, int colStart, int colEnd);

}
