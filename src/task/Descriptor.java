package task;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class Descriptor {

	public final String name;
	public final Class<? extends Task> taskClass;
	public final Constructor<? extends Task> constructor;
	public final String description; // nullable
	public final String[] tags; // nullable
	public final boolean cancelable;

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
		if(taskClass.isAnnotationPresent(Tag.class)) {
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
		} else {
			this.tags = null;
		}
		this.cancelable = taskClass.isAnnotationPresent(Cancelable.class);
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