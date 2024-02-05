package com.nge.smarttrigger.manager;

import java.io.IOException;
import java.util.Properties;

import com.nge.smarttrigger.SmartTriggerApp;
import com.nge.smarttrigger.spi.SmartTrigger;
import com.nge.smarttrigger.spi.SmartTriggerException;
import com.nge.smarttrigger.spi.SmartTriggerStateType;

public class TriggerManagerImpl implements TriggerManagerMXBean {
	
	@Override
	public String installTrigger(String triggerFQN) {
		String triggerId;
		TriggerInstaller installer = TriggerInstaller.getInstaller();
		try {
			NewTriggerRequest request = installer.loadNewTrigger(triggerFQN);
			SmartTrigger trigger = request.getTrigger();
			Properties config = trigger.getProperties();
			SmartTriggerApp.getApp().addTrigger(trigger, request);
			installer.saveConfiguration(trigger.getClass(), config);
			triggerId = request.getId();
		}
		catch (IOException e) {
			handleException(e);
			return null;
		}
		catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
		
		return triggerId;
	}
	
	@Override
	public void removeTrigger(String triggerId) {
		try {
			SmartTrigger trigger = SmartTriggerApp.getApp().removeTrigger(triggerId);
			TriggerInstaller.getInstaller().removeTrigger(trigger.getClass());
		} 
		catch (SmartTriggerException e) {
			handleException(e);
		}
	}
	
	@Override
	public String[] getTriggerIds() {
		return SmartTriggerApp.getApp().getTriggerIds();
	}
	
	@Override
	public String getLoadingDirectory() {
		return TriggerInstaller.getInstaller().getLoadingDirectory().toString();
	}
	
	@Override
	public String getTriggerState(String triggerId) {
		SmartTrigger trigger = null;
		try {
			trigger = SmartTriggerApp.getApp().getTrigger(triggerId);
		}
		catch (SmartTriggerException e) {
			handleException(e);
			return null;
		}
		return trigger.getState().name();
	}
	
	@Override
	public boolean makeTriggerOffline(String triggerId) {
		boolean success;
		try {
			SmartTrigger trigger = SmartTriggerApp.getApp().getTrigger(triggerId);
			trigger.setState(SmartTriggerStateType.OFFLINE);
			success = true;
		}
		catch (SmartTriggerException e) {
			handleException(e);
			success = false;
		}
		return success;
	}
	
	@Override
	public boolean makeTriggerOnline(String triggerId) {
		boolean success;
		try {
			SmartTrigger trigger = SmartTriggerApp.getApp().getTrigger(triggerId);
			trigger.setState(SmartTriggerStateType.RUNNING);
			success = true;
		}
		catch (SmartTriggerException e) {
			handleException(e);
			success = false;
		}
		return success;
	}
	
	private void handleException(Exception e) {
		// TODO: handle this
		System.err.println(e.getMessage());
		e.printStackTrace();
	}
}