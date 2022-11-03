package audio;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;

import org.json.JSONObject;
import org.json.JSONWriter;
import org.tinylog.Logger;

import audio.review.ReviewedLabel;
import util.JsonUtil;
import util.collections.vec.Vec;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class Label {

	public static enum LabelStatus {
		OPEN,
		DONE;

		public static LabelStatus parse(String s) {
			if(s == null) {
				return null;				
			}
			switch(s) {
			case "open": return OPEN;
			case "done": return DONE;
			default: return null;
			}
		}

		@Override
		public String toString() {
			switch(this) {
			case OPEN: return "open";
			case DONE: return "done";
			default: return null;
			}
		}

		public static LabelStatus merge(LabelStatus a, LabelStatus b) {
			if(a == null && b == null) {
				return null;
			}
			if(a == LabelStatus.DONE && b == LabelStatus.DONE) {
				return LabelStatus.DONE;
			}
			return LabelStatus.OPEN;
		}
	}

	public final double start;
	public final double end;
	public final String comment;
	public final Vec<GeneratorLabel> generatorLabels;
	public Vec<UserLabel> userLabels;
	public final Vec<ReviewedLabel> reviewedLabels;
	public LabelStatus labelStatus;

	public static final Comparator<Label> INTERVAL_COMPARATOR = (a,b) -> {
		int c = Double.compare(a.start, b.start);
		if(c != 0) {
			return c;
		}
		return Double.compare(a.end, b.end);
	};

	private Label(double start, double end, String comment, Vec<GeneratorLabel> generatorLabels, Vec<UserLabel> userLabels, Vec<ReviewedLabel> reviewedLabels, LabelStatus labelStatus) {
		this.start = start;
		this.end = end;
		this.comment = comment;
		this.generatorLabels = generatorLabels;
		this.userLabels = userLabels;
		this.reviewedLabels = reviewedLabels;
		this.labelStatus = labelStatus;
	}

	public Label(double start, double end) {
		this(start, end, "", new Vec<GeneratorLabel>(), new Vec<UserLabel>(), new Vec<ReviewedLabel>(), null);
	}

	public static Label ofJSON(JSONObject jsonLabel) {		
		double a = jsonLabel.getDouble("start");
		double b = jsonLabel.getDouble("end");
		double start = Math.min(a, b);
		double end = Math.max(a, b);
		String comment = jsonLabel.optString("comment", "");
		Vec<GeneratorLabel> generatorLabels = JsonUtil.optVec(jsonLabel, "generated_labels", GeneratorLabel::ofJSON);
		Vec<UserLabel> userLabels = JsonUtil.optVec(jsonLabel, "labels", UserLabel::ofJSON);
		Vec<ReviewedLabel> reviewedLabels = JsonUtil.optVec(jsonLabel, "reviewed_labels", ReviewedLabel::ofJSON);
		LabelStatus labelstatus = LabelStatus.parse(jsonLabel.optString("label_status", null));
		return new Label(start, end, comment, generatorLabels, userLabels, reviewedLabels, labelstatus);
	}

	public void toJSON(JSONWriter json) {
		json.object();
		json.key("start");
		json.value(start);
		json.key("end");
		json.value(end);
		if(hasComment()) {
			json.key("comment");
			json.value(comment);		
		}
		JsonUtil.writeArray(json, "generated_labels", generatorLabels, GeneratorLabel::toJSON);
		JsonUtil.writeArray(json, "labels", userLabels, UserLabel::toJSON);
		JsonUtil.writeArray(json, "reviewed_labels", reviewedLabels, ReviewedLabel::toJSON);
		if(labelStatus != null) {
			json.key("label_status");
			json.value(labelStatus.toString());
		}
		json.endObject();		
	}

	public LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("start", start);
		map.put("end", end);
		if(hasComment()) {
			map.put("comment", comment);	
		}
		YamlUtil.putList(map, "generated_labels", generatorLabels, GeneratorLabel::toMap);
		YamlUtil.putList(map, "labels", userLabels, UserLabel::toMap);
		YamlUtil.putList(map, "reviewed_labels", reviewedLabels, ReviewedLabel::toMap);
		if(labelStatus != null) {
			map.put("label_status", labelStatus.toString());
		}
		return map;
	}

	public static Label ofYAML(YamlMap yamlMap) {
		double a = yamlMap.getDouble("start");
		double b = yamlMap.getDouble("end");
		double start = Math.min(a, b);
		double end = Math.max(a, b);		
		String comment = yamlMap.optString("comment", "");
		Vec<GeneratorLabel> generatorLabels = YamlUtil.optVec(yamlMap, "generated_labels", GeneratorLabel::ofYAML);
		Vec<UserLabel> userLabels = YamlUtil.optVec(yamlMap, "labels", UserLabel::ofYAML);
		Vec<ReviewedLabel> reviewedLabels = YamlUtil.optVec(yamlMap, "reviewed_labels", ReviewedLabel::ofYAML);
		String ls = yamlMap.optString("label_status", null);
		LabelStatus labelstatus = LabelStatus.parse(ls);
		return new Label(start, end, comment, generatorLabels, userLabels, reviewedLabels, labelstatus);
	}

	public String[] getGeneratorLabelNames() {
		return generatorLabels.mapArray(String[]::new, GeneratorLabel::name);
	}

	public String[] getUserLabelNames() {
		return userLabels.mapArray(String[]::new, UserLabel::name);
	}

	public Label withCreator(String username, String date) {
		Vec<UserLabel> ul = this.userLabels.map(userLabel -> userLabel.withCreator(username, date));
		return new Label(this.start, this.end, this.comment, this.generatorLabels, ul, this.reviewedLabels, this.labelStatus);
	}

	public boolean hasComment() {
		return !comment.isEmpty();
	}

	public void setReviewedLabel(ReviewedLabel reviewedLabel) {
		int index = reviewedLabels.findIndexOf(r -> r.name.equals(reviewedLabel.name));
		if(index < 0) {
			reviewedLabels.add(reviewedLabel);
		} else {
			reviewedLabels.setFast(index, reviewedLabel);
		}
	}

	public boolean isInterval(double label_start, double label_end) {
		return (start - 0.001d) <= label_start && label_start <= (start + 0.001d) && (end - 0.001d) <= label_end && label_end <= (end + 0.001d);
	}

	public boolean isInterval(Label label2) {		
		return label2 != null && isInterval(label2.start, label2.end);
	}

	public synchronized void addReview(ReviewedLabel reviewedLabel) {
		reviewedLabels.add(reviewedLabel);		
	}

	private static String mergeComment(String a, String b) {
		if(a == null) {
			if(b == null) {
				return null;
			} else {
				return b;
			}
		} else {
			if(b == null) {
				return a;
			} else {
				return a.isBlank() ? b : (a + "   " + b);
			}
		}
	}

	public static Label merge(Label a, Label b) {
		if(!a.isInterval(b)) {
			throw new RuntimeException("not same interval label merge " + a.start + " " + a.end + "   " + b.start + " " + b.end);
		}
		
		double start = a.start;
		
		double end = a.end;
		
		String comment = mergeComment(a.comment, b.comment);
		
		//Logger.info("merge " + a.generatorLabels.toString() + "  " + b.generatorLabels.toString());
		
		Vec<GeneratorLabel> generatorLabels = a.generatorLabels.copy();
		for(GeneratorLabel newGeneratorLabel : b.generatorLabels) {
			if(generatorLabels.none(generatorLabel -> generatorLabel.equals(newGeneratorLabel))) {
				generatorLabels.add(newGeneratorLabel);
			}
		}
		
		//Logger.info("merge " + a.generatorLabels.toString() + "  " + b.generatorLabels.toString()  +" :: " + generatorLabels.toString());
		
		Vec<UserLabel> userLabels = a.userLabels.copy();
		for(UserLabel newUserLabel : b.userLabels) {
			if(userLabels.none(userLabel -> userLabel.equals(newUserLabel))) {
				userLabels.add(newUserLabel);
			}
		}		
		
		Vec<ReviewedLabel> reviewedLabels = a.reviewedLabels.copy();		
		for(ReviewedLabel newReviewedLabel : b.reviewedLabels) {
			if(reviewedLabels.none(reviewedLabel -> reviewedLabel.equals(newReviewedLabel))) {
				reviewedLabels.add(newReviewedLabel);
			}
		}
		
		LabelStatus labelStatus = LabelStatus.merge(a.labelStatus, b.labelStatus);
		
		return new Label(start, end, comment, generatorLabels, userLabels, reviewedLabels, labelStatus);
	}

	public synchronized void setUserLabels(Vec<UserLabel> userLabels) {
		this.userLabels = userLabels.copy();		
	}

	public static boolean hasLabelDublicates(Vec<Label> labels) {
		int len = labels.size();
		for(int outerIndex = 0; outerIndex < len - 1; outerIndex++) {
			Label outerLabel = labels.get(outerIndex);
			for(int innerIndex = outerIndex + 1; innerIndex < len; innerIndex++) {
				Label innerLabel = labels.get(innerIndex);
				if(outerLabel.isInterval(innerLabel)) {
					return true;
				}
			}
		}
		return false;
	}

	public static Vec<Label> mergeLabelDublicates(Vec<Label> labels) {
		Vec<Label> resultLabels = labels.copy();			
		int len = resultLabels.size();
		for(int outerIndex = 0; outerIndex < len - 1; outerIndex++) {
			Label outerLabel = resultLabels.get(outerIndex);
			if(outerLabel != null) {
				for(int innerIndex = outerIndex + 1; innerIndex < len; innerIndex++) {
					Label innerLabel = resultLabels.get(innerIndex);
					if(innerLabel != null && outerLabel.isInterval(innerLabel)) {
						Label labelMerge = Label.merge(outerLabel, innerLabel);
						resultLabels.setFast(outerIndex, labelMerge);
						resultLabels.setFast(innerIndex, null);
						outerLabel = labelMerge;
					}
				}
			}
		}
		resultLabels = resultLabels.filter(label -> label != null);
		return resultLabels;
	}

	@Override
	public String toString() {
		return "Label [start=" + start + ", end=" + end + ", comment=" + comment + ", generatorLabels="
				+ generatorLabels + ", userLabels=" + userLabels + ", reviewedLabels=" + reviewedLabels
				+ ", labelStatus=" + labelStatus + "]";
	}
}
