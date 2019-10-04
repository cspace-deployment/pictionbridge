package edu.berkeley.cspace.pictionbridge.parser;

import java.util.List;

import edu.berkeley.cspace.pictionbridge.update.Update;

public class CineFilesFilenameParser implements FilenameParser {
	
	@Override
	public void parse(List<Update> updates) {
		for (Update update : updates) {
			parse(update);
		}
	}

	@Override
	public void parse(Update update) {
		// intentionally blank/empty
	}
}
