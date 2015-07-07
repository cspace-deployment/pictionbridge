package edu.berkeley.cspace.pictionbridge.filter;

import java.sql.Timestamp;

import edu.berkeley.cspace.pictionbridge.update.Update;

/**
 * A filter that accepts updates that were added to Piction after it was
 * launched into production.
 */
public class PostLaunchUpdateFilter extends AbstractUpdateFilter implements UpdateFilter {

	private Timestamp launchDate = Timestamp.valueOf("2015-07-01 00:00:00");
	
	@Override
	public boolean accept(Update update) {
		Timestamp dateAddedToPiction = update.getDateTimeAddedToPiction();
		
		return dateAddedToPiction.after(launchDate);
	}

	public String getLaunchDate() {
		return launchDate.toString();
	}

	public void setLaunchDate(String launchDate) {
		this.launchDate = Timestamp.valueOf(launchDate);
	}
}
