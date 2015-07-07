package edu.berkeley.cspace.pictionbridge.filter;

import edu.berkeley.cspace.pictionbridge.update.Update;
import edu.berkeley.cspace.pictionbridge.update.UpdateAction;

/**
 * A filter that accepts updates that represent new images.
 */
public class NewUpdateFilter extends AbstractUpdateFilter implements UpdateFilter {

	@Override
	public boolean accept(Update update) {
		return (update.getAction() == UpdateAction.NEW);
	}
}
