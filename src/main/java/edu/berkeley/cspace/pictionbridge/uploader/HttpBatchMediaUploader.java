package edu.berkeley.cspace.pictionbridge.uploader;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.berkeley.cspace.pictionbridge.update.Update;
import edu.berkeley.cspace.pictionbridge.update.UpdateAction;

public class HttpBatchMediaUploader implements Uploader {
	private static final Logger logger = LogManager.getLogger(HttpBatchMediaUploader.class);
	
	private static final String FILE_FIELD_NAME = "imagefiles";
	private static final String VALIDATE_ONLY_FIELD_NAME = "validateonly";
	private static final String UPLOAD_IMMEDIATELY_FIELD_NAME = "createmedia";
	private static final String UPLOAD_OFFLINE_FIELD_NAME = "uploadmedia";
	
	private static final String TRUE_VALUE = "true";
	private static final String DEFAULT_CHARSET = "UTF-8";

	// A very small JPEG, used as a placeholder when only parsing filenames.
	// This was created by downloading https://github.com/mathiasbynens/small/blob/master/jpeg.jpg,
	// and running hexdump -v -e '/1 "%d, "' jpeg.jpg
	private static final byte[] TINY_JPEG = new byte[]{
		-1, -40, -1, -37, 0, 67, 0, 3, 2, 2, 2, 2, 2, 3, 2, 2, 2, 3, 3, 3, 3, 4, 6, 4, 4, 4, 4, 4, 8, 6, 6, 5, 6, 9, 8, 10, 10, 9, 8, 9, 9, 10, 12, 15, 12, 10, 11, 14, 11, 9, 9, 13, 17, 13, 14, 15, 16, 16, 17, 16, 10, 12, 18, 19, 18, 16, 19, 15, 16, 16, 16, -1, -55, 0, 11, 8, 0, 1, 0, 1, 1, 1, 17, 0, -1, -52, 0, 6, 0, 16, 16, 5, -1, -38, 0, 8, 1, 1, 0, 0, 63, 0, -46, -49, 32, -1, -39		
	};
		
	private CloseableHttpClient client;
	private ObjectMapper objectMapper;
	private String uploadUrl;
	private boolean parseOnly = false;
	private boolean uploadImmediately = false;
	private Map<String, String> textFields = new HashMap<String, String>();
	
	public HttpBatchMediaUploader() {
		client = HttpClients.createDefault();
		objectMapper = new ObjectMapper();
	}
	
	@Override
	public boolean supportsAction(UpdateAction action) {
		return (action == UpdateAction.NEW);
	}
	
	@Override
	public void close() {
		try {
			client.close();
		} catch (IOException e) {
			logger.warn("error closing http client", e);
		}
	}

	@Override
	public List<Update> send(List<Update> updates) throws UploadException {
		uploadBatch(updates);
		
		return updates;
	}
	
	public HttpBatchMediaUploadResult uploadBatch(List<Update> updates) throws UploadException {
		HttpPost httpPost = new HttpPost(getUploadUrl());
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		
		entityBuilder.setCharset(Charset.forName(DEFAULT_CHARSET));
		
		ContentType textContentType = ContentType.TEXT_PLAIN.withCharset(DEFAULT_CHARSET);
		Map<String, String> textFields = getTextFields();
		
		for (String key : textFields.keySet()) {
			String value = textFields.get(key);
			entityBuilder.addTextBody(key, value, textContentType);
		}
		
		if (isUploadImmediately()) {
			entityBuilder.addTextBody(UPLOAD_IMMEDIATELY_FIELD_NAME, TRUE_VALUE);
		}
		else {
			entityBuilder.addTextBody(UPLOAD_OFFLINE_FIELD_NAME, TRUE_VALUE);
		}

		if (isParseOnly()) {
			entityBuilder.addTextBody(VALIDATE_ONLY_FIELD_NAME, TRUE_VALUE);
			
			// When parsing filenames only, there's no need to send any file data.
			// The BMU expects a valid image, so send a tiny placeholder jpeg.
						
			for (Update update : updates) {
				entityBuilder.addBinaryBody(FILE_FIELD_NAME, TINY_JPEG, ContentType.parse(update.getMimeType()), normalizeFilename(update.getFilename()));
			}			
		}
		else {
			for (Update update : updates) {
				entityBuilder.addBinaryBody(FILE_FIELD_NAME, update.getBinaryFile(), ContentType.parse(update.getMimeType()), normalizeFilename(update.getFilename()));
			}
		}
		
		httpPost.setEntity(entityBuilder.build());

		CloseableHttpResponse response = null;
		
		logger.info("submitting " + updates.size() + " updates to BMU" + (isParseOnly() ? " (parse only)" : ""));
		
		try {
			response = client.execute(httpPost);
		}
		catch(Exception e) {
			throw new UploadException(e);
		}

		int statusCode = response.getStatusLine().getStatusCode();
		
		String responseContent = null;
		HttpEntity responseEntity = response.getEntity();

		if (responseEntity != null) {
			try {
				responseContent = EntityUtils.toString(responseEntity, DEFAULT_CHARSET);
				logger.info("received response from BMU\n" + responseContent);
			}
			catch (IOException e) {
				logger.warn("error getting http response string", e);
			}
		}

		try {
			response.close();
		}
		catch(IOException e) {
			logger.warn("error closing http response", e);
		}
		
		if (statusCode < 200 || statusCode > 299) {
			logger.fatal("BMU submission failed with HTTP status " + statusCode);
			
			throw new UploadException(response.getStatusLine().getReasonPhrase());
		}
		
		HttpBatchMediaUploadResult bmuResponse = null;
		
		try {
			bmuResponse = objectMapper.readValue(responseContent, HttpBatchMediaUploadResult.class);
		} catch (Exception e) {
			logger.error("error parsing BMU response", e);
		}
		
		return bmuResponse;
	}
	
	private String normalizeFilename(String filename) {
		int dotIndex = filename.lastIndexOf(".");
		String basename = filename.substring(0, dotIndex);
	
		if (basename.matches(".*_[a-z]$")) {
			basename = basename.substring(0, basename.length()-2);
			String ext = filename.substring(dotIndex+1, filename.length());
			
			filename = basename + "." + ext;
		}
		
		return filename;
	}
	
	public String getUploadUrl() {
		return uploadUrl;
	}

	public void setUploadUrl(String uploadUrl) {
		this.uploadUrl = uploadUrl;
	}

	public boolean isParseOnly() {
		return parseOnly;
	}

	public void setParseOnly(boolean parseOnly) {
		this.parseOnly = parseOnly;
	}

	public boolean isUploadImmediately() {
		return uploadImmediately;
	}

	public void setUploadImmediately(boolean uploadImmediately) {
		this.uploadImmediately = uploadImmediately;
	}

	public Map<String, String> getTextFields() {
		return textFields;
	}

	public void setTextFields(Map<String, String> textFields) {
		this.textFields = textFields;
	}
}
