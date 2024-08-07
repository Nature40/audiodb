package photo.api;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import io.jsonwebtoken.lang.Objects;
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
				Vec<YamlMap> classifications = new Vec<YamlMap>();
				if(m.contains("classifications")) {
					for(YamlMap c : m.getList("classifications").asMaps()) {
						LinkedHashMap<String, Object> cm = new LinkedHashMap<String, Object>();
						c.getInternalMap().forEach((k,v) -> {
							cm.put(k, v);
						});
						classifications.add(new YamlMap(cm));
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
				Vec<Map<String, Object>> cs = new Vec<Map<String, Object>>();
				detection.classifications.forEach(c -> {
					cs.add(c.getInternalMap());
				});
				m.put("classifications", cs);
			}
			list.add(m);
		}
		metaMap.getInternalMap().put("detections", list);
	}

	public boolean setClassification(float[] bbox, String classification, String classificator, String identity, String date, float conf) {
		Vec<Detection> detections = getDetections();
		boolean ret = setClassification(detections, bbox, classification, classificator, identity, date, conf);
		setDetections(detections);
		return ret;
	}

	public boolean setClassification(Vec<Detection> detections, float[] bbox, String classification, String classificator, String identity, String date, float conf) {
		LinkedHashMap<String, Object> cMap = new LinkedHashMap<String, Object>();
		cMap.put("classification", classification);
		cMap.put("classificator", classificator);
		cMap.put("identity", identity);
		cMap.put("date", date);
		if(Float.isFinite(conf)) {
			cMap.put("conf", conf);
		}

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
		if(detection.classifications.none(m -> {
			Map<String, Object> oMap = m.getInternalMap();
			Object cConf = cMap.get("conf");
			Object oConf = oMap.get("conf");
			float cConfN = cConf != null && cConf instanceof Number ? ((Number)cConf).floatValue() : -9999;
			float oConfN = oConf != null && oConf instanceof Number ? ((Number)oConf).floatValue() : -9999;
			
			return Objects.nullSafeEquals(cMap.get("classification"), oMap.get("classification"))
					&& Objects.nullSafeEquals(cMap.get("classificator"), oMap.get("classificator"))
					&& Objects.nullSafeEquals(cMap.get("identity"), oMap.get("identity"))
					&& Objects.nullSafeEquals(cMap.get("date"), oMap.get("date"))
					&& cConfN == oConfN;
		})) {
			detection.classifications.add(new YamlMap(cMap));
			return true;
		} else {
			return false;
		}
	}

	public LinkedHashSet<String> getClassifications() {
		LinkedHashSet<String> classificationSet = new LinkedHashSet<String>();
		for(Detection detection : getDetections()) {
			for(YamlMap c : detection.classifications) {
				Object classification = c.optString("classification");
				if(classification != null) {
					classificationSet.add(classification.toString());
				}
			}
		}
		//Logger.info(classificationSet);
		return classificationSet;
	}

	public boolean isClassifiedAsPerson() {
		//Logger.info(getClassifications().contains("person"));
		LinkedHashSet<String> classifications = getClassifications();
		return classifications.contains("person") || classifications.contains("Human");
	}
}
