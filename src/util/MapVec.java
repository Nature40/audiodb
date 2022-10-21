package util;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

import util.collections.vec.Vec;

public class MapVec<T> {

	private HashMap<String, Vec<T>> map;

	public MapVec() {
		map = new HashMap<String, Vec<T>>();
	}

	public void add(String key, T value) {
		Vec<T> vec = map.get(key);
		if(vec == null) {
			vec = new Vec<T>();
			map.put(key, vec);
		}
		vec.add(value);
	}

	public void add(String key, IntFunction<T> supplier) {
		Vec<T> vec = map.get(key);
		if(vec == null) {
			vec = new Vec<T>();
			map.put(key, vec);
		}
		vec.add(supplier);
	}
	
	public void forEach(BiConsumer<String, Vec<T>> action) {
		map.forEach(action);
	}
}
