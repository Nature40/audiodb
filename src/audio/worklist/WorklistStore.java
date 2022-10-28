package audio.worklist;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import audio.Broker;
import audio.LabelStoreConnector;
import util.MapVec;
import util.collections.vec.Vec;

public class WorklistStore {

	private final Broker broker;
	private ConcurrentSkipListMap<String, Worklist> worklistMap = new ConcurrentSkipListMap<String, Worklist>();
	public final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public WorklistStore(Broker broker) {
		this.broker = broker;
	}

	public void forEachWorklistId(Consumer<String> action) {
		worklistMap.keySet().forEach(action);
	}
	
	public void forEachWorklist(BiConsumer<String, Worklist> action) {
		worklistMap.forEach(action);
	}

	public Worklist getWorklistByd(String worklistId) {
		return worklistMap.get(worklistId);
	}
	
	private void addAllSamplesWorklist() {
		Vec<WorklistEntry> vec = new Vec<WorklistEntry>();
		broker.sampleManager().forEach(sample -> {
		WorklistEntry e = new WorklistEntry(vec.size(), sample.id, 0, (float) (sample.samples() / sample.sampleRate()), "full sample");
		//WorklistEntry e = new WorklistEntry(vec.size(), sample.id, 0, (float) (1));
		vec.add(e);
		});
		Worklist worklist = new Worklist();
		worklist.replace(vec);
		worklistMap.put("all_samples", worklist);
	}
	
	private void addAllGeneratorLabelsWorklist() {
		Vec<WorklistEntry> vec = new Vec<WorklistEntry>();		
		LabelStoreConnector labelConn = broker.labelStore().tlLabelStoreConnector.get();		
		labelConn.forEachGeneratorLabel((int id, int label, float reliability, int location, int time, float start, float end) -> {
			String sampleName = labelConn.getSampleById(id);
			String labelName = labelConn.getLabelById(label);
			WorklistEntry e = new WorklistEntry(vec.size(), sampleName, start, end, labelName);
			vec.add(e);
		});
		Worklist worklist = new Worklist();
		worklist.replace(vec);
		worklistMap.put("all_generator_labels", worklist);
	}
	
	private void addNamedGeneratorLabelsWorklist() {
		MapVec<WorklistEntry> mapvec = new MapVec<WorklistEntry>();		
		LabelStoreConnector labelConn = broker.labelStore().tlLabelStoreConnector.get();		
		labelConn.forEachGeneratorLabel((int id, int label, float reliability, int location, int time, float start, float end) -> {
			String sampleName = labelConn.getSampleById(id);
			String labelName = labelConn.getLabelById(label);
			mapvec.add(labelName, i -> new WorklistEntry(i, sampleName, start, end, labelName));
		});
		
		mapvec.forEach((labelName, vec) -> {
			Worklist worklist = new Worklist();
			worklist.replace(vec);
			String name = "generator_label." + labelName; 
			worklistMap.put(name, worklist);
		});		
	}

	public void refresh() {		
		addAllSamplesWorklist();
		addAllGeneratorLabelsWorklist();
		addNamedGeneratorLabelsWorklist();		
	}
}
