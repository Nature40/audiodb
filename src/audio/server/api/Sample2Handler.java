package audio.server.api;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.eclipse.jetty.server.Request;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import audio.Account;
import audio.Broker;
import audio.Label;
import audio.Sample2;
import audio.SampleManager;
import audio.UserLabel;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import util.AudioTimeUtil;
import util.JsonUtil;
import util.Web;
import util.collections.vec.Vec;

public class Sample2Handler {


	private final Broker broker;
	private final SampleManager sampleManager;
	private final SpectrumHandler spectrumHandler;
	private final AudioHandler audioHandler;

	public Sample2Handler(Broker broker) {
		this.broker = broker;
		this.sampleManager = broker.sampleManager();
		this.spectrumHandler = new SpectrumHandler(broker);
		this.audioHandler = new AudioHandler(broker);
	}

	public void handle(String sampleId, String target, Request request, HttpServletResponse response) throws IOException {
		Sample2 sample = sampleManager.getById(sampleId);
		if(sample == null) {
			throw new RuntimeException("sample not found");
		}
		if(target.equals("/")) {
			handleRoot(sample, request, response);
		} else {
			int i = target.indexOf('/', 1);
			if(i == 1) {
				throw new RuntimeException("no name: "+target);
			}			
			String name = i < 0 ? target.substring(1) : target.substring(1, i);
			String next = i < 0 ? "/" : target.substring(i);
			switch(name) {
			case "spectrogram": {
				sampleManager.lock.readLock().lock();
				try {
					spectrumHandler.handle(sample, request, response);
				} finally {
					sampleManager.lock.readLock().unlock();
				}
				break;
			}
			case "audio": {
				sampleManager.lock.readLock().lock();
				try {
					audioHandler.handle(sample, request, response);
				} finally {
					sampleManager.lock.readLock().unlock();
				}
				break;
			}
			default:
				throw new RuntimeException("no call");
			}			
		}
	}

	private void handleRoot(Sample2 sample, Request request, HttpServletResponse response) throws IOException {
		switch(request.getMethod()) {
		case "GET":
			sampleManager.lock.readLock().lock();
			try {
				handleRoot_GET(sample, request, response);
			} finally {
				sampleManager.lock.readLock().unlock();
			}
			break;
		case "POST":
			sampleManager.lock.writeLock().lock();
			try {
				handleRoot_POST(sample, request, response);
			} finally {
				sampleManager.lock.writeLock().unlock();
			}
			break;			
		default:
			throw new RuntimeException("no call");
		}
	}

	private void handleRoot_GET(Sample2 sample, Request request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());

		boolean reqSamples = Web.getFlagBoolean(request, "samples");
		boolean reqSampleRate = Web.getFlagBoolean(request, "sample_rate");
		boolean reqLabels = Web.getFlagBoolean(request, "labels");

