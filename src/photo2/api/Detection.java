package photo2.api;

import util.collections.vec.Vec;
import util.yaml.YamlMap;

public class Detection {

	public final float[] bbox; // nullable
	public final Vec<YamlMap> classifications; // not null, possibly empty

	public Detection(float[] bbox, Vec<YamlMap> classifications) {
		this.bbox = bbox;
		this.classifications = classifications;
	}
	
	public Detection() {
		this(null);
	}
	
	public Detection(float[] bbox) {
		this(bbox, new Vec<YamlMap>());
	}
	
	public boolean isDetection(float[] bbox) {
		return bboxEquals(this.bbox, bbox);
	}
	
	private boolean tollerantEquals(float a, float b) {
		return a - 0.001 < b && a + 0.001 > b;
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
}
