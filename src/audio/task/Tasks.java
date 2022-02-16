package audio.task;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

import org.json.JSONObject;
import org.reflections.Reflections;
import org.tinylog.Logger;

import audio.Broker;
import audio.task.Task.Ctx;

public class Tasks {

	private static HashMap<String, Descriptor> descriptorMap;

	private final Broker broker;	
	private ConcurrentHashMap<String, Task> taskMap;

	public static class Descriptor {

		public final String name;
		public final Class<? extends Task> taskClass;
		public final Constructor<? extends Task> constructor;

		public Descriptor(String name, Class<? extends Task> taskClass) {
			this.name = name;
			this.taskClass = taskClass;
			try {
				constructor = taskClass.getDeclaredConstructor();
			} catch (NoSuchMethodException | SecurityException e) {
				throw new RuntimeException(e);
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

	private static final String TASK_PREFIX = "Task_";
	private static final int TASK_PREFIX_LEN = TASK_PREFIX.length();

	static {
		descriptorMap = new HashMap<String, Descriptor>();
		Set<Class<? extends Task>> taskClasses = new Reflections("audio.task").getSubTypesOf(Task.class);
		for(Class<? extends Task> taskClass : taskClasses) {
			Logger.info(taskClass);
			String name = taskClass.getSimpleName();			
			if(name.startsWith(TASK_PREFIX)) {
				name = name.substring(TASK_PREFIX_LEN);
			}
			Descriptor descriptor = new Descriptor(name, taskClass);
			if(descriptorMap.containsKey(name)) {
				Logger.warn("overwrite existing name " + name + "  of  " + descriptorMap.get(name).taskClass + "  to  " + taskClass);
			}
			descriptorMap.put(name, descriptor);
		}
	}

	public static void foreachDescriptor(BiConsumer<String, Descriptor> action) {
		descriptorMap.forEach(action);
	}
	
	public Task[] getTasks() {
		Task[] tasks = taskMap.values().toArray(Task[]::new);
		Arrays.sort(tasks);
		return tasks;
	}
	
	public Task getTask(String id) {
		return taskMap.get(id);
	}

	public Tasks(Broker broker) {
		this.broker = broker;
		this.taskMap = new ConcurrentHashMap<String, Task>();
	}

	public String submit(JSONObject json) {
		String taskName = json.optString("task", null);
		if(taskName == null) {
			throw new RuntimeException("missing task parameter in task");
		}
		Descriptor descriptor = descriptorMap.get(taskName);
		if(descriptor == null) {
			throw new RuntimeException("unknown task");
		}
		Task task = descriptor.newInstance();
		String id = null;
		Task ret = null;
		do {
			id = Long.toHexString(ThreadLocalRandom.current().nextLong());
			ret = taskMap.putIfAbsent(id, task);
		} while(ret != null);
		try {
			Ctx ctx = new Ctx(descriptor, json, id, broker);
			task.setCtxAndInit(ctx);
			ForkJoinPool.commonPool().execute(task);
		} catch (Exception e) {
			Logger.warn(e);	
		}
		return id;
	}
}
