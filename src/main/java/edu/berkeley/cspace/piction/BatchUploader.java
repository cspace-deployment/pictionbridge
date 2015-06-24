package edu.berkeley.cspace.piction;

import java.util.List;

public interface BatchUploader {
	public void send(List<PictionUpdate> updates) throws UploadException;
	public void close();
}
