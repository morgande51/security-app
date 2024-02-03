package com.nge.smarttrigger.spi;

import static com.nge.smarttrigger.spi.SmartTriggerStateType.*;

import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

public abstract class AbstractTrigger implements SmartTrigger {
	
	public static final long DEFAULT_FIRE_INTERVAL = 1000L;
	public static final long DEFAULT_RESET_INTERVAL = 5L;
	
	private String id;
	private String name;
	private SmartTriggerStateType state;
	private ScheduledFuture<?> resetTask;
	private Properties configuration;
	
	public AbstractTrigger(String name) {
		this.name = name;
		id = UUID.randomUUID().toString();
	}
	
	@Override
	public void init(ScheduledFuture<?> resetTask, Optional<Properties> config) {
		this.resetTask = resetTask;
		
		SmartTriggerStateType initalState;
		if (config.isEmpty()) {
			initalState = ERROR;
		}
		else {
			configuration = config.get();
			initalState = RUNNING;
		}
		setState(initalState);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getId() {
		return id;
	}
	
	@Override
	public void reset() {
		if (getState() == PAUSED) {
			System.out.println("The trigger is being reset");
			setState(RUNNING);
		}
	}
	
	@Override
	public boolean isReady() {
		return getState() == RUNNING;
	}
	
	@Override
	public boolean shouldRun() {
		boolean run;
		switch (getState()) {
			case REMOVED:
			case ERROR:
				run = false;
				break;
				
			default:
				run = true;
		}
		return run;
	}

	@Override
	public final synchronized SmartTriggerStateType getState() {
		return state;
	}
	
	@Override
	public final synchronized void setState(SmartTriggerStateType state) {
		if (state == SmartTriggerStateType.REMOVED) {
			resetTask.cancel(true);
		}
		this.state = state;
		System.out.println("Tigger[" + id + "] Current state: " + getState());
	}
	
	@Override
	public long getResetInterval() {
		return DEFAULT_RESET_INTERVAL;
	}
	
	@Override
	public long getFireInterval() {
		return DEFAULT_FIRE_INTERVAL;
	}
	
	@Override
	public Properties getProperties() {
		return configuration;
	}
	
	@Override
	public void updateProperty(String name, String value) {
		configuration.put(name, value);
	}
}