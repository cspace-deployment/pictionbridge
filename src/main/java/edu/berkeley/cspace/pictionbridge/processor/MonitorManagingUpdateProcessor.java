package edu.berkeley.cspace.pictionbridge.processor;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.berkeley.cspace.pictionbridge.monitor.UpdateMonitor;
import edu.berkeley.cspace.pictionbridge.update.Update;

public class MonitorManagingUpdateProcessor implements UpdateProcessor {
	private static final Logger logger = LogManager.getLogger(MonitorManagingUpdateProcessor.class);

	private UpdateMonitor monitor;
	private boolean markComplete = false;
	private boolean deleteBinary = false;
	private boolean logUpdate = false;
	private boolean deleteUpdate = false;
	
	@Override
	public void process(List<Update> updates) {
		logger.info(MonitorManagingUpdateProcessor.class.getSimpleName() + " processing " + updates.size() + " updates with markComplete=" + isMarkComplete() + ", deleteBinary=" + isDeleteBinary() + ", logUpdate=" + isLogUpdate() + ", deleteUpdate=" + isDeleteUpdate());

		for (Update update : updates) {
			if (isMarkComplete()) {
				getMonitor().markUpdateComplete(update);
				getMonitor().setObjectCSID(update);
			}
			
			if (isDeleteBinary()) {
				getMonitor().deleteBinary(update);
			}
			
			if (isLogUpdate()) {
				getMonitor().logUpdate(update);
			}
			
			if (isDeleteUpdate()) {
				getMonitor().deleteUpdate(update);
			}
		}
	}

	@Override
	public void close() {

	}

	public UpdateMonitor getMonitor() {
		return monitor;
	}

	public void setMonitor(UpdateMonitor monitor) {
		this.monitor = monitor;
	}

	public boolean isMarkComplete() {
		return markComplete;
	}

	public void setMarkComplete(boolean markComplete) {
		this.markComplete = markComplete;
	}

	public boolean isDeleteBinary() {
		return deleteBinary;
	}

	public void setDeleteBinary(boolean deleteBinary) {
		this.deleteBinary = deleteBinary;
	}

	public boolean isLogUpdate() {
		return logUpdate;
	}

	public void setLogUpdate(boolean logUpdate) {
		this.logUpdate = logUpdate;
	}

	public boolean isDeleteUpdate() {
		return deleteUpdate;
	}

	public void setDeleteUpdate(boolean deleteUpdate) {
		this.deleteUpdate = deleteUpdate;
	}
}
