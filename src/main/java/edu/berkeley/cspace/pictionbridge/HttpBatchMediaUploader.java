package edu.berkeley.cspace.pictionbridge;

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

public class HttpBatchMediaUploader implements Uploader {
	private static final Logger logger = LogManager.getLogger(HttpBatchMediaUploader.class);
	
	private CloseableHttpClient client;
	private String uploadUrl;
	private String fileFieldName = "imagefiles";
	private Map<String, String> textFields = new HashMap<String, String>();
	private String charset = "UTF-8";
	
	public HttpBatchMediaUploader() {
		client = HttpClients.createDefault();
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
		HttpPost httpPost = new HttpPost(getUploadUrl());
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		
		entityBuilder.setCharset(Charset.forName(charset));
		
		ContentType textContentType = ContentType.TEXT_PLAIN.withCharset(charset);
		Map<String, String> textFields = getTextFields();
		
		for (String key : textFields.keySet()) {
			String value = textFields.get(key);
			entityBuilder.addTextBody(key, value, textContentType);
		}
		
		for (Update update : updates) {
			entityBuilder.addBinaryBody(getFileFieldName(), update.getBinaryFile(), ContentType.parse(update.getMimeType()), update.getFilename());
		}
				
		httpPost.setEntity(entityBuilder.build());

		CloseableHttpResponse response = null;
		
		logger.info("submitting " + updates.size() + " updates to BMU");
		
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
				responseContent = EntityUtils.toString(responseEntity, charset);
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
		
		return updates;
	}
	
	public String getUploadUrl() {
		return uploadUrl;
	}

	public void setUploadUrl(String uploadUrl) {
		this.uploadUrl = uploadUrl;
	}

	public String getFileFieldName() {
		return fileFieldName;
	}

	public void setFileFieldName(String fileFieldName) {
		this.fileFieldName = fileFieldName;
	}

	public Map<String, String> getTextFields() {
		return textFields;
	}

	public void setTextFields(Map<String, String> textFields) {
		this.textFields = textFields;
	}
}
