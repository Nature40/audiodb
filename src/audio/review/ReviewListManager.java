package audio.review;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import audio.Label;
import audio.Sample;
import audio.Samples;

public class ReviewListManager {
	private static final Logger log = LogManager.getLogger();

	ConcurrentSkipListMap<String, ReviewList> reviewListMap = new ConcurrentSkipListMap<>();
	private final Path root;

	public ReviewListManager(Path root) {
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
						log.warn("Error loading " + sub);
					}
				}
			} else {
				log.warn("unknown entity: " + sub);
			}
		}	
	}

	private void load(Path path, String id) {		
		ReviewList reviewList = ReviewList.ofFile(path);
		reviewListMap.put(id, reviewList);
	}

	public void forEach(BiConsumer<? super String, ? super ReviewList> action) {
		reviewListMap.forEach(action);
	}

	public ReviewList getThrow(String id) {
		ReviewList reviewList = reviewListMap.get(id);
		if(reviewList == null) {
			throw new RuntimeException("review_list not found: " + id);
		}
		return reviewList;
	}

	public void updateReviewLists(Samples samples) {
		forEach((id, reviewList) -> {
			reviewList.mutate(list -> {
				list.forEachIndexedUnsync((index, e) -> {
					Sample sample = samples.getSample(e.sample_id);
					if(sample != null) {
						Label label = sample.findLabel(e.label_start, e.label_end);
						if(label != null) {
							if(label.reviewedLabels.find(reviewedLabel -> reviewedLabel.name.equals(e.label_name)) != null) {
								if(e.classified) {
									// OK
								} else {
									// change
									ReviewListEntry e2 = e.withClassified(true);
									list.setUnsync(index, e2);
									log.info("updated " + e2);
								}
							} else {
								if(e.classified) {
									// change
									ReviewListEntry e2 = e.withClassified(false);
									list.setUnsync(index, e2);
									log.info("updated " + e2);
								} else {
									// OK
								}
							}
						} else {
							log.warn("label not found" + e);
						}
					} else {
						log.warn("sample not found " + e);
					}					
				});
				list.sortUnsync(ReviewListEntry.COMPARATOR);
			});			
		});		
	}
}
