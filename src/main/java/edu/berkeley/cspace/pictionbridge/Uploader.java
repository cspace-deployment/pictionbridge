package edu.berkeley.cspace.pictionbridge;

import java.util.List;

public interface Uploader {
	public void send(List<Update> updates) throws UploadException;
	public void close();
}
