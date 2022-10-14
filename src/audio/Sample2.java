package audio;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;

import org.tinylog.Logger;

import util.HashUtil;
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
	private double duration = Double.NEGATIVE_INFINITY;

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
				if(MetaCreator.supplementYaml(m.getInternalMap(), samplePath)) {
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

	public double duration() {
		if(duration == Double.NEGATIVE_INFINITY) {
			duration = meta().optDouble("Duration", Double.NaN);
		}
		return duration;
	}

	/**
	 * return nullable
	 * @return 
	 */
	public String comment() {
		return meta().optString("Comment");
	}

	/**
	 * return nullable
	 * @return 
	 */
	public String getUTC() {
		String comment = comment();
		if(comment == null) {
			return null;
		}
		try {
			final Matcher offsetMatcher = MetaCreator.UTC_OFFSET_PATTERN.matcher(comment);
			if(offsetMatcher.matches() && offsetMatcher.groupCount() == 1) {
				String offsetText = offsetMatcher.group(1);
				return "UTC" + offsetText;
			} else {
				return null;
			}
		} catch(Exception e) {
			Logger.warn(e);
			return null;
		}
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

	public String getFileHash(boolean createIfMissing) {
		YamlMap meta = meta();
		Map<String, Object> map = meta.getInternalMap();
		String xxh64 = meta.optString("XXH64");
		if(xxh64 == null && createIfMissing) {
			File file = samplePath.toFile();
			if(meta.contains("file_size")) {
				long metaFileSize = meta.getLong("file_size");
				long fileSize = file.length();
				if(metaFileSize != fileSize) {
					throw new RuntimeException("unexpected file size " + fileSize + "  in  " + samplePath.toString());
				}
			} else {
				try {
					long fileSize = file.length();
					map.put("file_size", fileSize); // write later
				} catch(Exception e) {
					Logger.warn(e);
				}
			}
			Logger.info(samplePath);		
			try {
				xxh64 = HashUtil.getFileHashString(file);
				map.put("XXH64", xxh64);
				YamlUtil.writeSafeYamlMap(metaPath, map);
			} catch (Exception e) {
				Logger.warn(e);
			}
		}
		return xxh64;
	}

	public double getTemperature() {
		YamlMap meta = meta();
		double temperature = meta.optDouble("temperature");
		if(!Double.isFinite(temperature)) {
			temperature = MetaCreator.getTemperature(comment());		
		}
		return temperature;
	}
}
