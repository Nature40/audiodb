package audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import util.collections.vec.Vec;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class Sample {
	static final Logger log = LogManager.getLogger();

	public final String id;
	public final Path metaPath;
	public final Path directoryPath;

	private YamlMap yamlMap;
	private String audioFilename;
	private Path audioPath;
	private File audioFile;
	private Vec<Label> labels;

	public Sample(String id, Path metaPath) {
		this.id = id;		
		this.metaPath = metaPath;
		this.directoryPath = metaPath.getParent();
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
		audioPath = directoryPath.resolve(audioFilename);
		audioFile = audioPath.toFile();

		labels = new Vec<Label>();
		for(YamlMap labelMap:yamlMap.optList("Labels").asMaps()) {		
			log.info(labelMap.toString());
			Label label = Label.ofYAML(labelMap);
			log.info(label.toString());
			labels.add(label);
		}
	}

	public synchronized void writeToFile() {		
		YamlUtil.putList(yamlMap.getRootMap(), "Labels", labels, Label::toMap);		
		YamlUtil.writeSafeYamlMap(metaPath, yamlMap.getRootMap());
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
}
