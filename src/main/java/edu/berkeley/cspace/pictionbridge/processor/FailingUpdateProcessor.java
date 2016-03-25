package edu.berkeley.cspace.pictionbridge.processor;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.berkeley.cspace.pictionbridge.update.Update;

/**
 * An update processor that logs an error message for each update in the list,
 * and clears the list.
 */
public class FailingUpdateProcessor implements UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(FailingUpdateProcessor.class);

	private String message;
	
	@Override
	public void process(List<Update> updates) {
		logger.info(FailingUpdateProcessor.class.getSimpleName() + " processing " + updates.size() + " updates");
		
		for (Update update : updates) {
			logger.error(FailingUpdateProcessor.class.getSimpleName() + " failing update " + update.getId() + (getMessage() != null ? ( ": " + getMessage()) : ""));
		}
		
		updates.clear();
	}

	@Override
	public void close() {

	}

	/**
	 * @return The error message to log for each update.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Sets the error message to log for each update.
	 * 
	 * @param message The message.
	 */
	public void setMessage(String message) {
		this.message = message;
	}
}
