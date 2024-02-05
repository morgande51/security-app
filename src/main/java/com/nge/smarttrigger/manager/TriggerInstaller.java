package com.nge.smarttrigger.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import com.dme.agent.JarLoadingAgent;
import com.nge.smarttrigger.spi.SmartTrigger;
import com.nge.smarttrigger.spi.SmartTriggerException;

public class TriggerInstaller {
//	private static final Log
//	private static final Logger logger = LogManager.getLogManager().
	public static final String TRIGGER_DIR_PATH_PROPERTY = "com.dme.smarttrigger.lib";
	public static final String TRIGGER_CONFIG_PATH_PROPERTY = "com.dme.smarttrigger.config";
	public static final String TRIGGER_LOADING_DIR = "SmartTrigger/loading";
	
	private static final String SYSTEM_TEMP_DIR = "java.io.tmpdir";
	private static final String JAR_EXTENSION = ".jar";
	private static final String DOT = ".";
	private static final String CONFIG_SUFFIX = ".config";
	private static final String TRIGGER_INFO_FMT = "META-INF/%s.json";
	
	private static final TriggerInstaller SINGLETON = new TriggerInstaller();
	static {
		try {
			SINGLETON.init();
		}
		catch (IOException e) {
			// TODO: log this
			e.printStackTrace();
		}
	}
	
	private Path libDir;
	private Path loadingDir;
	private Path configDir;
	
	private TriggerInstaller() {}
	
	private void init() throws IOException {
		libDir = getDirectoryFor(System.getProperty(TRIGGER_DIR_PATH_PROPERTY));
		configDir = getDirectoryFor(System.getProperty(TRIGGER_CONFIG_PATH_PROPERTY));
		
		String tmpdir = System.getProperty(SYSTEM_TEMP_DIR);
		System.err.println("System Temp: " + tmpdir);
		loadingDir = getDirectoryFor(Paths.get(tmpdir, TRIGGER_LOADING_DIR), true);
		System.err.println("Loading from: " + loadingDir);
	}
	
	private Path getDirectoryFor(String dirPath) throws IOException {
		return getDirectoryFor(Paths.get(dirPath), false);
	}
	
	@SuppressWarnings("unchecked")
	public NewTriggerRequest loadNewTrigger(String triggerFQN) throws IOException {
		String jarFileName = triggerFQN.substring(triggerFQN.lastIndexOf(DOT) + 1) + JAR_EXTENSION;
		Path jarPath = loadingDir.resolve(jarFileName);
		if (!Files.exists(jarPath)) 
		{
			/*
			System.out.println("Path: " + jarPath);
			System.out.println("Exist: " + Files.exists(jarPath));
			System.out.println("isDir: " + Files.isDirectory(jarPath));
			System.out.println("Readable: " + Files.isReadable(jarPath));
			System.out.println("Can Write: " + Files.isWritable(jarPath));
			*/
			throw new IOException(jarPath + " does not exist or is not readable/writeable");
		}
		
		// load the new trigger from the fileSystem
		JarLoadingAgent.addToClassPath(jarPath.toFile());
		Class<SmartTrigger> triggerClass = (Class<SmartTrigger>) getClassFor(triggerFQN);
		SmartTrigger trigger;
		try {
			trigger = triggerClass.getDeclaredConstructor().newInstance();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		// load info about trigger from jar file
		String infoFileName = String.format(TRIGGER_INFO_FMT, triggerClass.getName());
		String info = getTriggerInfo(jarPath, infoFileName);
		
		// create an empty Properties file for the new trigger
		Properties config = createConfigurationFor(trigger.getClass());
					
		// copy the jar file over to the library and remove the old jar
		Path libJarPath = libDir.resolve(jarFileName);
		Files.copy(jarPath, libJarPath);
		Files.delete(jarPath);
		
		return new NewTriggerRequest(config, info, trigger);
	}
	
	public void removeTrigger(Class<?> triggerClass) throws SmartTriggerException {
		try {
			Path jarFile = libDir.resolve(triggerClass.getSimpleName() + JAR_EXTENSION);
			Path configFile = configDir.resolve(triggerClass.getName() + CONFIG_SUFFIX);
			Files.delete(jarFile);
			Files.delete(configFile);
		}
		catch (IOException e) {
			throw new SmartTriggerException(e);
		}
	}
	
	public Path getLoadingDirectory() {
		return loadingDir;
	}
	
	public <T extends SmartTrigger> Properties getConfiguration(Class<T> triggerClass) throws IOException {
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
	
	public <T extends SmartTrigger> void saveConfiguration(Class<T> triggerClass, Properties config) throws IOException {
		String fileName = triggerClass.getName() + CONFIG_SUFFIX;
		File configFile = configDir.resolve(fileName).toFile();
		try (FileOutputStream stream = new FileOutputStream(configFile)) {
			config.store(stream, "Last updated: " + LocalDateTime.now().toString());
		}
	}
	
	public <T extends SmartTrigger> String getTriggerInfo(Class<T> triggerClass) throws IOException {
		Path jarFile = getJarFor(triggerClass);
		String fileName = String.format(TRIGGER_INFO_FMT, triggerClass.getName());
		return getTriggerInfo(jarFile, fileName);
	}
	
	private Path getJarFor(Class<?> triggerClass) {
		CodeSource src = triggerClass.getProtectionDomain().getCodeSource();
		String jarLoc = src.getLocation().getFile();
		return Paths.get(jarLoc);
	}
	
	private <T extends SmartTrigger> Properties createConfigurationFor(Class<T> triggerClass) throws IOException {
		String fileName = triggerClass.getName() + CONFIG_SUFFIX;
		Path configFile = configDir.resolve(fileName);
		Files.createFile(configFile);
		return new Properties();
	}
	
	private Class<?> getClassFor(String triggerClassName) {
//		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		Class<?> triggerClass;
		try {
			triggerClass = cl.loadClass(triggerClassName);
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		return triggerClass;
	}
	
	private String getTriggerInfo(Path jarPath, String fileName) throws IOException {
		System.err.println("going for fileName: " + fileName);
		JarFile jf = new JarFile(jarPath.toFile());
		ZipEntry infoEntry = jf.getEntry(fileName);
		return getTriggerInfo(jf.getInputStream(infoEntry));
	}
	
	public String getTriggerInfo(InputStream stream) throws IOException {
		StringBuffer sb = new StringBuffer();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		}
		return sb.toString();
	}
	
	private Path getDirectoryFor(Path path, boolean create) throws IOException {
		try {
			if (!Files.exists(path) && 
					!Files.isDirectory(path) &&
					!Files.isReadable(path) && 
					!Files.isWritable(path))
			{
				//TODO: Log these states
				/*
				System.out.println("Path: " + path);
				System.out.println("Exist: " + Files.exists(dir));
				System.out.println("isDir: " + Files.isDirectory(dir));
				System.out.println("Readable: " + Files.isReadable(dir));
				System.out.println("Can Write: " + Files.isWritable(dir));
				*/
				throw new IOException("Directory: " + path + " does not exist or is not readable/writable");
			}
		}
		catch (IOException e) {
			if (create && !Files.exists(path)) {
				Files.createDirectories(path);
			}
			else {
				throw e;
			}
		}
		return path;
	}
	
	public String getTriggerInfoFileName(Class<? extends SmartTrigger> triggerClass) {
		return String.format(TRIGGER_INFO_FMT, triggerClass.getName());
	}
	
	public static TriggerInstaller getInstaller() {
		return SINGLETON;
	}
}