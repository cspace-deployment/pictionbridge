package edu.berkeley.cspace.pictionbridge.parser;

import java.util.List;

import edu.berkeley.cspace.pictionbridge.update.Update;

public interface FilenameParser {
	public void parse(List<Update> updates);
	public void parse(Update update);
}
