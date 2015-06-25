package edu.berkeley.cspace.piction;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StandardUpdateProcessor implements UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(StandardUpdateProcessor.class);

	private UpdateMonitor updateMonitor;
	private BatchUploader batchUploader;
	
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
		
		List<PictionUpdate> updates = getUpdateMonitor().getUpdates(limit);
		List<PictionUpdate> uploads = new ArrayList<PictionUpdate>(uploadBatchSize);
		
		for (PictionUpdate update : updates) {
			count++;
			
			logger.info("processing update " + count + "/" + updates.size() + " (" + totalCount + " updates found" + (limit == null ? "" : ", limited to " + limit) + ")");
			logger.info("\n" + update.toString());
			
			if (update.getAction() == UpdateAction.NEW) {
				uploads.add(update);
				
				if (uploads.size() == uploadBatchSize) {
					sendUploads(uploads);
					uploads.clear();
				}
			}
			else {
				// Some actions are not implemented yet.
				
				logger.warn("skipping update with action " + update.getAction());
			}
		}
		
		
		if (uploads.size() > 0) {
			sendUploads(uploads);
		}
		
		return count;
	}
	
	private void sendUploads(List<PictionUpdate> updates) {
		try {
			getBatchUploader().send(updates);
		}
		catch(UploadException e) {
			logger.error("error sending batch", e);
			return;
		}
		
		// The upload was successfully submitted.
		
		if (getDeleteProcessedUpdates()) {
			for (PictionUpdate update : updates) {
				getUpdateMonitor().deleteUpdate(update);
			}
		}
	}
	
	public UpdateMonitor getUpdateMonitor() {
		return updateMonitor;
	}

	public void setUpdateMonitor(UpdateMonitor updateMonitor) {
		this.updateMonitor = updateMonitor;
	}

	public BatchUploader getBatchUploader() {
		return batchUploader;
	}

	public void setBatchUploader(BatchUploader batchUploader) {
		this.batchUploader = batchUploader;
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
