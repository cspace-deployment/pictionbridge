package edu.berkeley.cspace.pictionbridge;

import java.util.List;

public interface Uploader {
	public List<Update> send(List<Update> updates) throws UploadException;
	public boolean supportsAction(UpdateAction action);
	public void close();
}
