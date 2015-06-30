package edu.berkeley.cspace.pictionbridge;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StandardUpdateProcessor implements UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(StandardUpdateProcessor.class);

	private UpdateMonitor updateMonitor;
	private Uploader uploader;
	
	private Integer updateLimit;
	private int uploadBatchSize = 100;
	private boolean deleteProcessedUpdates = true;
	
	public StandardUpdateProcessor() {
		
	}
	
	public int processUpdates() {
		Integer limit = getUpdateLimit();
		int uploadBatchSize = getUploadBatchSize();
		int totalCount = getUpdateMonitor().getUpdateCount();
		int count = 0;
		
		List<Update> updates = getUpdateMonitor().getUpdates(limit);
		List<Update> sentUpdates = new ArrayList<Update>();
		List<Update> batch = new ArrayList<Update>(uploadBatchSize);
		
		logger.info("found " + updates.size() + " updates");
		
		for (Update update : updates) {
			count++;
			
			logger.info("processing update " + count + "/" + updates.size() + " (" + totalCount + " updates found" + (limit == null ? "" : ", limited to " + limit) + ")\n" + update.toString());
			
			if (getUploader().supportsAction(update.getAction())) {
				batch.add(update);
				
				if (batch.size() == uploadBatchSize) {
					sentUpdates.addAll(sendBatch(batch));
					batch.clear();
				}
			}
			else {
				logger.warn("skipping update " + update.getId() + ": uploader does not support action " + update.getAction());
			}
		}
		
		if (batch.size() > 0) {
			sentUpdates.addAll(sendBatch(batch));
		}
		
		// The upload was successfully submitted.
		
		if (getDeleteProcessedUpdates()) {
			for (Update sentUpdate : sentUpdates) {
				getUpdateMonitor().deleteUpdate(sentUpdate);
			}
		}
		else {
			if (logger.isDebugEnabled()) {
				for (Update sentUpdate : sentUpdates) {
					logger.debug("deletion disabled, would delete update " + sentUpdate.getId());
				}
			}
		}

		return sentUpdates.size();
	}
	
	private List<Update> sendBatch(List<Update> updates) {
		List<Update> sentUpdates = new ArrayList<Update>();
		
		try {
			sentUpdates = getUploader().send(updates);
		}
		catch(UploadException e) {
			logger.error("error sending batch", e);
		}
		
		return sentUpdates;
	}
	
	public void close() {
		getUploader().close();
	}
	
	public UpdateMonitor getUpdateMonitor() {
		return updateMonitor;
	}

	public void setUpdateMonitor(UpdateMonitor updateMonitor) {
		this.updateMonitor = updateMonitor;
	}

	public Uploader getUploader() {
		return uploader;
	}

	public void setUploader(Uploader uploader) {
		this.uploader = uploader;
	}

	public int getUploadBatchSize() {
		return uploadBatchSize;
	}

	public void setUploadBatchSize(int uploadBatchSize) {
		this.uploadBatchSize = uploadBatchSize;
	}


	public Integer getUpdateLimit() {
		return updateLimit;
	}

	public void setUpdateLimit(Integer updateLimit) {
		this.updateLimit = updateLimit;
	}

	public boolean getDeleteProcessedUpdates() {
		return deleteProcessedUpdates;
	}

	public void setDeleteProcessedUpdates(boolean deleteProcessedUpdates) {
		this.deleteProcessedUpdates = deleteProcessedUpdates;
	}
}
