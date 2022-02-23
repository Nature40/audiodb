package audio;

import java.util.Comparator;
import java.util.LinkedHashMap;

import org.json.JSONObject;
import org.json.JSONWriter;

import audio.review.ReviewedLabel;
import util.JsonUtil;
import util.collections.vec.Vec;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class Label {
	public final double start;
	public final double end;
	public final String comment;
	public final Vec<GeneratorLabel> generatorLabels;
	public Vec<UserLabel> userLabels;
	public final Vec<ReviewedLabel> reviewedLabels;
	
	public static final Comparator<Label> INTERVAL_COMPARATOR = (a,b) -> {
		int c = Double.compare(a.start, b.start);
		if(c != 0) {
			return c;
		}
		return Double.compare(a.end, b.end);
	};
	
	private Label(double start, double end, String comment, Vec<GeneratorLabel> generatorLabels, Vec<UserLabel> userLabels, Vec<ReviewedLabel> reviewedLabels) {
		this.start = start;
		this.end = end;
		this.comment = comment;
		this.generatorLabels = generatorLabels;
		this.userLabels = userLabels;
		this.reviewedLabels = reviewedLabels;
	}

	public Label(double start, double end) {
		this(start, end, "", new Vec<GeneratorLabel>(), new Vec<UserLabel>(), new Vec<ReviewedLabel>());
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
		return new Label(start, end, comment, generatorLabels, userLabels, reviewedLabels);
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
		return new Label(start, end, comment, generatorLabels, userLabels, reviewedLabels);
	}

	public String[] getGeneratorLabelNames() {
		return generatorLabels.mapArray(String[]::new, GeneratorLabel::name);
	}
	
	public String[] getUserLabelNames() {
		return userLabels.mapArray(String[]::new, UserLabel::name);
	}

	public Label withCreator(String username, String date) {
		Vec<UserLabel> ul = this.userLabels.map(userLabel -> userLabel.withCreator(username, date));
		return new Label(this.start, this.end, this.comment, this.generatorLabels, ul, this.reviewedLabels);
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

	public synchronized void addReview(ReviewedLabel reviewedLabel) {
		reviewedLabels.add(reviewedLabel);		
	}

	public static Label merge(Label label, Label label2) {
		double start = label.start;
		double end = label.end;
		String comment = label.comment;
		Vec<GeneratorLabel> generatorLabels = label.generatorLabels.copy();
		Vec<UserLabel> userLabels = label.userLabels.copy();
		Vec<ReviewedLabel> reviewedLabels = label.reviewedLabels.copy();
		if(start != label2.start || end != label2.end) {
			throw new RuntimeException("not same interval label merge");
		}
		for(GeneratorLabel generatorLabel : label2.generatorLabels) {
			if(!generatorLabels.some(g -> g.equals(generatorLabel))) {
				generatorLabels.add(generatorLabel);
			}
		}
		for(UserLabel userLabel : label2.userLabels) {
			if(!userLabels.some(g -> g.equals(userLabel))) {
				userLabels.add(userLabel);
			}
		}
		for(ReviewedLabel reviewedLabel : label2.reviewedLabels) {
			if(!reviewedLabels.some(g -> g.equals(reviewedLabel))) {
				reviewedLabels.add(reviewedLabel);
			}
		}
		return new Label(start, end, comment, generatorLabels, userLabels, reviewedLabels);
	}

	@Override
	public String toString() {
		return "Label [start=" + start + ", end=" + end + ", comment=" + comment + ", generatorLabels="
				+ generatorLabels + ", userLabels=" + userLabels + ", reviewedLabels=" + reviewedLabels + "]";
	}

	public synchronized void setUserLabels(Vec<UserLabel> userLabels) {
		this.userLabels = userLabels.copy();		
	}
}
