package edu.berkeley.cspace.piction;

import java.util.List;

public interface UpdateMonitor {
	public boolean hasUpdates();
	public int getUpdateCount();
	public List<PictionUpdate> getUpdates();
	public List<PictionUpdate> getUpdates(Integer limit);
	public void deleteUpdate(PictionUpdate update);
}
