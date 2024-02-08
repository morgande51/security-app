package com.nge.smarttrigger.manager;

import java.lang.management.PlatformManagedObject;
import java.util.Set;

import com.nge.smarttrigger.spi.SmartTriggerException;
import com.nge.smarttrigger.spi.SmartTriggerStateType;

public interface TriggerManagerMXBean extends PlatformManagedObject {

	public SmartTriggerStateType getTriggerState(String triggerId);

	public String getLoadingDirectory();

	public String[] getTriggerIds();

	public void removeTrigger(String triggerId);

	public String installTrigger(String triggerFQN);

	public boolean makeTriggerOffline(String triggerId);
	
	public boolean makeTriggerOnline(String triggerId);
	
	public String getTriggerInfo(String triggerId) throws SmartTriggerException;

	public Set<SimpleKeyValue> getTriggerConfig(String triggerId) throws SmartTriggerException;
}