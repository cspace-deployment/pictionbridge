package edu.berkeley.cspace.pictionbridge;

import java.util.List;

public interface Uploader {
	public void send(List<PictionUpdate> updates) throws UploadException;
	public void close();
}
