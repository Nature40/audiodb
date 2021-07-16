package photo2.api;

import java.util.LinkedHashMap;

import util.collections.vec.Vec;

public class Detection {

	public final float[] bbox; // nullable
	public final Vec<LinkedHashMap<String, Object>> classifications; // not null, possibly empty

	public Detection(float[] bbox, Vec<LinkedHashMap<String, Object>> classifications) {
		this.bbox = bbox;
		this.classifications = classifications;
	}
	
	public Detection(float[] bbox) {
		this(bbox, new Vec<LinkedHashMap<String, Object>>());
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
