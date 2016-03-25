package edu.berkeley.cspace.pictionbridge.processor;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.berkeley.cspace.pictionbridge.update.Update;

/**
 * An update processor that logs an info message for each update in the list.
 */
public class LoggingUpdateProcessor implements UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(LoggingUpdateProcessor.class);

	private String message;
	
	@Override
	public void process(List<Update> updates) {
		logger.info(LoggingUpdateProcessor.class.getSimpleName() + " processing " + updates.size() + " updates");
		
		for (Update update : updates) {
			logger.info(LoggingUpdateProcessor.class.getSimpleName() + " logging update " + update.getId() + (getMessage() != null ? ( ": " + getMessage()) : ""));
		}
	}

	@Override
	public void close() {

	}

	/**
	 * @return The info message to log for each update.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Sets the info message to log for each update.
	 * 
	 * @param message The message.
	 */
	public void setMessage(String message) {
		this.message = message;
	}
}
