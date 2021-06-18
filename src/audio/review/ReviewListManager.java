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
				int[] sampleNotFoundCount = new int[] {0};
				list.forEachIndexedUnsync((index, e) -> {
					Sample sample = samples.getSample(e.sample_id);
					if(sample != null) {
						if(e.missing_sample) {
							e = e.withMissingSample(false);
							list.setUnsync(index, e);
							log.info("updated " + e);
						}
						Label label = sample.findLabel(e.label_start, e.label_end);
						if(label != null) {
							ReviewListEntry e1 = e;
							ReviewedLabel reviewedLabel = label.reviewedLabels.findLast(rl -> rl.name.equals(e1.label_name));
							if(reviewedLabel != null) {
								if(e.classified) {
									if(reviewedLabel.reviewed == e.latest_review) {
										// OK
									} else {
										// change
										e = e.withClassifiedAndReviewed(true, reviewedLabel.reviewed);
										list.setUnsync(index, e);
										log.info("updated " + e);
									}
								} else {
									// change
									e = e.withClassifiedAndReviewed(true, reviewedLabel.reviewed);
									list.setUnsync(index, e);
									log.info("updated " + e);
								}
							} else {
								if(e.classified) {
									// change
									e = e.withClassifiedAndReviewed(false, null);
									list.setUnsync(index, e);
									log.info("updated " + e);
								} else {
									// OK
								}
							}
						} else {
							log.warn("label not found" + e);
						}
					} else {
						sampleNotFoundCount[0]++;
						if(sampleNotFoundCount[0] <= 3) {
							log.warn("sample not found " + e);
						}
						// change
						if(!e.missing_sample) {
							e = e.withMissingSample(true);
							list.setUnsync(index, e);
							if(sampleNotFoundCount[0] <= 3) {
								log.info("updated " + e);
							}
						}
					}					
				});
				//list.sortUnsync(ReviewListEntry.COMPARATOR);
				if(sampleNotFoundCount[0] > 0) {
					log.info(sampleNotFoundCount[0] + " samples not found for " + id);
				}
			});			
		});		
	}
}