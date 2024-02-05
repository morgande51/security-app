package com.nge.smarttrigger.manager;

import java.util.Properties;

import com.nge.smarttrigger.spi.InitRequest;
import com.nge.smarttrigger.spi.SmartTrigger;

public class NewTriggerRequest extends InitRequest {

	private SmartTrigger trigger;
	
	public NewTriggerRequest(Properties configuration, String triggerInfo, SmartTrigger trigger) {
		super(configuration, triggerInfo);
		this.trigger = trigger;
	}


	public SmartTrigger getTrigger() {
		return trigger;
	}
}