package com.nge.smarttrigger.manager;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nge.smarttrigger.SmartTriggerApp;
import com.nge.smarttrigger.spi.SmartTrigger;
import com.nge.smarttrigger.spi.SmartTriggerException;
import com.nge.smarttrigger.spi.SmartTriggerStateType;

public class TriggerManagerImpl implements TriggerManagerMXBean {
	
	private static final Logger logger = LoggerFactory.getLogger(TriggerManagerImpl.class);
	
	private ObjectName name;
	
	public TriggerManagerImpl() {};
	
	public TriggerManagerImpl(ObjectName name) {
		this.name = name;
	}
	
	@Override
	public ObjectName getObjectName() {
		return name;
	}
	
	@Override
	public String installTrigger(String triggerFQN) {
		String triggerId;
		TriggerInstaller installer = TriggerInstaller.getInstaller();
		try {
			NewTriggerRequest request = installer.loadNewTrigger(triggerFQN);
			SmartTrigger trigger = request.getTrigger();
			SmartTriggerApp.getApp().addTrigger(trigger, request);
			
			Properties requestConfig = request.getConfiguration();
			Properties triggerConfig = trigger.getProperties();
			logger.debug("Request Config is null: {}", (requestConfig == null));
			logger.debug("Trigger Config is null: {}", (triggerConfig == null));
			logger.debug("Request Config = Trigger Config: {}", (requestConfig == triggerConfig));
			logger.debug("Request Config equals Trigger Config: {}", (requestConfig.equals(triggerConfig)));
			Properties config = trigger.getProperties();
			
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
	public void deleteTrigger(String triggerId) {
		try {
			SmartTriggerApp.getApp().deleteTrigger(triggerId);
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
	public SmartTriggerStateType getTriggerState(String triggerId) {
		SmartTrigger trigger = null;
		try {
			trigger = SmartTriggerApp.getApp().getTrigger(triggerId);
		}
		catch (SmartTriggerException e) {
			handleException(e);
			return null;
		}
		return trigger.getState();
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
	
	@Override
	public String getTriggerInfo(String triggerId) throws SmartTriggerException {
		SmartTrigger trigger = SmartTriggerApp.getApp().getTrigger(triggerId);
		return trigger.getInfo();
	}
	
	@Override
	public Set<SimpleKeyValue> getTriggerConfig(String triggerId) throws SmartTriggerException {
		SmartTrigger trigger = SmartTriggerApp.getApp().getTrigger(triggerId);
		return trigger.getProperties().entrySet().stream().map(e -> SimpleKeyValue.buildFrom(e)).collect(Collectors.toSet());
	}
	
	@Override
	public void updateTriggerConfig(String triggerId, String name, String value) throws SmartTriggerException {
		SmartTriggerApp app = SmartTriggerApp.getApp();
		SmartTrigger trigger = app.removeTrigger(triggerId);
		trigger.updateProperty(name, value);
		try {
			TriggerInstaller.getInstaller().saveConfiguration(trigger.getClass(), trigger.getProperties());
		}
		catch (IOException e) {
			handleException(e);
		}
		
		app.restartTrigger(trigger);
	}
	
	@Override
	public void updateTriggerConfig(String triggerId, Set<SimpleKeyValue> properties) throws SmartTriggerException {
		properties.forEach(p -> logger.debug("Prop: {} - val: {}", p.getKey(), p.getValue()));
		SmartTriggerApp app = SmartTriggerApp.getApp();
		SmartTrigger trigger = app.removeTrigger(triggerId);
		properties.forEach(p -> trigger.updateProperty(p.getKey(), p.getValue()));
		try {
			TriggerInstaller.getInstaller().saveConfiguration(trigger.getClass(), trigger.getProperties());
		}
		catch (IOException e) {
			handleException(e);
		}
		
		app.restartTrigger(trigger);
	}
	
	@Override
	public SmartTriggerStateType restartTrigger(String triggerId) throws SmartTriggerException {
		SmartTriggerApp app = SmartTriggerApp.getApp();
		SmartTrigger trigger = app.getTrigger(triggerId);
		app.restartTrigger(trigger);
		return trigger.getState();
	}
	
	@Override
	public String getError(String triggerId) throws SmartTriggerException {
		SmartTrigger trigger = SmartTriggerApp.getApp().getTrigger(triggerId);
		StringWriter writer = new StringWriter();
		PrintWriter pw = new PrintWriter(writer);
		trigger.getRuntimeException().printStackTrace(pw);
		return writer.toString();
	}
	
	private void handleException(Exception e) {
		// TODO: handle this
		logger.error(e.getMessage(), e);
	}
}