package audio.worklist;

public class WorklistEntry {
	
	public final int index;
	public final int sample;
	public final float start;
	public final float end;
	public final String title;
	
	public WorklistEntry(int index, int sample, float start, float end, String title) {
		this.index = index;
		this.sample = sample;
		this.start = start;
		this.end = end;
		this.title = title; 
	}

	@Override
	public String toString() {
		return "WorklistEntry [index=" + index + ", sample=" + sample + ", start=" + start + ", end=" + end + ", title="
				+ title + "]";
	}
}
