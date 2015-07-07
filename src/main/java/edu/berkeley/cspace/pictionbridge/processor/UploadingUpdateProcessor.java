package edu.berkeley.cspace.pictionbridge.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.berkeley.cspace.pictionbridge.update.Update;
import edu.berkeley.cspace.pictionbridge.uploader.UploadException;
import edu.berkeley.cspace.pictionbridge.uploader.Uploader;

public class UploadingUpdateProcessor implements UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(UploadingUpdateProcessor.class);

	private Uploader uploader;
	
	@Override
	public List<Update> processUpdates(List<Update> updates) {
		logger.info(UploadingUpdateProcessor.class.getSimpleName() + " processing " + updates.size() + " updates using uploader " + getUploader().getClass().getSimpleName());

		List<Update> uploadedUpdates = null;
		
		try {
			uploadedUpdates = getUploader().send(updates);
		} catch (UploadException e) {
			// Uh oh, we don't know which updates were successful, so assume none were.

			logger.error("error occurred while uploading " + updates.size() + " updates -- an unknown number may have succeeded", e);
			
			uploadedUpdates = new ArrayList<Update>();
		}
		
		return uploadedUpdates;
	}

	@Override
	public void close() {
		
	}

	public Uploader getUploader() {
		return uploader;
	}

	public void setUploader(Uploader uploader) {
		this.uploader = uploader;
	}
}
