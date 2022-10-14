package task;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

import org.json.JSONObject;
import org.reflections.Reflections;
import org.tinylog.Logger;

import audio.Account;
import audio.Broker;

public class Tasks {

	private static TreeMap<String, Descriptor> descriptorMap;

	private final Broker broker;	
	private ConcurrentHashMap<String, Task> taskMap;

	private static final String TASK_PREFIX = "Task_";
	private static final int TASK_PREFIX_LEN = TASK_PREFIX.length();
	
	private static final String[] TASK_PACKAGES = new String[] {"audio.task", "photo2.task"};

	static {
		descriptorMap = new TreeMap<String, Descriptor>();
		for(String taskPackage :  TASK_PACKAGES) {
			Set<Class<? extends Task>> taskClasses = new Reflections(taskPackage).getSubTypesOf(Task.class);
			for(Class<? extends Task> taskClass : taskClasses) {
				//Logger.info(taskClass);
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
	
	private static final String idCharacters = "0123456789abcdefghijklmnopqrstuvwxyz";
	private static final int idCharactersLen = idCharacters.length();
	private static String createID() {
		String id = "";
		for (int i = 0; i < 16; i++) {
			id += idCharacters.charAt((int) (Math.abs(ThreadLocalRandom.current().nextLong()) % idCharactersLen));
		}
		return id;
	}

	public String submit(JSONObject json, Account account) {
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
			//id = Long.toHexString(ThreadLocalRandom.current().nextLong());
			id = createID();
			ret = taskMap.putIfAbsent(id, task);
		} while(ret != null);
		try {
			Ctx ctx = new Ctx(descriptor, json, id, broker, account);
			task.setCtxAndInit(ctx);
			ForkJoinPool.commonPool().execute(task);
		} catch (Exception e) {
			Logger.warn(e);	
		}
		return id;
	}
}
