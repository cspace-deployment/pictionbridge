package edu.berkeley.cspace.pictionbridge.filter;

import java.util.List;

import edu.berkeley.cspace.pictionbridge.update.Update;

public interface UpdateFilter {
	/**
	 * Filters a list of updates, removing any updates
	 * that are not accepted by the filter.
	 * 
	 * @param updates The updates to filter
	 * @return        The updates that were removed
	 *                (not accepted by the filter)
	 */
	public List<Update> apply(List<Update> updates);
	
	/**
	 * Tests if a given update should be accepted by the
	 * filter.
	 * 
	 * @param update The update to test
	 * @return       True, if the update passes the filter,
	 *               false otherwise
	 */
	public boolean accept(Update update);
}
