package edu.berkeley.cspace.piction;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StandardUpdateProcessor implements UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(StandardUpdateProcessor.class);

	private UpdateMonitor updateMonitor;
	private BatchUploader batchUploader;
	
	private Integer updateCountLimit;
	private int uploadBatchSize = 100;
	
	public StandardUpdateProcessor() {
		
	}
	
	public int processUpdates() {
		Integer countLimit = getUpdateCountLimit();
		int uploadBatchSize = getUploadBatchSize();
		int totalCount = updateMonitor.getUpdateCount();
		int count = 0;
		
		List<PictionUpdate> updates = updateMonitor.getUpdates(countLimit);
		List<PictionUpdate> uploads = new ArrayList<PictionUpdate>(uploadBatchSize);
		
		for (PictionUpdate update : updates) {
			count++;
			
			logger.info("processing update " + count + "/" + updates.size() + " (" + totalCount + " updates found" + (countLimit == null ? "" : ", limited to " + countLimit) + ")");
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
		
		sendUploads(uploads);
		
		return count;
	}
	
	private void sendUploads(List<PictionUpdate> updates) {
		try {
			batchUploader.send(updates);
		}
		catch(UploadException e) {
			logger.error("error sending batch", e);
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


	public Integer getUpdateCountLimit() {
		return updateCountLimit;
	}

	public void setUpdateCountLimit(Integer updateCountLimit) {
		this.updateCountLimit = updateCountLimit;
	}
}
