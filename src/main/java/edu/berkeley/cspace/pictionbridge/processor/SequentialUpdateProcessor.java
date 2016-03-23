package edu.berkeley.cspace.pictionbridge.processor;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.berkeley.cspace.pictionbridge.update.Update;

public class SequentialUpdateProcessor implements UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(SequentialUpdateProcessor.class);

	private List<UpdateProcessor> processors;
	
	@Override
	public List<Update> processUpdates(List<Update> updates) {
		logger.info(SequentialUpdateProcessor.class.getSimpleName() + " processing " + updates.size() + " updates with " + getProcessors().size() + " processors");

		int count = 0;
		
		for (UpdateProcessor processor : getProcessors()) {
			count++;
			
			logger.info(SequentialUpdateProcessor.class.getSimpleName() + " processing " + updates.size() + " updates with processor " + count + " - " + processor.getClass().getSimpleName());
			
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
