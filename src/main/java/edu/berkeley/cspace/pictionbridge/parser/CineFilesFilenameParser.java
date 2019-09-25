package edu.berkeley.cspace.pictionbridge.parser;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.berkeley.cspace.pictionbridge.update.Update;

public class CineFilesFilenameParser implements FilenameParser {
	private static final Logger logger = LogManager.getLogger(CineFilesFilenameParser.class);
	private static final Pattern objectNumberPattern = Pattern.compile("([a-z]+)\\.([a-zA-Z0-9]+)");
	
	@Override
	public void parse(List<Update> updates) {
		for (Update update : updates) {
			parse(update);
		}
	}

	@Override
	public void parse(Update update) {
		ParsedFilename parsed = parseFilename(update.getFilename());
		
		logger.debug("parse results for " + update.getFilename() + ": objectNumber=" + parsed.objectNumber + ", imageNumber=" + parsed.imageNumber);

		update.setObjectNumber(parsed.objectNumber);
		update.setImageNumber(parsed.imageNumber);
	}

	private ParsedFilename parseFilename(String filename) {
		// This algorithm is copied from the BMU: https://github.com/cspace-deployment/bampfa_project/blob/10c29f40736c6dfcf1fc1f750edceb16bd3b6ffc/uploadmedia/utils.py#L146
		
		ParsedFilename parsed = new ParsedFilename();
		
		String objectNumber = filename.replace("cinefiles_", "");
		String[] parts = objectNumber.split("_");
		
		if (parts.length > 1) {
			try {
				parsed.imageNumber = Integer.parseInt(parts[1]);
			}
			catch (Exception e) {
				parsed.imageNumber = null;
			}
		}
		else {
			parsed.imageNumber = 1;
		}
		
		objectNumber = parts[0];
		objectNumber = objectNumber.replace("-", ".");
		objectNumber = objectNumberPattern.matcher(objectNumber).replaceAll("$1-$2");
		objectNumber = objectNumber.replace(".JPG", "").replace(".jpg", "").replace(".TIF", "").replace(".tif", "");
		
		parsed.objectNumber = objectNumber;
		
		return parsed;
	}
	
	private class ParsedFilename {
		public String objectNumber;
		public Integer imageNumber;
	}
}
