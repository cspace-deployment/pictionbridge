package edu.berkeley.cspace.pictionbridge.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.berkeley.cspace.pictionbridge.update.Update;

public class FailingUpdateProcessor implements UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(FailingUpdateProcessor.class);

	private String message;
	
	@Override
	public List<Update> processUpdates(List<Update> updates) {
		logger.info(FailingUpdateProcessor.class.getSimpleName() + " processing " + updates.size() + " updates");
		
		for (Update update : updates) {
			logger.error(FailingUpdateProcessor.class.getSimpleName() + " failing update " + update.getId() + (getMessage() != null ? ( ": " + getMessage()) : ""));
		}
		
		return new ArrayList<Update>();
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
