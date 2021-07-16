package photo2.api;

import java.util.LinkedHashMap;

import util.collections.vec.Vec;
import util.yaml.YamlList;
import util.yaml.YamlMap;

public class PhotoMeta {
	public final YamlMap metaMap;

	public PhotoMeta(YamlMap metaMap) {
		this.metaMap = metaMap;
	}

	private boolean tollerantEquals(float a, float b) {
		return a - 0.00001 < b && a + 0.00001 > b;
	}

	private boolean bboxEquals(float[] a, float[] b) {
		if(a == null) {
			return b == null;
		} else if(b == null) {
			return false;
		} else if(a.length == 4 && b.length == 4){
			return tollerantEquals(a[0], b[0]) && tollerantEquals(a[1], b[1]) && tollerantEquals(a[2], b[2]) && tollerantEquals(a[3], b[3]);
		} else {
			return false;
		}
	}

	public Vec<Detection> getDetections() {
		Vec<Detection> vec = new Vec<Detection>();
		if(metaMap.contains("detections")) {
			YamlList detections = metaMap.getList("detections");
			for(YamlMap m : detections.asMaps()) {
				float[] bbox = m.contains("bbox") ? m.getList("bbox").asFloatArray() : null;
				Vec<LinkedHashMap<String, Object>> classifications = new Vec<LinkedHashMap<String, Object>>();
				if(m.contains("classifications")) {
					for(YamlMap c : m.getList("classifications").asMaps()) {
						LinkedHashMap<String, Object> cm = new LinkedHashMap<String, Object>();
						c.getInternalMap().forEach((k,v) -> {
							cm.put(k, v);
						});
						classifications.add(cm);
					}
				}
				Detection detection = new Detection(bbox, classifications);
				vec.add(detection);
			}
		}		
		return vec;
	}

	public void setDetections(Vec<Detection> detections) {
		Vec<Object> list = new Vec<Object>();
		for(Detection detection : detections) {
			LinkedHashMap<String, java.lang.Object> m = new LinkedHashMap<String, Object>();
			if(detection.bbox != null) {
				m.put("bbox", detection.bbox);
			}
			if(!detection.classifications.isEmpty()) {
				m.put("classifications", detection.classifications);
			}
			list.add(m);
		}
		metaMap.getInternalMap().put("detections", list);
	}

	public void setClassification(float[] bbox, String classification, String classificator, String identity, String date) {
		Vec<Detection> detections = getDetections();
		setClassification(detections, bbox, classification, classificator, identity, date);
		setDetections(detections);
	}

	public void setClassification(Vec<Detection> detections, float[] bbox, String classification, String classificator, String identity, String date) {
		LinkedHashMap<String, Object> cMap = new LinkedHashMap<String, Object>();
		cMap.put("classification", classification);
		cMap.put("classificator", classificator);
		cMap.put("identity", identity);
		cMap.put("date", date);
		
		Detection detection = null;
		for(Detection d : detections) {
			if(d.isDetection(bbox)) {
				detection = d;
				break;
			}
		}
		if(detection == null) {
			detection = new Detection(bbox);
			detections.add(detection);
		}
		detection.classifications.add(cMap);
	}
}
