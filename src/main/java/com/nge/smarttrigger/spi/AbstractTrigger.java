package com.nge.smarttrigger.spi;

import static com.nge.smarttrigger.spi.SmartTriggerStateType.*;

import java.util.Properties;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTrigger implements SmartTrigger {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractTrigger.class);
	
	public static final long DEFAULT_FIRE_INTERVAL = 1000L;
	public static final long DEFAULT_RESET_INTERVAL = 5L;
	
	private String id;
	private SmartTriggerStateType state;
	private ScheduledFuture<?> resetTask;
	private Properties configuration;
	private String info;
	private RuntimeException runtimeExcpetion; 
	
	@Override
	public final void init(ScheduledFuture<?> resetTask, InitRequest request) {
		this.resetTask = resetTask;
		this.info = request.getTriggerInfo();
		this.configuration = request.getConfiguration();
		this.id = request.getId();
		
		SmartTriggerStateType initalState;
		if (configuration == null) {
			initalState = ERROR;
		}
		else if (configuration.isEmpty()) {
			initalState = OFFLINE;
			declareConfiguration(configuration);
		}
		else {
			initalState = init(configuration);
		}
		setState(initalState);
	}
	
	@Override
	public final void init(ScheduledFuture<?> resetTask) {
		this.resetTask = resetTask;
		setState(init(configuration));
	}

	@Override
	public String getId() {
		return id;
	}
	
	@Override
	public String getInfo() {
		return info;
	}
	
	@Override
	public void reset() {
		if (getState() == PAUSED) {
			logger.info("Trigger[{}] is being reset", id);
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
		if (state == REMOVED || state == ERROR) {
			resetTask.cancel(true);
		}
		else if (state == RUNNING) {
			runtimeExcpetion = null;
		}
		this.state = state;
		logger.info("Trigger[{}] current state: {}", id, state);
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
	
	@Override
	public RuntimeException getRuntimeException() {
		return runtimeExcpetion;
	}
	
	@Override
	public void setRuntimeException(RuntimeException e) {
		this.runtimeExcpetion = e;
	}
	
	protected void declareConfiguration(Properties config) {}
	
	protected SmartTriggerStateType init(Properties config) {
		return RUNNING;
	}
}