		json.object();
		json.key("sample");		
		json.object();
		json.key("id");
		json.value(sample.id);
		json.key("project");
		json.value(sample.project);
		if(sample.hasLocation()) {
			json.key("location");
			json.value(sample.location);
		}
		if(sample.hasDevice()) {
			json.key("device");
			json.value(sample.device);
		}
		if(sample.hasTimestamp()) {
			AudioTimeUtil.writePropsTimestampDateTime(json, sample.timestamp);
		}
		if(reqSamples && sample.hasSamples()) {
			json.key("samples");
			json.value(sample.samples());
		}
		if(reqSampleRate && sample.hasSampleRate()) {
			json.key("sample_rate");
			json.value(sample.sampleRate());
		}
		if(reqLabels) {
			Vec<Label> labels = sample.getLabels();
			json.key("labels");
			JsonUtil.writeArray(json, labels,  Label::toJSON);
		}
		json.endObject();
		json.endObject();
	}

	private void handleRoot_POST(Sample2 sample, Request request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession(false);
		Account account = (Account) session.getAttribute("account");
		BitSet roleBits = (BitSet) session.getAttribute("roles");
		broker.roleManager().role_readOnly.checkHasNot(roleBits);

		JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
		JSONArray jsonActions = jsonReq.getJSONArray("actions");
		int jsonActionsLen = jsonActions.length();
		for (int actionIndex = 0; actionIndex < jsonActionsLen; actionIndex++) {
			JSONObject jsonAction = jsonActions.getJSONObject(actionIndex);
			String actionName = jsonAction.getString("action");
			switch(actionName) {
			case "set_label_names": {
				double a = jsonAction.getDouble("start");
				double b = jsonAction.getDouble("end");
				String comment = jsonAction.optString("comment", null);
				if(comment != null && comment.isBlank()) {
					comment = null;
				}
				if(!(Double.isFinite(a) || Double.isFinite(b))) {
					throw new RuntimeException("invalid label range parameter");
				}
				double start = Math.min(a, b);
				double end = Math.max(a, b);
				String[] names = JsonUtil.optStrings(jsonAction, "names");
				Vec<Label> labels = sample.getLabels();
				int labelIndex = labels.findIndexOf(l -> l.isInterval(start, end));
				if(labelIndex < 0) {
					//throw new RuntimeException("label not found");
					Label label = new Label(start, end);
					labelIndex = labels.addGetIndex(label);
				}
				Label label = sample.getLabels().get(labelIndex);
				Vec<UserLabel> userLabels = label.userLabels;
				LinkedHashSet<String> namesSet = new LinkedHashSet<String>();
				Collections.addAll(namesSet, names);
				HashSet<String> retainNames = new HashSet<String>();
				Vec<UserLabel> newUserlabels = new Vec<UserLabel>();
				for(int userLabelIndex = 0; userLabelIndex < userLabels.size(); userLabelIndex++) {
					UserLabel userLabel = userLabels.get(userLabelIndex);
					if(namesSet.contains(userLabel.name)) {
						newUserlabels.add(userLabel);
						retainNames.add(userLabel.name);
					}
				}
				for(String name : namesSet) {
					if(!retainNames.contains(name)) {
						String creator = account.username;
						String creation_date = LocalDateTime.now().toString();
						UserLabel userLabel = new UserLabel(name, creator, creation_date);
						newUserlabels.add(userLabel);
					}
				}
				label.userLabels = newUserlabels;
				String label_status = JsonUtil.optString(jsonAction, "set_label_status", null);
				label.labelStatus = Label.LabelStatus.parse(label_status);
				if(comment != null) {
					label.comment = comment;
				}
				sample.setLabels(labels);
				break;
			}
			case "add_label": {
				double a = jsonAction.getDouble("start");
				double b = jsonAction.getDouble("end");
				double start = Math.min(a, b);
				double end = Math.max(a, b);
				double fqa = jsonAction.optDouble("lower");
				double fqb = jsonAction.optDouble("upper");
				double lower = Math.min(fqa, fqb);
				double upper = Math.max(fqa, fqb);
				if(!Double.isFinite(lower)) {
					lower = Double.NaN;
				}
				if(!Double.isFinite(upper)) {
					upper = Double.NaN;
				}
				String[] names = JsonUtil.optStrings(jsonAction, "names");
				Vec<Label> labels = sample.getLabels();
				Vec<UserLabel> newUserlabels = new Vec<UserLabel>();
				for(String name : names) {
					String creator = account.username;
					String creation_date = LocalDateTime.now().toString();
					UserLabel userLabel = new UserLabel(name, creator, creation_date);
					newUserlabels.add(userLabel);
				}
				Label label = new Label(start, end, lower, upper);
				label.userLabels = newUserlabels;
				labels.add(label);
				sample.setLabels(labels);
				break;
			}
			case "remove_label": {
				double a = jsonAction.getDouble("start");
				double b = jsonAction.getDouble("end");
				double start = Math.min(a, b);
				double end = Math.max(a, b);
				Vec<Label> labels = sample.getLabels();
				int labelIndex = labels.findIndexOf(l -> l.isInterval(start, end));
				if(labelIndex < 0) {
					throw new RuntimeException("label not found");
				}
				labels.remove(labelIndex);
				sample.setLabels(labels);
				break;
			}
			default:
				throw new RuntimeException("unknown action: " + actionName);
			}
		}		

		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("result");
		json.value("OK");
		json.endObject();
	}
}