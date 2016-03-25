package edu.berkeley.cspace.pictionbridge.processor;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.berkeley.cspace.pictionbridge.filter.UpdateFilter;
import edu.berkeley.cspace.pictionbridge.update.Update;

/**
 * An update processor that removes updates from the list using a filter.
 * The rejected updates may optionally be run through another processor.
 */
public class FilteringUpdateProcessor implements UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(FilteringUpdateProcessor.class);

	private UpdateFilter filter;
	private UpdateProcessor rejectedProcessor;
	
	@Override
	public void process(List<Update> updates) {
		logger.info(FilteringUpdateProcessor.class.getSimpleName() + " filtering " + updates.size() + " updates with " + getFilter().getClass().getSimpleName());
		
		List<Update> rejectedUpdates = getFilter().apply(updates);
		
		logger.info(getFilter().getClass().getSimpleName() + " rejected " + rejectedUpdates.size() + " updates, accepted " + updates.size() + " updates");
		
		if (getRejectedProcessor() != null && rejectedUpdates.size() > 0) {
			getRejectedProcessor().process(rejectedUpdates);
		}
	}

	@Override
	public void close() {

	}

	/**
	 * @return The filter to use.
	 */
	public UpdateFilter getFilter() {
		return filter;
	}

	/**
	 * Sets the filter to use.
	 * 
	 * @param filter The filter.
	 */
	public void setFilter(UpdateFilter filter) {
		this.filter = filter;
	}

	/**
	 * @return The processor to use on rejected updates.
	 */
	public UpdateProcessor getRejectedProcessor() {
		return rejectedProcessor;
	}

	/**
	 * Sets the processor to use on rejected updates.
	 * 
	 * @param rejectedProcessor The processor.
	 */
	public void setRejectedProcessor(UpdateProcessor rejectedProcessor) {
		this.rejectedProcessor = rejectedProcessor;
	}
}
