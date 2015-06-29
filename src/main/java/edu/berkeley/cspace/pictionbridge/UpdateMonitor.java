package edu.berkeley.cspace.pictionbridge;

import java.util.List;

public interface UpdateMonitor {
	public boolean hasUpdates();
	public int getUpdateCount();
	public List<Update> getUpdates();
	public List<Update> getUpdates(Integer limit);
	public void deleteUpdate(Update update);
}
