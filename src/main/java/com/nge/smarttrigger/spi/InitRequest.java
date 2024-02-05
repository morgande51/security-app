package com.nge.smarttrigger.spi;

import java.util.Properties;
import java.util.UUID;

public class InitRequest {
	
	private Properties configuration;
	
	private String triggerInfo;
	
	private String id;

	public InitRequest(Properties configuration, String triggerInfo) {
		super();
		this.configuration = configuration;
		this.triggerInfo = triggerInfo;
		id = UUID.randomUUID().toString();
	}
	
	public Properties getConfiguration() {
		return configuration;
	}

	public String getTriggerInfo() {
		return triggerInfo;
	}
	
	public String getId() {
		return id;
	}
}