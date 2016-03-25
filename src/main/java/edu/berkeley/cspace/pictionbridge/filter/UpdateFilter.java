package edu.berkeley.cspace.pictionbridge.filter;

import java.util.List;

import edu.berkeley.cspace.pictionbridge.update.Update;


/**
 * A filter that removes unacceptable updates from a list.
 *
 */
public interface UpdateFilter {
	/**
	 * Applies the filter to a list of updates.
	 * 
	 * @param updates The updates to filter. Updates that are not
	 *                accepted by this filter's test will be
	 *                removed from the list.
	 * @return        The updates that were removed (not
	 *                accepted by this filter's test).
	 */
	public List<Update> apply(List<Update> updates);
	
	/**
	 * Tests if an update should be accepted by the filter.
	 * 
	 * @param update The update to test.
	 * @return       True if the update should be accepted by the
	 *               filter, false otherwise.
	 */
	public boolean accept(Update update);
}
