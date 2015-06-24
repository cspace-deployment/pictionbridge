package edu.berkeley.cspace.piction;

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

public class HttpBatchUploader implements BatchUploader {
	private static final Logger logger = LogManager.getLogger(HttpBatchUploader.class);
	private CloseableHttpClient client;
	
	private String uploadUrl;
	private String fileFieldName = "imagefiles";
	private Map<String, String> textFields = new HashMap<String, String>();
	private String charset = "UTF-8";
	
	public HttpBatchUploader() {
//		CredentialsProvider credsProvider = new BasicCredentialsProvider();
//		
//		credsProvider.setCredentials(
//				new AuthScope(CSPACE_HOST, AuthScope.ANY_PORT, AUTH_REALM, "basic"),
//				new UsernamePasswordCredentials(username, new String(password)));
		
//		mClientContext = HttpClientContext.create();
//		mClientContext.setCredentialsProvider(credsProvider);

		client = HttpClients.createDefault();		
	}
	
	public void close() {
		try {
			client.close();
		} catch (IOException e) {
			logger.warn("error closing http client", e);
		}
	}
	
	public void send(List<PictionUpdate> updates) throws UploadException {
		HttpPost httpPost = new HttpPost(getUploadUrl());
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		
		entityBuilder.setCharset(Charset.forName(charset));
		
		ContentType textContentType = ContentType.TEXT_PLAIN.withCharset(charset);
		Map<String, String> textFields = getTextFields();
		
		for (String key : textFields.keySet()) {
			String value = textFields.get(key);
			entityBuilder.addTextBody(key, value, textContentType);
		}
		
		for (PictionUpdate update : updates) {
			entityBuilder.addBinaryBody(getFileFieldName(), update.getBinaryFile(), ContentType.parse(update.getMimeType()), update.getFilename());
		}
				
		httpPost.setEntity(entityBuilder.build());

		CloseableHttpResponse response = null;
		
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
				logger.debug(responseContent);
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
			throw new UploadException(response.getStatusLine().getReasonPhrase());
		}
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
