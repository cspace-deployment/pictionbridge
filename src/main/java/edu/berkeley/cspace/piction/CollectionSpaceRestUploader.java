package edu.berkeley.cspace.piction;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class CollectionSpaceRestUploader implements Uploader {
	private static final Logger logger = LogManager.getLogger(CollectionSpaceRestUploader.class);

	private RestTemplate restTemplate;
	private Credentials credentials;

	private String createBlobUrl;

	public CollectionSpaceRestUploader() {
	}
	
	@Override
	public void close() {

	}
	
	@Override
	public void send(List<PictionUpdate> updates) throws UploadException {		
		for (PictionUpdate update : updates) {
			send(update);
		}
	}

	private void send(PictionUpdate update) throws UploadException {
		if (update.getAction() == UpdateAction.NEW) {
			doNew(update);
		}
		else {
			logger.warn("skipping unhandled action " + update.getAction() + " for update " + update.getId());
		}		
	}
	
	private void doNew(PictionUpdate update) throws UploadException {
		String blobCsid = createBlob(update.getFilename(), update.getBinaryFile());
		
		logger.debug("blobCsid=" + blobCsid);
	}
	
	private String createBlob(String filename, File binaryFile) throws UploadException {
		logger.debug("creating blob for file " + filename);
		
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<String, Object>();
		form.add("file", new FileSystemResource(binaryFile));
		
		URI location = this.restTemplate.postForLocation(getCreateBlobUrl(), form);
		
		return uriToCsid(location);
	}
	
	private String uriToCsid(URI uri) {
		String uriString = uri.toString();
		int index = uriString.lastIndexOf('/') + 1;
		
		return uriString.substring(index);
	}
	
	private void createMedia(String name, String number, String blobCsid) throws UploadException {
		
	}
	
	private void createMedia() {
		
	}
	
	private void createRelation() {
		
	}
	
	public Credentials getCredentials() {
		return credentials;
	}

	public void setCredentials(Credentials credentials) {
		BasicCredentialsProvider credentialsProvider =  new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, credentials);
		
		HttpClient httpClient = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();
				
		restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
	}

	public String getCreateBlobUrl() {
		return createBlobUrl;
	}

	public void setCreateBlobUrl(String createBlobUrl) {
		this.createBlobUrl = createBlobUrl;
	}
}
