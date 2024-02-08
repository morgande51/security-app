package com.nge.smarttrigger.spi;

import java.util.Properties;
import java.util.concurrent.ScheduledFuture;

public interface SmartTrigger {
	
	public String getId();
	
	public boolean isReady();
	
	public void fire() throws SmartTriggerException;
	
	public void init(ScheduledFuture<?> resetTask, InitRequest request);
	
	public Properties getProperties();
	
	public String getInfo();
	
	public void updateProperty(String name, String value);
	
	public SmartTriggerStateType getState();
	
	public void setState(SmartTriggerStateType state);
	
	public void reset();
	
	public long getResetInterval();
	
	public long getFireInterval();

	public boolean shouldRun();
}