package audio.worklist;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import util.collections.vec.Vec;

public class Worklist {
	
	private Vec<WorklistEntry> vec = new Vec<WorklistEntry>();
	public final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

	/*public void forEachEntry(Consumer<WorklistEntry> consumer) {		
		lock.readLock().lock();
		try {
			vec.forEach(consumer);
		} finally {
			lock.readLock().unlock();
		}
	}*/
	
	public void getByIndex(int index) {
		lock.readLock().lock();
		try {
			vec.get(index);
		} finally {
			lock.readLock().unlock();
		}
	}
	
	public int size() {
		lock.readLock().lock();
		try {
			return vec.size();
		} finally {
			lock.readLock().unlock();
		}
	}

	public WorklistEntry find(int firstIndex, Predicate<WorklistEntry> predicate) {
		lock.readLock().lock();
		try {
			return vec.find(firstIndex, predicate);
		} finally {
			lock.readLock().unlock();
		}
	}

	public WorklistEntry findLast(int lastIndex, Predicate<WorklistEntry> predicate) {
		lock.readLock().lock();
		try {
			return vec.findLast(lastIndex, predicate);
		} finally {
			lock.readLock().unlock();
		}
	}

	public void replace(Vec<WorklistEntry> vec) {
		lock.writeLock().lock();
		try {
			this.vec.clear();
			this.vec.addAll(vec);
		} finally {
			lock.writeLock().unlock();
		}		
	}
}
