package edu.berkeley.cspace.pictionbridge.monitor;

import java.util.List;

import edu.berkeley.cspace.pictionbridge.update.Update;

public interface UpdateMonitor {
	public boolean hasUpdates();
	public int getUpdateCount();
	
	public List<Update> getUpdates(String defaultRelationshipValue);
	public void markUpdateComplete(Update update);
	public void setObjectCSID(Update update);
	public void deleteBinary(Update update);
	public void deleteUpdate(Update update);
	public void logUpdate(Update update);
	
	public Integer getLimit();
	public void setLimit(Integer limit);

	public String getDefaultRelationshipValue();
	public void setDefaultRelationshipValue(String defaultRelationshipValue);
}
