package edu.berkeley.cspace.piction;

import java.io.File;
import java.sql.Timestamp;

public class PictionUpdate {
	private long id;
	private int pictionId;
	private String filename;
	private String mimeType;
	private int imgSize;
	private int imgHeight;
	private int imgWidth;
	private String objectCsid;
	private String mediaCsid;
	private String blobCsid;
	private UpdateAction action;
	private String relationship;
	private Timestamp dateTimeAddedToPiction;
	private Timestamp dateTimeUploaded;
	private File binaryFile;
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public int getPictionId() {
		return pictionId;
	}
	
	public void setPictionId(int pictionId) {
		this.pictionId = pictionId;
	}
	
	public String getFilename() {
		return filename;
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public String getMimeType() {
		return mimeType;
	}
	
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	
	public int getImgSize() {
		return imgSize;
	}
	
	public void setImgSize(int imgSize) {
		this.imgSize = imgSize;
	}
	
	public int getImgHeight() {
		return imgHeight;
	}
	
	public void setImgHeight(int imgHeight) {
		this.imgHeight = imgHeight;
	}
	
	public int getImgWidth() {
		return imgWidth;
	}
	
	public void setImgWidth(int imgWidth) {
		this.imgWidth = imgWidth;
	}
	
	public String getObjectCsid() {
		return objectCsid;
	}
	
	public void setObjectCsid(String objectCsid) {
		this.objectCsid = objectCsid;
	}
	
	public String getMediaCsid() {
		return mediaCsid;
	}
	
	public void setMediaCsid(String mediaCsid) {
		this.mediaCsid = mediaCsid;
	}
	
	public String getBlobCsid() {
		return blobCsid;
	}
	
	public void setBlobCsid(String blobCsid) {
		this.blobCsid = blobCsid;
	}
	
	public UpdateAction getAction() {
		return action;
	}
	
	public void setAction(UpdateAction action) {
		this.action = action;
	}
	
	public String getRelationship() {
		return relationship;
	}
	
	public void setRelationship(String relationship) {
		this.relationship = relationship;
	}
	
	public Timestamp getDateTimeAddedToPiction() {
		return dateTimeAddedToPiction;
	}
	
	public void setDateTimeAddedToPiction(Timestamp dateTimeAddedToPiction) {
		this.dateTimeAddedToPiction = dateTimeAddedToPiction;
	}
	
	public Timestamp getDateTimeUploaded() {
		return dateTimeUploaded;
	}
	
	public void setDateTimeUploaded(Timestamp dateTimeUploaded) {
		this.dateTimeUploaded = dateTimeUploaded;
	}

	public File getBinaryFile() {
		return binaryFile;
	}

	public void setBinaryFile(File binaryFile) {
		this.binaryFile = binaryFile;
	}

	public String toString() {
		StringBuilder str = new StringBuilder();
		
		str.append("id: " + this.getId() + "\n");
		str.append("pictionId: " + this.getPictionId() + "\n");
		str.append("filename: " + this.getFilename() + "\n");
		str.append("mimeType: " + this.getMimeType() + "\n");
		str.append("imgSize: " + this.getImgSize() + "\n");
		str.append("imgHeight: " + this.getImgHeight() + "\n");
		str.append("imgWidth: " + this.getImgWidth() + "\n");
		str.append("objectCsid: " + this.getObjectCsid() + "\n");
		str.append("mediaCsid: " + this.getMediaCsid() + "\n");
		str.append("blobCsid: " + this.getBlobCsid() + "\n");
		str.append("action: " + this.getAction() + "\n");
		str.append("relationship: " + this.getRelationship() + "\n");
		str.append("dateTimeAddedToPiction: " + this.getDateTimeAddedToPiction() + "\n");
		str.append("dateTimeUploaded: " + this.getDateTimeUploaded() + "\n");
		
		return str.toString();
	}
}
