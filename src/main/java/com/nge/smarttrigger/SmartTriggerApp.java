package com.nge.smarttrigger;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.nge.smarttrigger.manager.TriggerInstaller;
import com.nge.smarttrigger.spi.SmartTrigger;
import com.nge.smarttrigger.spi.SmartTriggerException;
import com.nge.smarttrigger.spi.SmartTriggerStateType;

public class SmartTriggerApp implements Runnable {
	
	// TODO: this should be configurable
	public static final int THREAD_POOL_SIZE = 25;
	
	private Map<String, SmartTrigger> triggers;
	private ScheduledExecutorService resetExecutor;
	private ExecutorService triggerExecutor;
	
	public SmartTriggerApp() {
		triggers = new ConcurrentHashMap<>();
		init();
	}
	
	public void init() {
		// Load the map with all the triggers
		ServiceLoader<SmartTrigger> triggerLoader =  ServiceLoader.load(SmartTrigger.class);
		triggerLoader.findFirst().orElseThrow();
		triggers = new ConcurrentHashMap<>();
		triggerLoader.forEach((t) -> triggers.put(t.getId(), t));
		
		// create thread services for the triggers
		// TODO: thread pool sizes may need to be elastic
		triggerExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		resetExecutor = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
		
		// init each trigger
		triggers.forEach((i, t) -> {
			Properties props = null;
			try {
				props = TriggerInstaller.getInstaller().getConfigurationFor(t.getClass());
			}
			catch (IOException e) {
				// property file cannot be read or other IO error
				// TODO: log this
				e.printStackTrace();
			}
			addTrigger(t, Optional.ofNullable(props));
		});
	}

	/*
//	@SuppressWarnings("unchecked")
	public void _addTrigger(String className) throws SmartTriggerException {
		System.out.println("inside addTrigger(String)");
		try {
			Class<?> cls = Class.forName(className);
//			Class<?> cls = ClassLoader.getSystemClassLoader().loadClass(className);
			System.out.println("about to add triggerClass: " + cls.getSimpleName());
			if (SmartTrigger.class.isAssignableFrom(cls)) {
				Object obj = cls.getDeclaredConstructor().newInstance();
				addTrigger(SmartTrigger.class.cast(obj), null);
			}
		}
		catch (Exception e) {
			System.err.println("Did we blow up here?????");
			throw new SmartTriggerException(e);
		}
		System.out.println("Was this successfull?");
	}
	*/
	
	public void addTrigger(SmartTrigger t, Optional<Properties> configuration) {
		ScheduledFuture<?> resetTask = resetExecutor.scheduleAtFixedRate(() -> t.reset(), 1L, t.getResetInterval(), TimeUnit.SECONDS);
		t.init(resetTask, configuration);
		triggers.put(t.getId(), t);
//		triggerExecutor.
		triggerExecutor.execute(asRunnable(t));
	}
	
	public SmartTrigger removeTrigger(String id) throws SmartTriggerException {
		if (!triggers.containsKey(id)) {
			throw new SmartTriggerException("Unknown Trigger: " + id);
		}
		
		SmartTrigger trigger = triggers.get(id);
		trigger.setState(SmartTriggerStateType.REMOVED);
		triggers.remove(id);
		return trigger;
	}
	
	public void run() {
		while (true) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private Runnable asRunnable(SmartTrigger trigger) {
		Runnable r = () -> {
			SmartTriggerStateType state = trigger.getState();
			System.out.println("Trigger State: " + state);
			try {
//				while (state != SmartTriggerStateType.REMOVED) {
				while (trigger.shouldRun()) {
					try {
//						if ((state == SmartTriggerStateType.RUNNING) && trigger.isReady()) {
						if (trigger.isReady()) {
							trigger.fire();
						}
					}
					catch (SmartTriggerException e) {
						// TODO: handle this
					}
					
					Thread.sleep(trigger.getFireInterval());
				}
				System.out.println("Trigger[" + trigger.getId() + "] + has stopped");
			}
			catch (InterruptedException e) {
				// TODO: handle this
				e.printStackTrace();
			}
		};
		return r;
	}
	
	public SmartTrigger getTrigger(String id) throws SmartTriggerException {
		SmartTrigger trigger = triggers.get(id);
		if (trigger == null) {
			throw new SmartTriggerException("No known trigger: " + id);
		}
		return trigger;
	}
	
	public String[] getTriggerIds() {
		return triggers.keySet().toArray((i) -> new String[i]);
	}
}