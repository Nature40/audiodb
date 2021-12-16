package audio.labeling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiConsumer;


import org.tinylog.Logger;

import audio.Label;
import audio.Sample;
import audio.Samples;

public class LabelingListManager {
	private 

	ConcurrentSkipListMap<String, LabelingList> labelingListMap = new ConcurrentSkipListMap<>();
	private final Path root;

	public LabelingListManager(Path root) {
		this.root = root;
		refresh();
	}

	public synchronized void refresh() {
		try {
			scan(root, "");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}	
	}

	private void scan(Path path, String prefix) throws IOException {
		for(Path sub:Files.newDirectoryStream(path)) {
			if(sub.toFile().isDirectory()) {
				String newPrefix = prefix + (prefix.isEmpty()? "" : "__")  + sub.getFileName();
				scan(sub, newPrefix);
			} else if(sub.toFile().isFile()) {
				if(sub.getFileName().toString().endsWith(".yaml")) {
					String id =  prefix + (prefix.isEmpty()? "" : "__") + sub.getFileName().toString().replaceAll(".yaml", "");
					try {
					load(sub, id);
					} catch(Exception e) {
						Logger.warn("Error loading " + sub);
					}
				}
			} else {
				Logger.warn("unknown entity: " + sub);
			}
		}	
	}

	private void load(Path path, String id) {		
		LabelingList labelingList = LabelingList.ofFile(path);
		labelingListMap.put(id, labelingList);
	}

	public void forEach(BiConsumer<? super String, ? super LabelingList> action) {
		labelingListMap.forEach(action);
	}

	public LabelingList getThrow(String id) {
		LabelingList labelingList = labelingListMap.get(id);
		if(labelingList == null) {
			throw new RuntimeException("labeling_list not found: " + id);
		}
		return labelingList;
	}

	public void updateLabelingLists(Samples samples) {
		forEach((id, reviewList) -> {
			reviewList.mutate(list -> {
				list.forEachIndexedUnsync((index, e) -> {
					Sample sample = samples.getSample(e.sample_id);
					if(sample != null) {
						Label label = sample.findLabel(e.label_start, e.label_end);
						if(label != null) {
							if(!label.userLabels.isEmpty()) {
								if(e.labeled) {
									// OK
								} else {
									// change
									LabelingListEntry e2 = e.withLabeled(true);
									list.setUnsync(index, e2);
									Logger.info("updated " + e2);
								}
							} else {
								if(e.labeled) {
									// change
									LabelingListEntry e2 = e.withLabeled(false);
									list.setUnsync(index, e2);
									Logger.info("updated " + e2);
								} else {
									// OK
								}
							}
						} else {
							Logger.warn("label not found" + e);
						}
					} else {
						Logger.warn("sample not found " + e);
					}					
				});
				//list.sortUnsync(LabelingListEntry.COMPARATOR);
			});			
		});		
	}
}
