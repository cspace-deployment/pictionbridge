package edu.berkeley.cspace.piction;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import edu.berkeley.cspace.jaxb.CollectionObject;
import edu.berkeley.cspace.jaxb.Media;
import edu.berkeley.cspace.jaxb.Relation;

public class CollectionSpaceRestUploader implements Uploader {
	private static final Logger logger = LogManager.getLogger(CollectionSpaceRestUploader.class);

	private RestTemplate restTemplate;
	private Credentials credentials;

	private String createBlobUrl;
	private String createMediaUrl;
	private String createRelationUrl;
	private String readCollectionObjectUrl;
	
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
		// TEST!!!
		update.setObjectCsid("5dfbcb91-f924-43ca-905a");
		
		if (update.getObjectCsid() == null) {
			logger.warn("skipping update " + update.getId() + " with null objectcsid");
			return;
		}

		CollectionObject collectionObject = readCollectionObject(update.getObjectCsid());

		if (collectionObject == null) {
			logger.error("could not find collectionobject for update " + update.getId() + " with csid " + update.getObjectCsid());
			return;			
		}
		
		logger.debug("found collection object: " + collectionObject.toString());

		String blobCsid = createBlob(update.getFilename(), update.getBinaryFile());
		String mediaCsid = createMedia(update.getFilename(), getImageNumber(update), blobCsid);
		
		// Synthesize a Media object with just the fields needed to make a relation.
		// This saves a call to read the full media record from the REST API.
		
		Media media = new Media();
		media.csid = mediaCsid;
		media.core.uri = "/media/" + mediaCsid;
		media.core.refName = "urn:cspace:bampfa.cspace.berkeley.edu:media:id(" + mediaCsid + ")";
		
		List<String> relationCsids = createRelations(collectionObject, media);	

		logger.info("created blob with csid " + blobCsid);
		logger.info("created media with csid " + mediaCsid);
		logger.info("created relations with csids " + StringUtils.join(relationCsids, ", "));
	}
	
	private String createBlob(String filename, File binaryFile) throws UploadException {
		logger.debug("creating blob for file " + filename);
		
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<String, Object>();
		form.add("file", new FileSystemResource(binaryFile));
		
		URI location = this.restTemplate.postForLocation(getCreateBlobUrl(), form);
		
		return uriToCsid(location);
	}
		
	private String createMedia(String title, Integer imageNumber, String blobCsid) throws UploadException {
		Media media = new Media();
		media.common.title = title;
		media.common.blobCsid = blobCsid;
		media.bampfa.imageNumber = imageNumber;
	
		URI location = this.restTemplate.postForLocation(getCreateMediaUrl(), media);

		return uriToCsid(location);
	}
	
	private List<String> createRelations(CollectionObject collectionObject, Media media) {
		List<String> csids = new ArrayList<String>();
		
		Relation relation = new Relation();
		relation.common.relationshipType = "affects";
		relation.common.subjectDocumentType = CollectionObject.DOCTYPE;
		relation.common.subjectCsid = collectionObject.csid;
		relation.common.subjectRefName = collectionObject.core.refName;		
		relation.common.subjectUri = collectionObject.core.uri;
		relation.common.objectDocumentType = Media.DOCTYPE;
		relation.common.objectCsid = media.csid;
		relation.common.objectRefName = media.core.refName;		
		relation.common.objectUri = media.core.uri;
		
		URI location = this.restTemplate.postForLocation(getCreateRelationUrl(), relation);
		csids.add(uriToCsid(location));

		Relation backRelation = new Relation();
		backRelation.common.relationshipType = "affects";
		backRelation.common.subjectDocumentType = Media.DOCTYPE;
		backRelation.common.subjectCsid = media.csid;
		backRelation.common.subjectRefName = media.core.refName;		
		backRelation.common.subjectUri = media.core.uri;
		backRelation.common.objectDocumentType = CollectionObject.DOCTYPE;
		backRelation.common.objectCsid = collectionObject.csid;
		backRelation.common.objectRefName = collectionObject.core.refName;		
		backRelation.common.objectUri = collectionObject.core.uri;
		
		URI backLocation = this.restTemplate.postForLocation(getCreateRelationUrl(), backRelation);
		csids.add(uriToCsid(backLocation));
		
		return csids;
	}
	
	private CollectionObject readCollectionObject(String csid) {
		CollectionObject collectionObject = null;
		
		try {
			collectionObject = this.restTemplate.getForObject(getReadCollectionObjectUrl(), CollectionObject.class, csid);
			collectionObject.csid = csid;
		}
		catch(HttpClientErrorException e) {
			logger.error("could not read collectionobject with csid " + csid, e);
		}
		
		return collectionObject;
	}
	
	private String uriToCsid(URI uri) {
		String uriString = uri.toString();
		int index = uriString.lastIndexOf('/') + 1;
		
		return uriString.substring(index);
	}

	private Integer getImageNumber(PictionUpdate update) {
		Integer imageNumber = null;

		if (update.getRelationship() == UpdateRelationship.PRIMARY) {
			imageNumber = 1;
		}
		else if (update.getRelationship() == UpdateRelationship.ALTERNATE) {
			imageNumber = 2;
		}
		
		return imageNumber;
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

	public String getCreateMediaUrl() {
		return createMediaUrl;
	}

	public void setCreateMediaUrl(String createMediaUrl) {
		this.createMediaUrl = createMediaUrl;
	}

	public String getCreateRelationUrl() {
		return createRelationUrl;
	}

	public void setCreateRelationUrl(String createRelationUrl) {
		this.createRelationUrl = createRelationUrl;
	}

	public String getReadCollectionObjectUrl() {
		return readCollectionObjectUrl;
	}

	public void setReadCollectionObjectUrl(String readCollectionObjectUrl) {
		this.readCollectionObjectUrl = readCollectionObjectUrl;
	}
}
