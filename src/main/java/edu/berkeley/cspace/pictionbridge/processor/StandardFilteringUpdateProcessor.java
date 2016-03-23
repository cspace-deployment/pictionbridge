package edu.berkeley.cspace.pictionbridge.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.berkeley.cspace.pictionbridge.update.Update;

/**
 * A filtering update processor that tests each update, and immediately
 * processes that update with the appropriate processor. Consequently,
 * each update is processed in the order in which it was received.
 * 
 * Use BatchedFilteringUpdateProcessor if you want to filter first,
 * then process all rejected updates as a batch, and all accepted
 * updates as a separate batch.
 *
 */
public class StandardFilteringUpdateProcessor extends AbstractFilteringUpdateProcessor implements UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(StandardFilteringUpdateProcessor.class);

	@Override
	public List<Update> processUpdates(List<Update> updates) {
		logger.info(StandardFilteringUpdateProcessor.class.getSimpleName() + " filtering " + updates.size() + " updates with " + getFilter().getClass().getSimpleName());
		
		List<Update> processedUpdates = new ArrayList<Update>();
		
		for (Update update : updates) {
			UpdateProcessor updateProcessor = getFilter().accept(update) ? getAcceptedProcessor() : getRejectedProcessor();
			
			if (updateProcessor != null) {
				processedUpdates.addAll(updateProcessor.processUpdates(Arrays.asList(update)));
			}
		}
		
		return processedUpdates;
	}
}
