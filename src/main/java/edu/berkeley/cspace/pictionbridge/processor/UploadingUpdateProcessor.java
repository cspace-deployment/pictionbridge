package edu.berkeley.cspace.pictionbridge.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.berkeley.cspace.pictionbridge.update.Update;
import edu.berkeley.cspace.pictionbridge.uploader.UploadException;
import edu.berkeley.cspace.pictionbridge.uploader.Uploader;

/**
 * A processor that uploads updates to CollectionSpace, using an uploader. The uploader must
 * set the isUploadedToCollectionSpace property to true for each update successfully uploaded.
 * After attempting the upload, any updates for which isUploadedToCollectionSpace is false are
 * removed from the list.
 */
public class UploadingUpdateProcessor implements UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(UploadingUpdateProcessor.class);

	private Uploader uploader;
	
	@Override
	public void process(List<Update> updates) {
		logger.info(UploadingUpdateProcessor.class.getSimpleName() + " processing " + updates.size() + " updates using uploader " + getUploader().getClass().getSimpleName());

		try {
			getUploader().send(updates);
		} catch (UploadException e) {
			logger.error("error while uploading " + updates.size() + " updates", e);
		}
		
		List<Update> uploadedUpdates = new ArrayList<Update>();
		
		for (Update update : updates) {
			if (update.isUploadedToCollectionSpace()) {
				uploadedUpdates.add(update);
			}
		}
		
		updates.clear();
		updates.addAll(uploadedUpdates);
	}

	@Override
	public void close() {
		
	}

	/**
	 * @return The uploader to use to send updates.
	 */
	public Uploader getUploader() {
		return uploader;
	}

	/**
	 * Sets the uploader to use to send updates.
	 * 
	 * @param uploader The uploader.
	 */
	public void setUploader(Uploader uploader) {
		this.uploader = uploader;
	}
}
