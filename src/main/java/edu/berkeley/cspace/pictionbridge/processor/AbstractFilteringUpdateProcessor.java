package edu.berkeley.cspace.pictionbridge.processor;

import java.util.List;

import edu.berkeley.cspace.pictionbridge.filter.UpdateFilter;
import edu.berkeley.cspace.pictionbridge.update.Update;

/**
 * An update processor that tests each update against a filter, and sends
 * accepted updates to one processor, and rejected updates to another. 
 *
 */
public abstract class AbstractFilteringUpdateProcessor implements UpdateProcessor {
	private UpdateFilter filter;
	private UpdateProcessor acceptedProcessor;
	private UpdateProcessor rejectedProcessor;
	
	@Override
	public abstract List<Update> processUpdates(List<Update> updates);

	@Override
	public void close() {

	}

	public UpdateFilter getFilter() {
		return filter;
	}

	public void setFilter(UpdateFilter filter) {
		this.filter = filter;
	}

	public UpdateProcessor getAcceptedProcessor() {
		return acceptedProcessor;
	}

	public void setAcceptedProcessor(UpdateProcessor acceptedProcessor) {
		this.acceptedProcessor = acceptedProcessor;
	}

	public UpdateProcessor getRejectedProcessor() {
		return rejectedProcessor;
	}

	public void setRejectedProcessor(UpdateProcessor rejectedProcessor) {
		this.rejectedProcessor = rejectedProcessor;
	}
}
