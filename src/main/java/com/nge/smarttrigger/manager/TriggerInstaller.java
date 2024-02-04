package com.nge.smarttrigger.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.time.LocalDateTime;
import java.util.Properties;

import com.dme.agent.JarLoadingAgent;
import com.nge.smarttrigger.spi.SmartTrigger;
import com.nge.smarttrigger.spi.SmartTriggerException;

public class TriggerInstaller {
	
	public static final String TRIGGER_DIR_PATH_PROPERTY = "com.dme.smarttrigger.lib";
	public static final String TRIGGER_LOADING_DIR_PROPERTY = "com.dme.smarttrigger.loading";
	public static final String TRIGGER_CONFIG_PATH_PROPERTY = "com.dme.smarttrigger.config";
	
	private static final String JAR_EXTENSION = ".jar";
	private static final String DOT = ".";
	private static final String CONFIG_SUFFIX = ".config";
	
	private static final TriggerInstaller SINGLETON = new TriggerInstaller();
	
	private Path libDir;
	private Path loadingDir;
	private Path configDir;
	
	private TriggerInstaller() {
		libDir = getPathFor(System.getProperty(TRIGGER_DIR_PATH_PROPERTY));
		
		//TODO: refactor this to use the tmp directory
		loadingDir = getPathFor(System.getProperty(TRIGGER_LOADING_DIR_PROPERTY));
		configDir = getPathFor(System.getProperty(TRIGGER_CONFIG_PATH_PROPERTY));
	}
	
	private Path getPathFor(String dirPath) {
		Path path = Paths.get(dirPath);
		if (!Files.exists(path) && 
				!Files.isDirectory(path) &&
				!Files.isReadable(path) && 
				!Files.isWritable(path))
		{
			/*
			 * TODO: Log these states
			System.out.println("Path: " + path);
			System.out.println("Exist: " + Files.exists(dir));
			System.out.println("isDir: " + Files.isDirectory(dir));
			System.out.println("Readable: " + Files.isReadable(dir));
			System.out.println("Can Write: " + Files.isWritable(dir));
			*/
			throw new RuntimeException("Directory: " + dirPath + " does not exist or is not readable/writable");
		}
		return path;
	}
	
	@SuppressWarnings("unchecked")
	public SmartTrigger loadTrigger(String triggerFQN) throws SmartTriggerException {
		String jarFileName = triggerFQN.substring(triggerFQN.lastIndexOf(DOT) + 1) + JAR_EXTENSION;
		Path jarPath = loadingDir.resolve(jarFileName);
		if (!Files.exists(jarPath) ||
			!Files.isDirectory(jarPath) && (
					!Files.isWritable(jarPath) || 
					!Files.isReadable(jarPath))) 
		{
			System.out.println("Path: " + jarPath);
			System.out.println("Exist: " + Files.exists(jarPath));
			System.out.println("isDir: " + Files.isDirectory(jarPath));
			System.out.println("Readable: " + Files.isReadable(jarPath));
			System.out.println("Can Write: " + Files.isWritable(jarPath));
			throw new SmartTriggerException(jarPath + " does not exist or is not readable/writeable");
		}
		
		SmartTrigger trigger;
		try {
			JarLoadingAgent.addToClassPath(jarPath.toFile());
			Class<SmartTrigger> triggerClass = (Class<SmartTrigger>) getClassFor(triggerFQN);
			trigger = triggerClass.getDeclaredConstructor().newInstance();
		}
		catch (Exception e) {
			throw new SmartTriggerException(e);
		}
		
		// copy the jar file over to the library
		Path libJarPath = libDir.resolve(jarFileName);
		try {
			Files.copy(jarPath, libJarPath);
			Files.delete(jarPath);
		}
		catch (IOException e) {
			throw new SmartTriggerException(e);
		}		
		
		// return the trigger;
		return trigger;
	}
	
	public void removeTrigger(String triggerClassName) throws SmartTriggerException {
		removeTrigger(getClassFor(triggerClassName));
	}
	
	public void removeTrigger(Class<?> triggerClass) throws SmartTriggerException {
		try {
			Path jarFile = libDir.resolve(triggerClass.getSimpleName() + JAR_EXTENSION);
			Files.delete(jarFile);
		}
		catch (IOException e) {
			throw new SmartTriggerException(e);
		}
	}
	
	public Path getLoadingDirectory() {
		return loadingDir;
	}
	
	public <T extends SmartTrigger> Properties getConfigurationFor(Class<T> triggerClass) throws IOException {
		String fileName = triggerClass.getName() + CONFIG_SUFFIX;
		File configFile = configDir.resolve(fileName).toFile();
		
		Properties config = new Properties();
		if (configFile.exists() && configFile.canRead()) {
			try (FileInputStream stream = new FileInputStream(configFile)) {
				config.load(stream);
			}
		}
		else if (!configFile.exists()) {
			Files.list(configDir).forEach((p) -> System.err.println(p));
			throw new IOException(configFile.getPath() + " exist: " + configFile.exists());
		}
		else {
			throw new IOException(configFile.getPath() + " can read: " + configFile.canRead());
		}
		
		return config;
	}
	
	public <T extends SmartTrigger> void createConfigurationFor(Class<T> triggerClass) throws IOException {
		String fileName = triggerClass.getName() + CONFIG_SUFFIX;
		Path configFile = configDir.resolve(fileName);
		Files.createFile(configFile);
	}
	
	public <T extends SmartTrigger> void saveConfiguration(Class<T> triggerClass, Properties config) throws IOException {
		String fileName = triggerClass.getName() + CONFIG_SUFFIX;
		File configFile = configDir.resolve(fileName).toFile();
		try (FileOutputStream stream = new FileOutputStream(configFile)) {
			config.store(stream, "Last updated: " + LocalDateTime.now().toString());
		}
	}
	
	protected Path getJarFor(Class<?> triggerClass) throws SmartTriggerException {
		CodeSource src = triggerClass.getProtectionDomain().getCodeSource();
		String jarLoc = src.getLocation().getFile();
		Path jar = Paths.get(jarLoc);
		if (Files.isDirectory(jar) || !Files.isWritable(jar)) {
			throw new SmartTriggerException(jarLoc + " is not a file or is not writable");
		}
		return jar;
	}
	
	private Class<?> getClassFor(String triggerClassName) throws SmartTriggerException {
//		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		Class<?> triggerClass;
		try {
			triggerClass = cl.loadClass(triggerClassName);
			if (!SmartTrigger.class.isAssignableFrom(triggerClass)) {
				throw new SmartTriggerException(triggerClassName + " does not implement SmartTrigger");
			}
		}
		catch (ClassNotFoundException e) {
			throw new SmartTriggerException(e);
		}
		return triggerClass;
	}
	
	public static TriggerInstaller getInstaller() {
		return SINGLETON;
	}
}