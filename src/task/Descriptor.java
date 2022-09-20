package task;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class Descriptor {

	public static class Param {

		public static enum Type {
			BOOLEAN,
			STRING
		};

		public final String name;
		public final Type type;
		public final String preset;
		public final String description;

		public Param(String name, Type type, String preset, String description) {
			this.name = name;
			this.type = type;
			this.preset = preset;
			this.description = description;
		}
	}

	public final String name;
	public final Class<? extends Task> taskClass;
	public final Constructor<? extends Task> constructor;
	public final String description; // nullable
	public final String[] tags; // nullable
	public final boolean cancelable;
	public final Param[] params;

	public Descriptor(String name, Class<? extends Task> taskClass) {
		this.name = name;
		this.taskClass = taskClass;
		try {
			this.constructor = taskClass.getDeclaredConstructor();
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
		if(taskClass.isAnnotationPresent(Description.class)) {
			this.description = taskClass.getAnnotation(Description.class).value();
		} else {
			this.description = null;
		}	
		{
			Tag[] annotations = taskClass.getAnnotationsByType(Tag.class);
			if(annotations != null && annotations.length > 0) {
				String[] res = new String[annotations.length];
				for (int i = 0; i < annotations.length; i++) {
					res[i] = annotations[i].value();
				}
				this.tags = res;
			} else {
				this.tags = null;
			}
		}
		this.cancelable = taskClass.isAnnotationPresent(Cancelable.class);		
		{
			task.Param[] annotations = taskClass.getAnnotationsByType(task.Param.class);
			if(annotations != null && annotations.length > 0) {
				Param[] res = new Param[annotations.length];
				for (int i = 0; i < annotations.length; i++) {
					task.Param p = annotations[i];
					res[i] = new Param(p.name(), p.type(), p.preset(), p.description());
				}
				this.params = res;
			} else {
				this.params = null;
			}
		}
	}

	public Task newInstance() {
		try {
			Task task = constructor.newInstance();
			return task;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}