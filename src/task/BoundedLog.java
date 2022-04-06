package task;

import java.util.function.Consumer;

public class BoundedLog {

	private final String[] a;
	private final int capacity;
	private int start = 0;
	private int end = 0;

	public BoundedLog(int capacity) {
		this.capacity = capacity;
		a = new String[capacity];		
		start = 0;
		end = 0;
	}

	public synchronized void add(String s) {		
		a[end] = s;
		end = (end + 1) % capacity;
		if(start == end) {
			start = (start + 1) % capacity;
		}
	}

	public synchronized void foreach(Consumer<String> consumer) {
		if(start <= end) {
			for(int i = start; i < end; i++) {
				consumer.accept(a[i]);
			}
		} else {
			for(int i = start; i < capacity; i++) {
				consumer.accept(a[i]);
			}
			for(int i = 0; i < end; i++) {
				consumer.accept(a[i]);
			}
		}
	}

}
