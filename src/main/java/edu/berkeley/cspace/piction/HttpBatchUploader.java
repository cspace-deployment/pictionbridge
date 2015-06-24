package edu.berkeley.cspace.piction;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

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
	
	private static final String BMU_URL = "https://dev.cspace.berkeley.edu/bampfaDev_project/uploadmedia/rest/upload";
	private static final String BMU_FILE_PART_NAME = "imagefiles";
	
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
		HttpPost httpPost = new HttpPost(BMU_URL);
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		
		entityBuilder.setCharset(Charset.forName("UTF-8"));
		
		ContentType textContentType = ContentType.TEXT_PLAIN.withCharset("UTF-8");
		
		entityBuilder.addTextBody("validateonly", "on", textContentType);
		entityBuilder.addTextBody("creator", "", textContentType);
		entityBuilder.addTextBody("overridecreator", "always", textContentType);
		entityBuilder.addTextBody("rightsholder", "", textContentType);
		entityBuilder.addTextBody("overriderightsholder", "always", textContentType);
		entityBuilder.addTextBody("contributor", "", textContentType);
		entityBuilder.addTextBody("overridecontributor", "always", textContentType);
		entityBuilder.addTextBody("creator", "", textContentType);
		entityBuilder.addTextBody("overridecreator", "always", textContentType);
		entityBuilder.addTextBody("uploadmedia", "yes", textContentType);
		
		for (PictionUpdate update : updates) {
			entityBuilder.addBinaryBody(BMU_FILE_PART_NAME, update.getBinaryFile(), ContentType.parse(update.getMimeType()), update.getFilename());
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
				responseContent = EntityUtils.toString(responseEntity, "UTF-8");
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
}
