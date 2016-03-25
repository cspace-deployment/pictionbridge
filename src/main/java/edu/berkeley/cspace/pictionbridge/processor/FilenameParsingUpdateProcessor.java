package edu.berkeley.cspace.pictionbridge.processor;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.berkeley.cspace.pictionbridge.parser.FilenameParser;
import edu.berkeley.cspace.pictionbridge.update.Update;

public class FilenameParsingUpdateProcessor implements UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(FilenameParsingUpdateProcessor.class);

	private FilenameParser filenameParser;

	@Override
	public void process(List<Update> updates) {
		logger.info(FilenameParsingUpdateProcessor.class.getSimpleName() + " processing " + updates.size() + " updates");

		getFilenameParser().parse(updates);
	}

	@Override
	public void close() {

	}

	public FilenameParser getFilenameParser() {
		return filenameParser;
	}

	public void setFilenameParser(FilenameParser filenameParser) {
		this.filenameParser = filenameParser;
	}
}
