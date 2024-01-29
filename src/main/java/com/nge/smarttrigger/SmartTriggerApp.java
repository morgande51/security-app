package com.nge.smarttrigger;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.nge.smarttrigger.spi.SmartTrigger;
import com.nge.smarttrigger.spi.SmartTriggerException;
import com.nge.smarttrigger.spi.SmartTriggerStateType;

public class SmartTriggerApp implements Runnable {
	
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
		triggers = new ConcurrentHashMap<>();
		triggerLoader.forEach((t) -> triggers.put(t.getId(), t));
		
		// create thread services for the triggers
		// TODO: thread pool sizes may need to be elastic
		triggerExecutor = Executors.newFixedThreadPool(triggers.size());
		resetExecutor = Executors.newScheduledThreadPool(triggers.size());
		
		// init each trigger
		triggers.forEach((i, t) -> addTrigger(t));
	}
	
	@SuppressWarnings("unchecked")
	public void addTrigger(String className) throws SmartTriggerException {
		Class<? extends SmartTrigger> triggerClass;
		try {
			Class<?> cls = Class.forName(className);
			triggerClass = ((Class<? extends SmartTrigger>) SmartTrigger.class.asSubclass(cls));
		}
		catch (ClassNotFoundException e) {
			throw new SmartTriggerException(e);
		}
		addTrigger(triggerClass);
	}
	
	public <T extends SmartTrigger> void addTrigger(Class<T> triggerClass) throws SmartTriggerException {
		try {
			SmartTrigger trigger = triggerClass.getDeclaredConstructor().newInstance();
			triggers.put(trigger.getId(), trigger);
			addTrigger(trigger);
		} 
		catch (Exception e) {
			throw new SmartTriggerException(e);
		}
	}
	
	protected void addTrigger(SmartTrigger t) {
		ScheduledFuture<?> resetTask = resetExecutor.scheduleAtFixedRate(() -> t.reset(), 1L, t.getResetInterval(), TimeUnit.SECONDS);
		t.init(resetTask);
		triggerExecutor.execute(asRunnable(t));
	}
	
	public void removeTrigger(String id) throws SmartTriggerException {
		if (!triggers.containsKey(id)) {
			throw new SmartTriggerException("Unknown Trigger: " + id);
		}
		
		SmartTrigger trigger = triggers.get(id);
		trigger.setState(SmartTriggerStateType.REMOVED);
		triggers.remove(id); 
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
				while (state != SmartTriggerStateType.REMOVED) {
					try {
						if ((state == SmartTriggerStateType.RUNNING) && trigger.isReady()) {
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
	
	public SmartTrigger getTrigger(String id) {
		return triggers.get(id);
	}
	
	public String[] getTriggerIds() {
		return triggers.keySet().toArray((i) -> new String[i]);
	}
}