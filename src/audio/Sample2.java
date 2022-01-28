package audio;

import java.io.File;
import java.nio.file.Path;


import org.tinylog.Logger;

import util.collections.vec.Vec;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class Sample2 implements GeneralSample {

	public final String id;
	public final String project;
	public final Path metaPath;
	public final Path samplePath;
	public final String location;
	public final long timestamp;
	public final String device;

	private YamlMap yamlMap = null;
	private long samples = -2;
	private double sampleRate = Double.NEGATIVE_INFINITY;
	private Vec<Label> labels = null;

	public Sample2(String id, String project, Path metaPath, Path samplePath, String location, long timestamp, String device) {
		this.id = id;
		this.project = project;
		this.metaPath = metaPath;
		this.samplePath = samplePath;
		this.location = location;
		this.timestamp = timestamp;
		this.device = device;
	}

	public Sample getSample() {
		return new Sample(id, metaPath, samplePath.getParent());
	}

	public boolean hasLocation() {
		return location != null;
	}

	public boolean hasTimestamp() {
		return timestamp > 0;
	}

	public boolean hasDevice() {
		return device != null;
	}

	@Override
	public File getAudioFile() {
		File audioFile = samplePath.toFile();
		return audioFile;
	}

	public YamlMap meta() {
		if(this.yamlMap == null) {
			this.yamlMap = YamlUtil.readYamlMap(metaPath);
		}
		return this.yamlMap;
	}

	public long samples() {
		if(samples == -2) {
			YamlMap m = meta();
			samples = m.optLong("Samples", -1);
			if(samples == -1) {
				if(Command_create_yaml.supplementYaml(m.getInternalMap(), samplePath)) {
					YamlUtil.writeSafeYamlMap(metaPath, yamlMap.getInternalMap());
					samples = m.optLong("Samples", -1);
				} else {
					this.yamlMap = null;
				}
			}
		}
		return samples;
	}

	public boolean hasSamples() {
		return samples() >= 0;
	}

	public double sampleRate() {
		if(sampleRate == Double.NEGATIVE_INFINITY) {
			sampleRate = meta().optDouble("SampleRate", Double.NaN);
		}
		return sampleRate;
	}

	public boolean hasSampleRate() {
		return Double.isFinite(sampleRate());
	}

	public Vec<Label> getLabels() {
		if(this.labels == null) {
			Vec<Label> labels = new Vec<Label>();
			for(YamlMap labelMap:meta().optList("Labels").asMaps()) {		
				Label label = Label.ofYAML(labelMap);
				labels.add(label);
			}
			this.labels = labels;
		}
		return this.labels;
	}
	
	public void setLabels(Vec<Label> labels) {	
		Logger.info("setLabels");
		this.labels = labels;
		YamlUtil.putList(yamlMap.getInternalMap(), "Labels", labels, Label::toMap);		
		YamlUtil.writeSafeYamlMap(metaPath, yamlMap.getInternalMap());		
	}
}
