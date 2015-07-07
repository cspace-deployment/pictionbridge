package edu.berkeley.cspace.pictionbridge.update;

import java.io.File;
import java.sql.Timestamp;
import java.util.Objects;

public class Update {
	private Long id;
	private Integer pictionId;
	private String filename;
	private String mimeType;
	private Integer imgSize;
	private Integer imgHeight;
	private Integer imgWidth;
	private String objectCsid;
	private String mediaCsid;
	private String blobCsid;
	private UpdateAction action;
	private UpdateRelationship relationship;
	private Timestamp dateTimeAddedToPiction;
	private Timestamp dateTimeUploaded;
	private File binaryFile;
	private String objectNumber;
	private Integer imageNumber;
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public Integer getPictionId() {
		return pictionId;
	}
	
	public void setPictionId(Integer pictionId) {
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
	
	public Integer getImgSize() {
		return imgSize;
	}
	
	public void setImgSize(Integer imgSize) {
		this.imgSize = imgSize;
	}
	
	public Integer getImgHeight() {
		return imgHeight;
	}
	
	public void setImgHeight(Integer imgHeight) {
		this.imgHeight = imgHeight;
	}
	
	public Integer getImgWidth() {
		return imgWidth;
	}
	
	public void setImgWidth(Integer imgWidth) {
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
	
	public UpdateRelationship getRelationship() {
		return relationship;
	}
	
	public void setRelationship(UpdateRelationship relationship) {
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

	public String getObjectNumber() {
		return objectNumber;
	}

	public void setObjectNumber(String objectNumber) {
		this.objectNumber = objectNumber;
	}

	public Integer getImageNumber() {
		return imageNumber;
	}

	public void setImageNumber(Integer imageNumber) {
		this.imageNumber = imageNumber;
	}

	public String toString() {
		StringBuilder str = new StringBuilder();
		
		str.append("id: " + Objects.toString(getId(), "") + "\n");
		str.append("pictionId: " + Objects.toString(getPictionId(), "") + "\n");
		str.append("filename: " + Objects.toString(getFilename(), "") + "\n");
		str.append("mimeType: " + Objects.toString(getMimeType(), "") + "\n");
		str.append("imgSize: " + Objects.toString(getImgSize(), "") + "\n");
		str.append("imgHeight: " + Objects.toString(getImgHeight(), "") + "\n");
		str.append("imgWidth: " + Objects.toString(getImgWidth(), "") + "\n");
		str.append("objectCsid: " + Objects.toString(getObjectCsid(), "") + "\n");
		str.append("mediaCsid: " + Objects.toString(getMediaCsid(), "") + "\n");
		str.append("blobCsid: " + Objects.toString(getBlobCsid(), "") + "\n");
		str.append("action: " + Objects.toString(getAction(), "") + "\n");
		str.append("relationship: " + Objects.toString(getRelationship(), "") + "\n");
		str.append("dateTimeAddedToPiction: " + Objects.toString(getDateTimeAddedToPiction(), "") + "\n");
		str.append("dateTimeUploaded: " + Objects.toString(getDateTimeUploaded(), "") + "\n");
		str.append("objectNumber: " + Objects.toString(getObjectNumber(), "") + "\n");
		str.append("imageNumber: " + Objects.toString(getImageNumber(), ""));
		
		return str.toString();
	}
}
