package com.nge.smarttrigger.manager;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import com.nge.smarttrigger.SmartTriggerApp;
import com.nge.smarttrigger.spi.SmartTrigger;
import com.nge.smarttrigger.spi.SmartTriggerException;
import com.nge.smarttrigger.spi.SmartTriggerStateType;

public class TriggerManagerImpl implements TriggerManagerMBean {

	private SmartTriggerApp app;
	private TriggerInstaller installer;
	
	public TriggerManagerImpl(SmartTriggerApp app, TriggerInstaller installer) {
		this.app = app;
		this.installer = installer;
	}
	
	public String installTrigger(String triggerFQN) {
		SmartTrigger trigger;
		try {
			trigger = installer.loadTrigger(triggerFQN);
			installer.createConfigurationFor(trigger.getClass());
		}
		catch (SmartTriggerException e) {
			// TODO: trigger cannot be installed
			return null;
		}
		catch (IOException e) {
			// TODO: this really shouldn't happen
			return null;
		}
		
		app.addTrigger(trigger, Optional.of(new Properties()));
		return trigger.getId();
	}
	
	public String[] getTriggerIds() {
		return app.getTriggerIds();
	}
	
	public String getLoadingDirectory() {
		return installer.getLoadingDirectory().toString();
	}
	
	public String getTriggerState(String triggerId) throws SmartTriggerException {
		SmartTrigger trigger = app.getTrigger(triggerId);
		return trigger.getState().name();
	}
	
	public void setTriggerOffline(String triggerId) throws SmartTriggerException {
		SmartTrigger trigger = app.getTrigger(triggerId);
		trigger.setState(SmartTriggerStateType.OFFLINE);
	}
	
	public void deleteTrigger(String triggerId) throws SmartTriggerException {
		SmartTrigger trigger = app.removeTrigger(triggerId);
		installer.removeTrigger(trigger.getClass());
	}
}