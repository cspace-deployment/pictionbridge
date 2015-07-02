package edu.berkeley.cspace.pictionbridge;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HttpBatchMediaUploadResult {
	public List<Image> images;
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Image {
		public String objectnumber;
		public Integer imagenumber;
	}
}
