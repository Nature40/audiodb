package util.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class VecMap implements Map<String, Object>{

	public static class Entr implements Entry<String, Object> {

		public final String k;
		public Object v;

		public Entr(String k, Object v) {
			this.k = k;
			this.v = v;
		}

		@Override
		public String getKey() {
			return k;
		}

		@Override
		public Object getValue() {
			return v;
		}

		@Override
		public Object setValue(Object value) {
			Object prev = v;
			v = value;
			return prev;
		}		
	}

	private static final Entr[] DEFAULT_SIZED_EMPTY_ARRAY = {};

	protected int size;
	protected Entr[] items;

	public VecMap() {
		items = DEFAULT_SIZED_EMPTY_ARRAY;
	}

	public VecMap(int initialCapacity) {
		items = new Entr[initialCapacity];
	}

	@Override
	public void clear() {
		int len = size;
		Entr[] data = items;
		for (int i = 0; i < len; i++) {
			data[i] = null;
		}		
		size = 0;		
	}

	@Override
	public boolean containsKey(Object key) {
		int len = size;
		Entr[] data = items;
		for (int i = 0; i < len; i++) {
			if(key.equals(data[i].k)) {
				return true;
			}
		}
		return false;
	}

	public boolean containsKey(String key) {
		int len = size;
		Entr[] data = items;
		for (int i = 0; i < len; i++) {
			if(key.equals(data[i].k)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		int len = size;
		Entr[] data = items;
		for (int i = 0; i < len; i++) {
			if(value.equals(data[i].v)) {
				return true;
			}
		}
		return false;
	}
	
	public class EntrSet implements Set<Entry<String, Object>> {

		@Override
		public boolean add(Entry<String, Object> e) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public boolean addAll(Collection<? extends Entry<String, Object>> c) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public void clear() {
			VecMap.this.clear();			
		}

		@Override
		public boolean contains(Object o) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public boolean isEmpty() {
			return VecMap.this.isEmpty();
		}

		@Override
		public Iterator<Entry<String, Object>> iterator() {
			return new EntrIterator();
		}

		@Override
		public boolean remove(Object o) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public int size() {
			return VecMap.this.size;
		}

		@Override
		public Object[] toArray() {
			throw new RuntimeException("not implemented");
		}

		@Override
		public <T> T[] toArray(T[] a) {
			throw new RuntimeException("not implemented");
		}		
	}
	
	private class EntrIterator implements Iterator<Entry<String, Object>> {
		private int pos = 0;

		@Override
		public boolean hasNext() {
			return pos < VecMap.this.size;
		}

		@Override
		public Entr next() {
			return VecMap.this.items[pos++];
		}		
	}

	@Override
	public EntrSet entrySet() {
		return new EntrSet();
	}
	
	public Object get(String key) {
		int len = size;
		Entr[] data = items;
		for (int i = 0; i < len; i++) {
			if(key.equals(data[i].k)) {
				return data[i].v;
			}
		}
		return null;
	}
	
	private Entr getEntry(String key) {
		int len = size;
		Entr[] data = items;
		for (int i = 0; i < len; i++) {
			if(key.equals(data[i].k)) {
				return data[i];
			}
		}
		return null;
	}

	@Override
	public Object get(Object key) {
		int len = size;
		Entr[] data = items;
		for (int i = 0; i < len; i++) {
			if(key.equals(data[i].k)) {
				return data[i].v;
			}
		}
		return null;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	private class KeySet implements Set<String> {

		@Override
		public boolean add(String arg0) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public boolean addAll(Collection<? extends String> arg0) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public void clear() {
			VecMap.this.clear();			
		}

		@Override
		public boolean contains(Object arg0) {
			return VecMap.this.containsKey(arg0);
		}

		@Override
		public boolean containsAll(Collection<?> arg0) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public boolean isEmpty() {
			return VecMap.this.isEmpty();
		}

		@Override
		public Iterator<String> iterator() {
			return new KeyIterator();
		}

		@Override
		public boolean remove(Object arg0) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public boolean removeAll(Collection<?> arg0) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public boolean retainAll(Collection<?> arg0) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public int size() {
			return VecMap.this.size;
		}

		@Override
		public Object[] toArray() {
			throw new RuntimeException("not implemented");
		}

		@Override
		public <T> T[] toArray(T[] arg0) {
			throw new RuntimeException("not implemented");
		}

	}

	class KeyIterator implements Iterator<String> {		
		private int pos = 0;

		@Override
		public boolean hasNext() {
			return pos < VecMap.this.size;
		}

		@Override
		public String next() {
			return VecMap.this.items[pos++].k;
		}		
	}

	@Override
	public Set<String> keySet() {
		return new KeySet();
	}
	
	private void growForOne() {
		if (items == DEFAULT_SIZED_EMPTY_ARRAY) {
			items = new Entr[10];
		} else {
			int oldLen = size;
			int newLen = oldLen + (oldLen >> 1) + 1;
			Entr[] newItems = new Entr[newLen];
			System.arraycopy(this.items, 0, newItems, 0, oldLen);
			items = newItems;
		}
	}
	
	private void add(Entr e) {		
		if (items.length == size) {
			growForOne();
		}
		items[size++] = e;
	}

	@Override
	public Object put(String key, Object value) {
		Entr e = getEntry(key);
		if(e == null) {
			add(new Entr(key, value));
			return null;
		} else {
			return e.setValue(value);
		}
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public Object remove(Object key) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public int size() {
		return size;
	}

	public class ValueCollection implements Collection<Object> {

		@Override
		public boolean add(Object e) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public boolean addAll(Collection<? extends Object> c) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public void clear() {
			VecMap.this.clear();			
		}

		@Override
		public boolean contains(Object value) {
			return VecMap.this.containsValue(value);
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public boolean isEmpty() {
			return VecMap.this.isEmpty();
		}

		@Override
		public Iterator<Object> iterator() {
			return new ValueIterator();
		}

		@Override
		public boolean remove(Object o) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public int size() {
			return VecMap.this.size;
		}

		@Override
		public Object[] toArray() {
			throw new RuntimeException("not implemented");
		}

		@Override
		public <T> T[] toArray(T[] a) {
			throw new RuntimeException("not implemented");
		}		
	}

	private class ValueIterator implements Iterator<Object> {
		private int pos = 0;

		@Override
		public boolean hasNext() {
			return pos < VecMap.this.size;
		}

		@Override
		public Object next() {
			return VecMap.this.items[pos++].v;
		}		
	}

	@Override
	public ValueCollection values() {
		return new ValueCollection();
	}
}
