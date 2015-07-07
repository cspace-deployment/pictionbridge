package edu.berkeley.cspace.pictionbridge.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.berkeley.cspace.pictionbridge.update.Update;

/**
 * A filtering update processor that filters the updates,
 * then processes all rejected updates as a batch, and all accepted
 * updates as a separate batch. Consequently, updates may not be
 * processed in the order in which they were received.
 *
 * Use StandardFilteringUpdateProcessor if you want to process
 * each update immediately after it is tested against the filter.
 * 
 */
public class BatchedFilteringUpdateProcessor extends AbstractFilteringUpdateProcessor implements UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(BatchedFilteringUpdateProcessor.class);

	private boolean processRejectedBeforeAccepted = false;
	
	@Override
	public List<Update> processUpdates(List<Update> updates) {
		logger.info(BatchedFilteringUpdateProcessor.class.getSimpleName() + " processing " + updates.size() + " updates with filter " + getFilter().getClass().getSimpleName());
		
		List<Update> rejectedUpdates = getFilter().apply(updates);
		List<Update> processedUpdates = new ArrayList<Update>();
		
		logger.info(getFilter().getClass().getSimpleName() + " rejected " + rejectedUpdates.size() + " updates, accepted " + updates.size() + " updates");
		
		if (isProcessRejectedBeforeAccepted()) {
			if (rejectedUpdates.size() > 0) {
				processedUpdates.addAll(getRejectedProcessor().processUpdates(rejectedUpdates));
			}
			
			if (updates.size() > 0) {
				processedUpdates.addAll(getAcceptedProcessor().processUpdates(updates));	
			}
		}
		else {
			if (updates.size() > 0) {
				processedUpdates.addAll(getAcceptedProcessor().processUpdates(updates));	
			}

			if (rejectedUpdates.size() > 0) {
				processedUpdates.addAll(getRejectedProcessor().processUpdates(rejectedUpdates));
			}
		}
		
		return processedUpdates;
	}

	public boolean isProcessRejectedBeforeAccepted() {
		return processRejectedBeforeAccepted;
	}

	public void setProcessRejectedBeforeAccepted(boolean processRejectedBeforeAccepted) {
		this.processRejectedBeforeAccepted = processRejectedBeforeAccepted;
	}
}
