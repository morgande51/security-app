package com.nge.smarttrigger.manager;

import java.util.Optional;
import java.util.Properties;

import com.nge.smarttrigger.SmartTriggerApp;
import com.nge.smarttrigger.spi.SmartTrigger;
import com.nge.smarttrigger.spi.SmartTriggerException;
import com.nge.smarttrigger.spi.SmartTriggerStateType;

public class TriggerManagerImpl implements TriggerManagerMXBean {

	private SmartTriggerApp app;
	private TriggerInstaller installer;
	
	@Override
	public String installTrigger(String triggerFQN) {
		SmartTrigger trigger;
		try {
			trigger = installer.loadTrigger(triggerFQN);
			installer.createConfigurationFor(trigger.getClass());
		}
		catch (Exception e) {
			handleException(e);
			return null;
		}
		
		app.addTrigger(trigger, Optional.of(new Properties()));
		return trigger.getId();
	}
	
	@Override
	public void removeTrigger(String triggerId) {
		try {
			SmartTrigger trigger = app.removeTrigger(triggerId);
			installer.removeTrigger(trigger.getClass());
		} 
		catch (SmartTriggerException e) {
			handleException(e);
		}
	}
	
	@Override
	public String[] getTriggerIds() {
		return app.getTriggerIds();
	}
	
	@Override
	public String getLoadingDirectory() {
		return installer.getLoadingDirectory().toString();
	}
	
	@Override
	public String getTriggerState(String triggerId) {
		SmartTrigger trigger = null;
		try {
			trigger = app.getTrigger(triggerId);
		}
		catch (SmartTriggerException e) {
			handleException(e);
			return null;
		}
		return trigger.getState().name();
	}
	
	@Override
	public void setTriggerOffline(String triggerId) {
		SmartTrigger trigger = null;
		try {
			trigger = app.getTrigger(triggerId);
		}
		catch (SmartTriggerException e) {
			handleException(e);
			return;
		}
		trigger.setState(SmartTriggerStateType.OFFLINE);
	}

	public SmartTriggerApp getApp() {
		return app;
	}

	public void setApp(SmartTriggerApp app) {
		this.app = app;
	}

	public TriggerInstaller getInstaller() {
		return installer;
	}

	public void setInstaller(TriggerInstaller installer) {
		this.installer = installer;
	}
	
	private void handleException(Exception e) {
		// TODO: handle this
		System.err.println(e.getMessage());
		e.printStackTrace();
	}
}