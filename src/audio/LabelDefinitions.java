package audio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.collections.vec.Vec;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class LabelDefinitions {
	private static final Logger log = LogManager.getLogger();

	private final ReadLock readLock;
	private final WriteLock changeLock;
	private final Path labelDefinitionsPath;
	private Vec<LabelDefinition> lds;

	public LabelDefinitions(Path labelDefinitionsPath) {
		this.labelDefinitionsPath = labelDefinitionsPath;
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
		readLock = lock.readLock();
		changeLock = lock.writeLock();	
		lds = new Vec<LabelDefinition>();
		read();
	}

	public boolean read() {
		changeLock.lock();
		try {
			if(!Files.exists(labelDefinitionsPath)) {
				log.info("no file: " + labelDefinitionsPath);
				return false;			
			}
			YamlMap yamlMap = YamlUtil.readYamlMap(labelDefinitionsPath);
			List<YamlMap> ldList = yamlMap.optList("label_definitions").asMaps();
			lds = ldList.stream().map(m -> LabelDefinition.ofYAML(m)).collect(Vec.collector());
			return true;
		} finally {
			changeLock.unlock();
		}
	}

	public void write() {		
		readLock.lock();
		try {
			LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
			Vec<Map<String, Object>> vec = new Vec<Map<String, Object>>();
			lds.forEach(ld -> vec.add(ld.toMap()));
			yamlMap.put("label_definitions", vec);
			YamlUtil.writeSafeYamlMap(labelDefinitionsPath, yamlMap);
		} finally {
			readLock.unlock();
		}
	}

	public void put(LabelDefinition ld) {
		changeLock.lock();
		try {
			lds.add(ld);
			write();
		} finally {
			changeLock.unlock();
		}
	}

	public void forEach(Consumer<LabelDefinition> consumer) {
		readLock.lock();
		try {
			lds.forEach(consumer);
		} finally {
			readLock.unlock();
		}
	}

	public void replace(Vec<LabelDefinition> vec) {
		changeLock.lock();
		try {
			lds = vec.copy();
			write();
		} finally {
			changeLock.unlock();
		}		
	}

}
