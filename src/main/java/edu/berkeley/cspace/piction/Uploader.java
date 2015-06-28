package edu.berkeley.cspace.piction;

import java.util.List;

public interface Uploader {
	public void send(List<PictionUpdate> updates) throws UploadException;
	public void close();
}
