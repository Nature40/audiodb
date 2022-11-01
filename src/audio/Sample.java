package audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Consumer;


import org.tinylog.Logger;
import org.yaml.snakeyaml.Yaml;

import audio.review.ReviewedLabel;
import util.collections.vec.Vec;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class Sample implements GeneralSample {
	

	public final String id;
	public final Path metaPath;
	public final Path dataDirectoryPath;

	private YamlMap yamlMap;
	private String audioFilename;
	private Path audioPath;
	private File audioFile;
	private Vec<Label> labels;
	private SampleUserLocked sampleUserLocked;

	public Sample(String id, Path metaPath, Path dataDirectoryPath) {
		this.id = id;		
		this.metaPath = metaPath;
		this.dataDirectoryPath = dataDirectoryPath;
	}	

	public synchronized void readFromFile() {
		Object inObject;
		try(InputStream in = new FileInputStream(metaPath.toFile())) {
			inObject = new Yaml().load(in);			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if(inObject instanceof String) {
			inObject = new Yaml().load((String) inObject);
		}		
		yamlMap = YamlMap.ofObject(inObject);
		if(!yamlMap.contains("AudioSens")) {
			throw new RuntimeException("no AudioSens");
		}
		audioFilename = yamlMap.getString("file");
		audioPath = dataDirectoryPath.resolve(audioFilename);
		audioFile = audioPath.toFile();

		labels = new Vec<Label>();
		for(YamlMap labelMap:yamlMap.optList("Labels").asMaps()) {		
			//Logger.info(labelMap.toString());
			Label label = Label.ofYAML(labelMap);
			//Logger.info(label.toString());
			labels.add(label);
		}
		if(yamlMap.contains("sample_locked")) {
			sampleUserLocked = SampleUserLocked.ofYAML(yamlMap.getMap("sample_locked"));
		} else {
			sampleUserLocked = null;
		}
	}

	public synchronized void writeToFile() {		
		YamlUtil.putList(yamlMap.getInternalMap(), "Labels", labels, Label::toMap);	
		//Logger.info("write meta" + id);
		YamlUtil.writeSafeYamlMap(metaPath, yamlMap.getInternalMap());
	}

	public synchronized File getAudioFile() {
		return audioFile;
	}

	public synchronized String getAudioFileName() {
		return audioFilename;
	}

	public synchronized YamlMap getMetaMap() {
		return yamlMap;
	}

	public synchronized Vec<Label> getLabels() {
		return labels;
	}

	public synchronized int findLabelIndexOf(double label_start, double label_end) {
		return labels.findIndexOf(e -> e.isInterval(label_start, label_end));		
	}

	public synchronized Label findLabel(double label_start, double label_end) {
		return labels.find(e -> e.isInterval(label_start, label_end));		
	}

	public synchronized Label getLabel(int label_index) {
		return labels.get(label_index);		
	}

	public synchronized void setLabel(int label_index, Label label) {
		labels.set(label_index, label);

	}

	public synchronized void addLabel(Label label) {
		labels.add(label);		
	}

	public synchronized void mutate(Consumer<Sample> action) {
		try {
			action.accept(this);
			writeToFile();
		} catch(Exception e) {
			readFromFile();
		}
	}

	public synchronized void forEachReviewed(Consumer<ReviewedLabel> action) {
		for(Label label : labels) {
			label.reviewedLabels.forEach(action);
		}
	}

	public synchronized void forEachLabel(Consumer<Label> action) {
		labels.forEach(action);
	}

	public synchronized boolean checkLabelDublicates() {
		return Label.hasLabelDublicates(labels);
	}

	public synchronized void checkAndCorrectLabelDublicates() {
		if(checkLabelDublicates()) {
			Logger.info("dublicate labels in " + id);
			Logger.info("old labels size " + labels.size());			
			int len = labels.size();
			for(int outerIndex = 0; outerIndex < len - 1; outerIndex++) {
				Label label = labels.get(outerIndex);
				if(label != null) {
					for(int innerIndex = outerIndex + 1; innerIndex < len; innerIndex++) {
						Label label2 = labels.get(innerIndex);
						if(label.isInterval(label2)) {
							Logger.info("merge");
							Label labelMerge = Label.merge(label, label2);
							labels.setFast(outerIndex, labelMerge);
							labels.setFast(innerIndex, null);
						}
					}
				}
			}
			labels = labels.filter(label -> label != null);
			Logger.info("new labels size " + labels.size());
			writeToFile();
		}
	}

	public synchronized boolean isSampleUserLocked() {
		return sampleUserLocked != null;
	}

	public synchronized void setSampleUserLocked(SampleUserLocked sampleUserLocked) {
		this.sampleUserLocked = sampleUserLocked;
		if(sampleUserLocked != null) {
			yamlMap.getInternalMap().put("sample_locked", sampleUserLocked.toMap());
		} else {
			yamlMap.getInternalMap().remove("sample_locked");
		}
		writeToFile();
	}
}
