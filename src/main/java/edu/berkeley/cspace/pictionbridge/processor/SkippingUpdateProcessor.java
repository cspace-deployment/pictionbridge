package edu.berkeley.cspace.pictionbridge.processor;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.berkeley.cspace.pictionbridge.update.Update;

public class SkippingUpdateProcessor implements UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(SkippingUpdateProcessor.class);

	private String message;
	
	@Override
	public List<Update> processUpdates(List<Update> updates) {
		logger.info(SkippingUpdateProcessor.class.getSimpleName() + " processing " + updates.size() + " updates");
		
		for (Update update : updates) {
			logger.warn(SkippingUpdateProcessor.class.getSimpleName() + " skipping update " + update.getId() + (getMessage() != null ? ( ": " + getMessage()) : ""));
		}
		
		return updates;
	}

	@Override
	public void close() {

	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
