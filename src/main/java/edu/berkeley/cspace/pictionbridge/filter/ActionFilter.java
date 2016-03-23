package edu.berkeley.cspace.pictionbridge.filter;

import java.util.List;

import edu.berkeley.cspace.pictionbridge.update.Update;
import edu.berkeley.cspace.pictionbridge.update.UpdateAction;

/**
 * A filter that accepts updates with specified actions.
 */
public class ActionFilter extends AbstractUpdateFilter implements UpdateFilter {

	private List<UpdateAction> acceptActions;
	
	@Override
	public boolean accept(Update update) {
		return (acceptActions.contains(update.getAction()));
	}

	public List<UpdateAction> getAcceptActions() {
		return acceptActions;
	}

	public void setAcceptActions(List<UpdateAction> acceptActions) {
		this.acceptActions = acceptActions;
	}
}
