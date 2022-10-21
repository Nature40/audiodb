package audio.worklist;

public class WorklistEntry {
	
	public final int index;
	public final String sample;
	public final float start;
	public final float end;
	public final String title;
	
	public WorklistEntry(int index, String sample, float start, float end, String title) {
		this.index = index;
		this.sample = sample;
		this.start = start;
		this.end = end;
		this.title = title; 
	}
}
