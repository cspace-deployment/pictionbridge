package edu.berkeley.cspace.pictionbridge.filter;

import edu.berkeley.cspace.pictionbridge.PictionBridgeUtils;
import edu.berkeley.cspace.pictionbridge.update.Update;


/**
 * A filter that accepts updates that have a verified hash.
 */
public class HashFilter extends AbstractUpdateFilter implements UpdateFilter {

	@Override
	public boolean accept(Update update) {
		return PictionBridgeUtils.verifyHash(update);
	}
}
