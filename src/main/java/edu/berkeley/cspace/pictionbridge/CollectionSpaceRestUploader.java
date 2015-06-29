package edu.berkeley.cspace.pictionbridge;

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

	public static final String BLOB_SERVICE_NAME = "blobs";
	public static final String MEDIA_SERVICE_NAME = "media";
	public static final String RELATION_SERVICE_NAME = "relations";
	public static final String COLLECTION_OBJECT_SERVICE_NAME = "collectionobjects";

	private RestTemplate restTemplate;
	private Credentials credentials;
	private String servicesUrl;
		
	public CollectionSpaceRestUploader() {
		
	}
	
	@Override
	public void close() {

	}
	
	@Override
	public void send(List<Update> updates) throws UploadException {		
		for (Update update : updates) {
			send(update);
		}
	}

	private void send(Update update) throws UploadException {
		switch(update.getAction()) {
			case NEW:
				doNew(update);
				break;
			case UPDATE:
				doUpdate(update);
				break;
			case DELETE:
				doDelete(update);
				break;
			default:
				logger.warn("skipping unhandled action " + update.getAction() + " for update " + update.getId());		
		}
	}
	
	private void doNew(Update update) throws UploadException {
		// TEST!!!
		update.setObjectCsid("5dfbcb91-f924-43ca-905a");
		
		if (update.getObjectCsid() == null) {
			logger.warn("skipping " + update.getAction() + " " + update.getId() + " with null object csid");
			return;
		}

		CollectionObject collectionObject = readCollectionObject(update.getObjectCsid());

		if (collectionObject == null) {
			logger.error("could not find collectionobject for update " + update.getId() + " with csid " + update.getObjectCsid());
			return;			
		}
		
		logger.debug("found collection object for csid " + update.getObjectCsid() + ": " + collectionObject.toString());

		String blobCsid = createBlob(update.getFilename(), update.getBinaryFile());
		String mediaCsid = createMedia(update.getFilename(), getImageNumber(update), blobCsid);
		
		// Synthesize a Media object with just the fields needed to make a relation.
		// This saves a call to read the full media record from the REST API.
		
		Media media = new Media();
		media.csid = mediaCsid;
		media.core.uri = "/media/" + mediaCsid;
		media.core.refName = "urn:cspace:bampfa.cspace.berkeley.edu:media:id(" + mediaCsid + ")";
		
		createRelations(collectionObject, media);	
	}
	
	private void doUpdate(Update update) throws UploadException {
		// TEST!!!
		update.setMediaCsid("5b7aac25-bc30-4e33-91ef");

		// Blobs currently can't have their binary updated (CSPACE-6633).
		// Instead, create a new blob, set the media blobCsid to point
		// to the new blob, and then delete the old blob.
		
		if (update.getMediaCsid() == null) {
			logger.warn("skipping " + update.getAction() + " " + update.getId() + " with null media csid");
			return;
		}

		Media media = readMedia(update.getMediaCsid());

		if (media == null) {
			logger.error("could not find media for update " + update.getId() + " with csid " + update.getObjectCsid());
			return;			
		}
		
		String oldBlobCsid = media.common.blobCsid;
		String newBlobCsid = createBlob(update.getFilename(), update.getBinaryFile());

		// Create a sparse media update.
		
		Media newMedia = new Media();
		newMedia.csid = update.getMediaCsid();
		newMedia.common.blobCsid = newBlobCsid;
		
		updateMedia(newMedia);
		
		// Delete the previous blob.
		
		deleteBlob(oldBlobCsid);
	}
	
	private void doDelete(Update update) throws UploadException {
		
	}
	
	private String createBlob(String filename, File binaryFile) throws UploadException {
		logger.debug("creating blob for file " + filename);
		
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<String, Object>();
		form.add("file", new FileSystemResource(binaryFile));
		
		URI location = this.restTemplate.postForLocation(getServicesUrl(), form, BLOB_SERVICE_NAME, null);
		String csid = uriToCsid(location);
		
		logger.info("created blob with csid " + csid);

		return csid;
	}
	
	private void deleteBlob(String csid) throws UploadException {
		logger.info("deleting blob with csid " + csid);

		this.restTemplate.delete(getServicesUrl(), BLOB_SERVICE_NAME, csid);		
	}
	
	private String createMedia(String title, Integer imageNumber, String blobCsid) throws UploadException {
		logger.debug("creating media with title " + title);
		
		Media media = new Media();
		media.common.title = title;
		media.common.blobCsid = blobCsid;
		media.bampfa.imageNumber = imageNumber;
	
		URI location = this.restTemplate.postForLocation(getServicesUrl(), media, MEDIA_SERVICE_NAME, null);
		String csid = uriToCsid(location);
		
		logger.info("created media with csid " + csid);

		return csid;
	}
	
	private Media readMedia(String csid) {
		Media media = null;
		
		try {
			media = this.restTemplate.getForObject(getServicesUrl(), Media.class, MEDIA_SERVICE_NAME, csid);
			media.csid = csid;
		}
		catch(HttpClientErrorException e) {
			logger.error("could not read media with csid " + csid, e);
		}
		
		return media;
	}
	
	private void updateMedia(Media media) {
		logger.info("updating media with csid " + media.csid);

		try {
			this.restTemplate.put(getServicesUrl(), media, MEDIA_SERVICE_NAME, media.csid);
		}
		catch(HttpClientErrorException e) {
			logger.error("could not update media with csid " + media.csid, e);
		}
	}
	
	private List<String> createRelations(CollectionObject collectionObject, Media media) {
		logger.debug("creating relations between collectionobject " + collectionObject.csid + " and media " + media.csid);

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
		
		URI location = this.restTemplate.postForLocation(getServicesUrl(), relation, RELATION_SERVICE_NAME, null);
		String csid = uriToCsid(location);
		csids.add(csid);
		
		logger.info("created relation with csid " + csid);

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
		
		URI backLocation = this.restTemplate.postForLocation(getServicesUrl(), backRelation, RELATION_SERVICE_NAME, null);
		String backCsid = uriToCsid(backLocation); 
		csids.add(backCsid);
		
		logger.info("created relation with csid " + csid);
		
		return csids;
	}
	
	private CollectionObject readCollectionObject(String csid) {
		CollectionObject collectionObject = null;
		
		try {
			collectionObject = this.restTemplate.getForObject(getServicesUrl(), CollectionObject.class, COLLECTION_OBJECT_SERVICE_NAME, csid);
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

	private Integer getImageNumber(Update update) {
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

	public String getServicesUrl() {
		return servicesUrl;
	}

	public void setServicesUrl(String servicesUrl) {
		this.servicesUrl = servicesUrl;
	}
}
