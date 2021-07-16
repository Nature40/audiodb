package photo2;

import java.util.function.Consumer;

import org.json.JSONWriter;

public class ClassificationDefinition {
	
    public final String name;
	public final String description;
	
	public ClassificationDefinition(String name, String description) {
		this.name = name;
		this.description = description;
	}
	
	public void toJSON(JSONWriter json) {
		json.object();
		json.key("name");
		json.value(name);
		json.key("description");
		json.value(description);
		json.endObject();
	}
	
	public static JsonConsumer toJsonConsumer(JSONWriter json) {
		return new JsonConsumer(json);
	}
	
	public static class JsonConsumer implements Consumer<ClassificationDefinition> {
		
		private final JSONWriter json;
		
		public JsonConsumer(JSONWriter json) {
			this.json = json;
		}
		
		@Override
		public void accept(ClassificationDefinition t) {
			t.toJSON(json);
			
		}
		
	}
}
