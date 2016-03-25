package edu.berkeley.cspace.pictionbridge.uploader;

import java.util.List;

import edu.berkeley.cspace.pictionbridge.update.Update;
import edu.berkeley.cspace.pictionbridge.update.UpdateAction;

public interface Uploader {
	public void send(List<Update> updates) throws UploadException;
	public boolean supportsAction(UpdateAction action);
	public void close();
}
