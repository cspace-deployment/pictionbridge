package edu.berkeley.cspace.pictionbridge.processor;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.berkeley.cspace.pictionbridge.update.Update;

/**
 * An update processor composed of other update processors. Each processor is
 * executed in order on the list of updates.
 */
public class CompositeUpdateProcessor implements UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(CompositeUpdateProcessor.class);

	private List<UpdateProcessor> processors;
	
	@Override
	public void process(List<Update> updates) {
		logger.info(CompositeUpdateProcessor.class.getSimpleName() + " processing " + updates.size() + " updates with " + getProcessors().size() + " processors");

		int count = 0;
		
		for (UpdateProcessor processor : getProcessors()) {
			count++;
			
			logger.info(CompositeUpdateProcessor.class.getSimpleName() + " processing " + updates.size() + " updates with processor " + count + " - " + processor.getClass().getSimpleName());
			
			processor.process(updates);
		}
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
