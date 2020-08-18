package audio.processing;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

import util.collections.vec.ReadonlyVecView;
import util.collections.vec.Vec;

public class Metrics {
	
	private final static Vec<Metric> m = new Vec<Metric>();
	public static ReadonlyVecView<Metric> metrics = m.readonlyView();
	private final static TreeMap<String, Metric> mMap = new TreeMap<String, Metric>();
	public final static NavigableMap<String, Metric> metricsMap = Collections.unmodifiableNavigableMap(mMap);
	
	static {
		m.add(Metric_duration.INSATNCE);
		m.add(Metric_intensity.INSATNCE);
		m.add(Metric_max_intensity.INSATNCE);
		m.add(Metric_max_intensity_frequency.INSATNCE);
		m.add(Metric_biophonic_intensity.INSATNCE);
		m.add(Metric_bin1.INSATNCE);
		m.add(Metric_bin2.INSATNCE);
		m.add(Metric_bin3.INSATNCE);
		
		for(Metric metric:m) {
			mMap.put(metric.name, metric);
		}
	}

}
