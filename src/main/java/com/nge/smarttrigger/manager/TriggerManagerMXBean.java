package com.nge.smarttrigger.manager;

public interface TriggerManagerMXBean {

	public String getTriggerState(String triggerId);

	public String getLoadingDirectory();

	public String[] getTriggerIds();

	public void removeTrigger(String triggerId);

	public String installTrigger(String triggerFQN);

	public boolean makeTriggerOffline(String triggerId);
	
	public boolean makeTriggerOnline(String triggerId);
}