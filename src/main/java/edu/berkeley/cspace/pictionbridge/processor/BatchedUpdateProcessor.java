package edu.berkeley.cspace.pictionbridge.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.berkeley.cspace.pictionbridge.update.Update;

/**
 * An update processor that divides updates into batches of configurable size,
 * and sends each batch to another processor.
 *
 */
public class BatchedUpdateProcessor implements UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(BatchedUpdateProcessor.class);

	private int batchSize = 100;
	private UpdateProcessor batchProcessor;
	
	public BatchedUpdateProcessor() {
		
	}
	
	@Override
	public List<Update> processUpdates(List<Update> updates) {
		logger.info(BatchedUpdateProcessor.class.getSimpleName() + " processing " + updates.size() + " updates with batch size " + getBatchSize());
		
		int batchSize = getBatchSize();
		List<Update> batch = new ArrayList<Update>(batchSize);
		List<Update> processedUpdates = new ArrayList<Update>();
		
		for (Update update : updates) {
			batch.add(update);
			
			if (batch.size() == batchSize) {
				processedUpdates.addAll(getBatchProcessor().processUpdates(batch));
				batch.clear();
			}
		}
		
		if (batch.size() > 0) {
			processedUpdates.addAll(getBatchProcessor().processUpdates(batch));
		}
		
		return processedUpdates;
	}
	
	@Override
	public void close() {

	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public UpdateProcessor getBatchProcessor() {
		return batchProcessor;
	}

	public void setBatchProcessor(UpdateProcessor batchProcessor) {
		this.batchProcessor = batchProcessor;
	}
}
