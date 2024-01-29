package com.nge.smarttrigger.spi;

public class SmartTriggerException extends Exception {
	
	public SmartTriggerException(String message, Throwable cause) {
		super(message, cause);
	}

	public SmartTriggerException(String message) {
		super(message);
	}

	public SmartTriggerException(Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = 1L;
}