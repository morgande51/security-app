package com.nge.smarttrigger.spi;

import java.util.concurrent.ScheduledFuture;

public interface SmartTrigger {
	
	public String getName();
	
	public String getId();
	
	public boolean isReady();
	
	public void fire() throws SmartTriggerException;

	public void init(ScheduledFuture<?> resetTask);
	
	public SmartTriggerStateType getState();
	
	public void setState(SmartTriggerStateType state);
	
	public void reset();
	
	public long getResetInterval();
	
	public long getFireInterval();

	public boolean shouldRun();
}