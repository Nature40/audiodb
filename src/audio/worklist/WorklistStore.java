package audio.worklist;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import audio.Broker;
import util.collections.vec.Vec;

public class WorklistStore {

	private ConcurrentSkipListMap<String, Worklist> worklistMap = new ConcurrentSkipListMap<String, Worklist>();
	public final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


	public WorklistStore(Broker broker) {

		Worklist worklist = new Worklist();

		Vec<WorklistEntry> vec = new Vec<WorklistEntry>();

		broker.sampleManager().forEach(sample -> {
			WorklistEntry e = new WorklistEntry(vec.size(), sample.id, 0, 10);
			vec.add(e);
		});

		worklist.replace(vec);
		worklistMap.put("list", worklist);
	}

	public void forEachWorklistId(Consumer<String> action) {
		worklistMap.keySet().forEach(action);
	}

	public Worklist getWorklistByd(String worklistId) {
		return worklistMap.get(worklistId);
	}
}
