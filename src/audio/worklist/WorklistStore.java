package audio.worklist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.tinylog.Logger;

import audio.Broker;
import audio.SampleStorage;
import util.AbstractTable.ColumnReaderDouble;
import util.AbstractTable.ColumnReaderString;
import util.StreamTable;
import util.collections.vec.Vec;

public class WorklistStore {

	private final Broker broker;
	private ConcurrentSkipListMap<String, Worklist> worklistMap = new ConcurrentSkipListMap<String, Worklist>();

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

	/*private void addAllSamplesWorklist() {
		Vec<WorklistEntry> vec = new Vec<WorklistEntry>();
		broker.sampleStorage().forEachOrderedSample(sample -> {
			WorklistEntry e = new WorklistEntry(vec.size(), sample.id, 0, (float) (sample.samples() / sample.sampleRate()), "full sample");
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
	}*/

	public void refresh() {		
		//addAllSamplesWorklist();
		//addAllGeneratorLabelsWorklist();
		//addNamedGeneratorLabelsWorklist();	
		loadWorklistsFromFiles();
	}

	private void loadWorklistsFromFiles() {
		try {
			Path worklist_path = broker.config().audioConfig.worklist_path;
			if(worklist_path != null) {
				loadWorklistsFromFiles(worklist_path, "");
			}
		} catch(Exception e) {
			Logger.warn("Error loading worklists " + e);
		}
	}

	private void loadWorklistsFromFiles(Path path, String prefix) throws IOException {
		for(Path sub:Files.newDirectoryStream(path)) {
			if(sub.toFile().isDirectory()) {
				String newPrefix = (prefix.isEmpty() ? "" : (prefix + '.'))  + sub.getFileName();
				loadWorklistsFromFiles(sub, newPrefix);
			} else if(sub.toFile().isFile()) {
				if(sub.getFileName().toString().endsWith(".csv")) {
					String id =  (prefix.isEmpty() ? "" : (prefix + '.')) + sub.getFileName().toString().replaceAll(".csv", "");
					try {
						loadWorklistFromFile(sub, id);
					} catch(Exception e) {
						Logger.warn("Error loading " + sub + " :  " + e.getMessage());
					}
				}
			} else {
				Logger.warn("unknown entity: " + sub);
			}
		}	
	}

	private void loadWorklistFromFile(Path path, String id) {
		Logger.info("load worklist " + id + "from " + path);
		StreamTable table = null;
		try {
			Vec<WorklistEntry> vec = new Vec<WorklistEntry>();
			table = StreamTable.openCSV(path, ',');
			ColumnReaderString colFile = table.createColumnReader("file");
			if(colFile == null) {
				throw new RuntimeException("For Worklist " + path + "  column 'file' missing in CSV");
			}
			ColumnReaderDouble colStart = table.createColumnReaderDouble("start", Double.NaN);
			ColumnReaderDouble colEnd = table.createColumnReaderDouble("end", Double.NaN);
			ColumnReaderString colLabel = table.createColumnReader("label", null);
			for(String[] row = table.readNext(); row != null; row = table.readNext()) {
				String file = colFile.get(row);
				file = file.replace('\\', '/');
				Logger.info(file);
				int filenameStart = file.lastIndexOf('/');
				String fileName = filenameStart < 0 ? file : file.substring(filenameStart + 1);
				String folderName = filenameStart < 0 ? SampleStorage.ROOT_FOLDER_MARKER : file.substring(0, filenameStart);
				int sampleId = broker.sampleStorage().tlSampleStorageConnector.get().getSampleIdByFile(folderName, fileName);
				if(sampleId < 0) {
					Logger.info("sample not found: " + file + "   " + folderName + "   " + fileName);
				} else {
					Logger.info(file + " -> " + sampleId);
					float start = (float) colStart.get(row, false);
					float end = (float) colEnd.get(row, false);
					String label = colLabel.get(row);
					//if(!file.isBlank() && Double.isFinite(start) && Double.isFinite(end)) {
						vec.add(index -> {
							WorklistEntry worklistEntry = new WorklistEntry(index, sampleId, start, end, label);
							Logger.info(worklistEntry);
							return worklistEntry;
						});		
					//}
				}
			}
			Worklist worklist = new Worklist();
			worklist.replace(vec);
			worklistMap.put(id, worklist);
		} catch (IOException e) {
			Logger.warn(e);
		} finally {
			if(table != null) {
				table.close();
			}
		}
	}
}
