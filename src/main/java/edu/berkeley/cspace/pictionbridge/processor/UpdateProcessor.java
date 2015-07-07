package edu.berkeley.cspace.pictionbridge.processor;

import java.util.List;

import edu.berkeley.cspace.pictionbridge.update.Update;

public interface UpdateProcessor {
	public List<Update> processUpdates(List<Update> updates);
	public void close();
}
