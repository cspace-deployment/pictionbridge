package edu.berkeley.cspace.pictionbridge.processor;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.berkeley.cspace.pictionbridge.update.Update;

public class ChainedUpdateProcessor implements UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(ChainedUpdateProcessor.class);

	private List<UpdateProcessor> processors;
	
	@Override
	public List<Update> processUpdates(List<Update> updates) {
		logger.info(ChainedUpdateProcessor.class.getSimpleName() + " processing " + updates.size() + " updates with " + getProcessors().size() + " processors");

		for (UpdateProcessor processor : getProcessors()) {
			logger.info(ChainedUpdateProcessor.class.getSimpleName() + " processing " + updates.size() + " updates with " + processor.getClass().getSimpleName());
			
			updates = processor.processUpdates(updates);
		}
		
		return updates;
	}

	@Override
	public void close() {

	}

	public List<UpdateProcessor> getProcessors() {
		return processors;
	}

	public void setProcessors(List<UpdateProcessor> processors) {
		this.processors = processors;
	}
}
