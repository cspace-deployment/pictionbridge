package edu.berkeley.cspace.pictionbridge.filter;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.cspace.pictionbridge.update.Update;

public abstract class AbstractUpdateFilter implements UpdateFilter {

	@Override
	public List<Update> apply(List<Update> updates) {
		List<Update> acceptedUpdates = new ArrayList<Update>();
		List<Update> rejectedUpdates = new ArrayList<Update>();
		
		for (Update candidateUpdate : updates) {
			if (accept(candidateUpdate)) {
				acceptedUpdates.add(candidateUpdate);
			}
			else {
				rejectedUpdates.add(candidateUpdate);
			}
		}
		
		updates.clear();
		updates.addAll(acceptedUpdates);
		
		return rejectedUpdates;
	}

	@Override
	public abstract boolean accept(Update update);
}
