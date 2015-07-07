package edu.berkeley.cspace.pictionbridge;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.berkeley.cspace.pictionbridge.monitor.UpdateMonitor;
import edu.berkeley.cspace.pictionbridge.processor.UpdateProcessor;
import edu.berkeley.cspace.pictionbridge.update.Update;

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

			UpdateMonitor updateMonitor = context.getBean("updateMonitor", UpdateMonitor.class);
			UpdateProcessor updateProcessor = context.getBean("updateProcessor", UpdateProcessor.class);

			int totalCount = updateMonitor.getUpdateCount();			
			List<Update> updates = updateMonitor.getUpdates();
			
			logger.info("found " + updates.size() + " updates" + (updateMonitor.getLimit() != null ? " (total " + totalCount + ", limited to " + updateMonitor.getLimit() + ")" : ""));
			
			List<Update> processedUpdates = updateProcessor.processUpdates(updates);
			
			logger.info("processed " + processedUpdates.size() + " updates");
			logger.info("exiting");
		}
		catch(Exception e) {
			logger.fatal("terminated with exception", e);
		}
	}
}