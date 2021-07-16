package util.yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import util.collections.ReadonlyList;
import util.collections.array.ReadonlyArray;
import util.collections.vec.Vec;

public class YamlList {
	
	private List<Object> list;
	
	public YamlList(Object data) {
		if(data instanceof List) {
			this.list = (List<Object>) data;
		} else {
			this.list = new ArrayList<Object>();
			this.list.add(data);
		}
	}
	
	public YamlList(List<Object> list) {
		this.list = list;
	}
	
	public Vec<YamlMap> asMaps() {
		Vec<YamlMap> vec = new Vec<YamlMap>(list.size());
		for(Object e:list) {
			if(e instanceof Map) {
				vec.add(new YamlMap((Map<String, Object>) e));
			} else {
				throw new RuntimeException("element is no map "+e);
			}
		}
		return vec;
	}
	
	public Vec<String> asStrings() {
		Vec<String> vec = new Vec<String>(list.size());
		for(Object e:list) {
			vec.add(e.toString());
		}
		return vec;
	}
	
	public String[] asStringArray() {
		return asStrings().toArray(new String[0]);
	}
	
	public ReadonlyArray<String> asReadonlyStrings() {
		return ReadonlyList.of(asStringArray());
	}

	public float[] asFloatArray() {
		float[] result = new float[list.size()];
		for (int i = 0; i < result.length; i++) {
			Object o = list.get(i);
			if(o instanceof Number) {
				result[i] = ((Number) o).floatValue();
			} else {
				throw new RuntimeException("element is not a number: "+o);
			}
		}
		return result;
	}
	
	public boolean isEmpty() {
		return list.isEmpty();
	}

	public <T> ReadonlyArray<T> asReadonlyList(Function<YamlMap, T> fun) {
		@SuppressWarnings("unchecked")
		T[] a = (T[]) new Object[list.size()];
		int i = 0;
		for(Object e : list) {
			if(e instanceof Map) {
				YamlMap yamlMap = new YamlMap((Map<String, Object>) e);
				T v = fun.apply(yamlMap);
				a[i] = v;
			} else {
				throw new RuntimeException("element is no map "+e);
			}
			i++;
		}
		return ReadonlyList.of(a);
	}

	public List<Object> getInternalList() {
		return list;		
	}
}
