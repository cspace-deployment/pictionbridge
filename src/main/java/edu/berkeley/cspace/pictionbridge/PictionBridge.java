package edu.berkeley.cspace.pictionbridge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class PictionBridge {
	private static final Logger logger = LogManager.getLogger(PictionBridge.class);
	
	public static final String CONFIG_FILE_PROPERTY = "pictionBridge.configurationFile";
	
	public static void main(String[] args) {
		String configFile = System.getProperty(CONFIG_FILE_PROPERTY);
		
		if (configFile == null || configFile.equals("")) {
			logger.fatal("required property " + CONFIG_FILE_PROPERTY + " is not set");
			return;
		}
				
		try (FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext("file:" + configFile)) {
			logger.info("starting up with configuration file " + configFile);
			
			UpdateProcessor updateProcessor = context.getBean("updateProcessor", UpdateProcessor.class);
			int count = updateProcessor.processUpdates();
			
			logger.info(count + " updates processed");
		}
		catch(Exception e) {
			logger.fatal("terminated with exception", e);
		}
	}
}