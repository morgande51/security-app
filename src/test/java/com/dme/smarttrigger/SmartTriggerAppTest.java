package com.dme.smarttrigger;

import java.util.concurrent.Executors;

import com.nge.smarttrigger.SmartTriggerApp;

public class SmartTriggerAppTest {

	public static void main(String[] args) {
		SmartTriggerApp app = SmartTriggerApp.getApp();
		Executors.newSingleThreadExecutor().execute(app);
	}
}
