package edu.berkeley.cspace.pictionbridge.filter;

import java.sql.Timestamp;

import edu.berkeley.cspace.pictionbridge.update.Update;

/**
 * A filter that accepts updates that were added to Piction after a given datetime.
 */
public class DateAddedFilter extends AbstractUpdateFilter implements UpdateFilter {

	private Timestamp cutoffDate;
	
	@Override
	public boolean accept(Update update) {
		Timestamp dateAddedToPiction = update.getDateTimeAddedToPiction();
		
		return dateAddedToPiction.after(cutoffDate);
	}

	public String getCutoffDate() {
		return cutoffDate.toString();
	}

	public void setCutoffDate(String cutoffDate) {
		this.cutoffDate = Timestamp.valueOf(cutoffDate);
	}
}
