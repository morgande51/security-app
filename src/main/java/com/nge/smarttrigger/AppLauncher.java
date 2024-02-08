package com.nge.smarttrigger;

import java.util.concurrent.Executors;

public class AppLauncher {

	public static void main(String[] args) {
		String classpath = System.getProperty("java.class.path");
		String[] classPathValues = classpath.split(java.io.File.pathSeparator);
	    for (String classPath: classPathValues) {
	      System.out.println(classPath);
	    }
		SmartTriggerApp app = SmartTriggerApp.getApp();
		Executors.newSingleThreadExecutor().execute(app);
	}
}