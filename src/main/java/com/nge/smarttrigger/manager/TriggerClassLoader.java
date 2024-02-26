package com.nge.smarttrigger.manager;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TriggerClassLoader extends ClassLoader {
	
	private static final Logger logger = LoggerFactory.getLogger(TriggerClassLoader.class);
	
	private static final char DOT = '.';
	private static final char SLASH  = '/';
	private static final int DEFAULT_OFFSET = 0;
	private static final String DOT_CLASS = ".class";
	
	public TriggerClassLoader(ClassLoader parent) {
		super(parent);
	}

	public Class<?> loadTrigger(JarFile jf, String triggerFQDN) throws ClassNotFoundException, IOException {
		String fileName = triggerFQDN.replace(DOT, SLASH).concat(DOT_CLASS);
		logger.info("Attemping to load: {} from {}", fileName, jf.getName());
		JarEntry entry = jf.getJarEntry(fileName);
		Class<?> triggerClass;
		try (InputStream is = jf.getInputStream(entry)) {
			byte[] contents = is.readAllBytes();
			triggerClass = defineClass(triggerFQDN, contents, DEFAULT_OFFSET, contents.length);
			logger.info("Loaded {}", triggerClass.getName());
		}
		return triggerClass;
 	}
}