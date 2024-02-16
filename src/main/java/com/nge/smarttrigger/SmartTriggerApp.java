package com.nge.smarttrigger;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.nge.smarttrigger.manager.TriggerInstaller;
import com.nge.smarttrigger.manager.TriggerManagerImpl;
import com.nge.smarttrigger.spi.InitRequest;
import com.nge.smarttrigger.spi.SmartTrigger;
import com.nge.smarttrigger.spi.SmartTriggerException;
import com.nge.smarttrigger.spi.SmartTriggerStateType;

public class SmartTriggerApp implements Runnable {
	
	public static final String JMX_OBJECT_NAME = "com.dme:type=SmartTrigger,name=TriggerManager";
	
	private static final Object DEV_MODE_PROPERTY = "com.nge.smarttrigger.SmartTriggerApp.devMode";
	
	private static final SmartTriggerApp SINGLETON = new SmartTriggerApp();
	
	// TODO: this should be configurable
	public static final int THREAD_POOL_SIZE = 25;
	
	private Map<String, SmartTrigger> triggers;
	private ScheduledExecutorService resetExecutor;
	private ExecutorService triggerExecutor;
	private boolean devMode;
	
	static {
		SINGLETON.init();
	}
	
	private SmartTriggerApp() {
		triggers = new ConcurrentHashMap<>();
	}
	
	public void init() {
		devMode = System.getProperties().containsKey(DEV_MODE_PROPERTY);
		
		// Load the map with all the triggers
		ServiceLoader<SmartTrigger> triggerLoader =  ServiceLoader.load(SmartTrigger.class);
		if (triggerLoader.findFirst().isEmpty()) {
			System.err.println("No known triggers");
		}
		triggers = new ConcurrentHashMap<>();
		//triggerLoader.forEach((t) -> triggers.put(t.getId(), t));
		
		// create thread services for the triggers
		// TODO: thread pool sizes may need to be elastic
		triggerExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		resetExecutor = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
		
		// init each trigger
		TriggerInstaller ti = TriggerInstaller.getInstaller();
		triggerLoader.forEach((t) -> {
			Class<? extends SmartTrigger> triggerClass = t.getClass();
			
			// get the configuration for this trigger
			Properties config = null;
			try {
				config = ti.getConfiguration(triggerClass);
			}
			catch (IOException e) {
				// config file cannot be read due to IO error
				// TODO: log this
				e.printStackTrace();
			}
			
			// get the info for this trigger
			String triggerInfo;
			try {
				triggerInfo = ti.getTriggerInfo(triggerClass);
			}
			catch (IOException e) {
				System.err.println("Are we in DEV mode: " + devMode);
				if (devMode) {
					ClassLoader cl = ClassLoader.getSystemClassLoader();
					String fileName = ti.getTriggerInfoFileName(triggerClass);
					java.io.InputStream stream = cl.getResourceAsStream(fileName);
					try {
						triggerInfo = ti.getTriggerInfo(stream);
					}
					catch (IOException ioe) {
						System.err.println("no trigger info: " + fileName);
						return;
					}
				}
				else {
					// trigger info cannot be found for trigger
					// TODO: log this
					System.err.append("no config for this trigger: " + t.getId());
					e.printStackTrace();
					return;
				}
			}
			
			addTrigger(t, new InitRequest(config, triggerInfo));
		});
		
		// configure MBServer to support Triggers Management
		// TODO: refactor this into its own method
		try {
			ObjectName objectName = new ObjectName(JMX_OBJECT_NAME);
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			TriggerManagerImpl managerMBean = new TriggerManagerImpl(objectName);
			server.registerMBean(managerMBean, objectName);
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	
	public void addTrigger(SmartTrigger trigger, InitRequest request) {
		ScheduledFuture<?> resetTask = createResetScheduler(trigger);
		trigger.init(resetTask, request);
		triggers.put(request.getId(), trigger);
		triggerExecutor.execute(asRunnable(trigger));
	}
	
	public SmartTrigger removeTrigger(String id) throws SmartTriggerException {		
		SmartTrigger trigger = getTrigger(id);
		trigger.setState(SmartTriggerStateType.REMOVED);
//		triggers.remove(id);
		return trigger;
	}
	
	public void deleteTrigger(String id) throws SmartTriggerException {
		SmartTrigger trigger = removeTrigger(id);
		triggers.remove(id);
		TriggerInstaller.getInstaller().removeTrigger(trigger.getClass());
	}
	
	public void restartTrigger(SmartTrigger trigger) {
		ScheduledFuture<?> resetTask = createResetScheduler(trigger);
		trigger.init(resetTask);
		triggerExecutor.execute(asRunnable(trigger));
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
			catch (RuntimeException e) {
				trigger.setRuntimeException(e);
				trigger.setState(SmartTriggerStateType.ERROR);
			}
		};
		return r;
	}
	
	private ScheduledFuture<?> createResetScheduler(SmartTrigger trigger) {
		return resetExecutor.scheduleAtFixedRate(() -> {
			try {
				trigger.reset();
			}
			catch (SmartTriggerException e) {
				// TODO: log this
				e.printStackTrace();
			}
		}, 1L, trigger.getResetInterval(), TimeUnit.SECONDS);
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
	
	public static SmartTriggerApp getApp() {
		return SINGLETON;
	}
}