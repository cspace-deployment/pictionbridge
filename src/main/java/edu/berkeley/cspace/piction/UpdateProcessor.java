package edu.berkeley.cspace.piction;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(UpdateProcessor.class);

	public static final int BMU_BATCH_SIZE = 10;

	private UpdateMonitor monitor;
	private BatchUploader batchUploader;
	
	public UpdateProcessor() {
		this.monitor = new DatabaseUpdateMonitor();
		this.batchUploader = new HttpBatchUploader();
	}
	
	public int processUpdates() {
		return processUpdates(null);
	}
	
	public int processUpdates(Integer limit) {
		int totalCount = monitor.getUpdateCount();
		int count = 0;
		
		List<PictionUpdate> updates = monitor.getUpdates(limit);
		List<PictionUpdate> uploads = new ArrayList<PictionUpdate>(BMU_BATCH_SIZE);
		
		for (PictionUpdate update : updates) {
			count++;
			
			logger.info("processing update " + count + "/" + updates.size() + " (" + totalCount + " updates found" + (limit == null ? "" : ", limited to " + limit) + ")");
			logger.info("\n" + update.toString());
			
			if (update.getAction() == UpdateAction.NEW) {
				uploads.add(update);
				
				if (uploads.size() == BMU_BATCH_SIZE) {
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
	
	public static void main(String[] args) {
		logger.info("processing updates");
		
		UpdateProcessor processor = new UpdateProcessor();
		int count = processor.processUpdates(2);
		
		logger.info(count + " updates processed");
	}
}
