package edu.berkeley.cspace.pictionbridge;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HttpBatchMediaUploaderFilenameParser implements FilenameParser {
	private static final Logger logger = LogManager.getLogger(HttpBatchMediaUploaderFilenameParser.class);

	private HttpBatchMediaUploader batchMediaUploader;
	
	public HttpBatchMediaUploaderFilenameParser() {
		
	}

	@Override
	public void parse(List<Update> updates) {
		HttpBatchMediaUploadResult result;
		
		try {
			result = getBatchMediaUploader().uploadBatch(updates);
		}
		catch (UploadException e) {
			logger.error("error parsing", e);
			return;
		}

		if (result != null) {
			List<HttpBatchMediaUploadResult.Image> images = result.images;
			
			if (images.size() != updates.size()) {
				logger.error("incorrect number of parse results: expected " + updates.size() + ", got " + images.size());
				return;
			}
			
			Iterator<Update> updateIterator = updates.iterator();
			
			for (HttpBatchMediaUploadResult.Image image : images) {
				Update update = updateIterator.next();
				
				logger.debug("parse results for " + update.getFilename() + ": objectnumber=" + image.objectnumber + ", imagenumber=" + image.imagenumber);
			
				update.setObjectNumber(image.objectnumber);
				update.setImageNumber(image.imagenumber);
			}
		}
	}

	@Override
	public void parse(Update update) {
		parse(Arrays.asList(update));
	}

	public HttpBatchMediaUploader getBatchMediaUploader() {
		return batchMediaUploader;
	}

	public void setBatchMediaUploader(HttpBatchMediaUploader batchMediaUploader) {
		batchMediaUploader.setParseOnly(true);
		
		this.batchMediaUploader = batchMediaUploader;
	}
}
