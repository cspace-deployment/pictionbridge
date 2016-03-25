package edu.berkeley.cspace.pictionbridge.processor;

import java.util.List;

import edu.berkeley.cspace.pictionbridge.update.Update;

/**
 * An update processor consumes a list of updates, and performs actions.
 * Actions could include modifying updates in the list, removing
 * updates from the the list, and adding updates to the list.
 */
public interface UpdateProcessor {
	
	/**
	 * Processes a list of updates. 
	 * 
	 * @param updates The updates to process.
	 */
	public void process(List<Update> updates);
	
	/**
	 * Releases system resources used by this processor.
	 */
	public void close();
}
