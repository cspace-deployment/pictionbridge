package edu.berkeley.cspace.pictionbridge;

import java.util.List;

public interface FilenameParser {
	public void parse(List<Update> updates);
	public void parse(Update update);
}
