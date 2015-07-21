package edu.berkeley.cspace.pictionbridge.uploader;

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
import org.springframework.web.util.UriTemplate;

import edu.berkeley.cspace.pictionbridge.update.Update;
import edu.berkeley.cspace.pictionbridge.update.UpdateAction;
import edu.berkeley.cspace.pictionbridge.update.UpdateRelationship;
import edu.berkeley.cspace.record.CollectionObject;
import edu.berkeley.cspace.record.Media;
import edu.berkeley.cspace.record.Relation;
import edu.berkeley.cspace.record.RelationList;

public class CollectionSpaceRestUploader implements Uploader {
	private static final Logger logger = LogManager.getLogger(CollectionSpaceRestUploader.class);

	private static final String RELATION_SEARCH_TEMPLATE = "?sbj={subjectCsid}&obj={objectCsid}&prd=affects&pgSz=0";

	public static final String BLOB_SERVICE_NAME = "blobs";
	public static final String MEDIA_SERVICE_NAME = "media";
	public static final String RELATION_SERVICE_NAME = "relations";
	public static final String COLLECTION_OBJECT_SERVICE_NAME = "collectionobjects";
	
	private RestTemplate restTemplate;
	private Credentials credentials;
	private String servicesUrlTemplate;
	private UriTemplate mediaRefNameTemplate;
	private int pauseBetweenUpdatesMillis = 0;

	public CollectionSpaceRestUploader() {
		
	}

	@Override
	public boolean supportsAction(UpdateAction action) {
		return true;
	}

	@Override
	public void close() {
		
	}
	
	@Override
	public List<Update> send(List<Update> updates) throws UploadException {
		List<Update> sentUpdates = new ArrayList<Update>();
		
		for (Update update : updates) {
			if (send(update)) {
				sentUpdates.add(update);
			}
			
			try {
				Thread.sleep(pauseBetweenUpdatesMillis);
			}
			catch (InterruptedException e) {
			}
		}
		
		return sentUpdates;
	}

	private boolean send(Update update) {
		switch(update.getAction()) {
			case NEW:
				return doNew(update);
			case UPDATE:
				return doUpdate(update);
			case DELETE:
				return doDelete(update);
			default:
				logger.warn("skipping update " +  update.getId() + ": unhandled action " + update.getAction());
				return false;
		}
	}
	
	private boolean doNew(Update update) {
		if (update.getObjectCsid() == null) {
			logger.warn("skipping update " +  update.getId() + " (" + update.getAction() + "): object csid is null");
			return false;
		}

		CollectionObject collectionObject = readCollectionObject(update.getObjectCsid());

		if (collectionObject == null) {
			logger.error("update " + update.getId() + " failed: could not find collection object with csid " + update.getObjectCsid());
			return false;
		}
		
		logger.debug("found collection object for csid " + update.getObjectCsid() + ": " + collectionObject.toString());

		String blobCsid = createBlob(update.getFilename(), update.getBinaryFile());
		String mediaCsid = createMedia(update.getFilename(), update.getRelationship(), getImageNumber(update), blobCsid);
		
		// Synthesize a Media object with just the fields needed to make a relation.
		// This saves a call to read the full media record from the REST API.
		
		Media media = new Media();
		media.csid = mediaCsid;
		media.core.uri = "/" + MEDIA_SERVICE_NAME + "/" + mediaCsid;
		media.core.refName = getMediaRefNameTemplate().expand(mediaCsid).toString();
		
		createRelations(collectionObject, media);
		
		return true;
	}
	
	private boolean doUpdate(Update update) {
		// Blobs currently can't have their binary updated (CSPACE-6633).
		// Instead, create a new blob, set the media blobCsid to point
		// to the new blob, and then delete the old blob.
		
		if (update.getMediaCsid() == null) {
			logger.warn("skipping update " +  update.getId() + " (" + update.getAction() + "): media csid is null");
			return false;
		}

		Media media = readMedia(update.getMediaCsid());

		if (media == null) {
			logger.error("update " + update.getId() + " failed: could not find media with csid " + update.getMediaCsid());
			return false;
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
		
		return true;
	}
	
	private boolean doDelete(Update update) {
		if (update.getBlobCsid() == null) {
			logger.warn("skipping update " +  update.getId() + " (" + update.getAction() + "): blob csid is null");
			return false;
		}

		if (update.getMediaCsid() == null) {
			logger.warn("skipping update " +  update.getId() + " (" + update.getAction() + "): media csid is null");
			return false;
		}

		if (update.getObjectCsid() == null) {
			logger.warn("skipping update " +  update.getId() + " (" + update.getAction() + "): object csid is null");
			return false;
		}
		
		deleteCollectionObjectMediaRelations(update.getObjectCsid(), update.getMediaCsid());
		deleteMedia(update.getMediaCsid());
		
		return true;
	}
		
	private String createBlob(String filename, File binaryFile) {
		logger.debug("creating blob for file " + filename + " with content " + binaryFile.getAbsolutePath());
		
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<String, Object>();
		form.add("file", new FileSystemResource(binaryFile));
		
		URI location = restTemplate.postForLocation(getServicesUrlTemplate(), form, BLOB_SERVICE_NAME, null);
		String csid = uriToCsid(location);
		
		logger.info("created blob with csid " + csid);

		return csid;
	}
	
	private void deleteBlob(String csid) {
		logger.debug("deleting blob with csid " + csid);

		restTemplate.delete(getServicesUrlTemplate(), BLOB_SERVICE_NAME, csid);

		logger.info("deleted blob with csid " + csid);
	}
	
	private String createMedia(String title, UpdateRelationship relationship, Integer imageNumber, String blobCsid) {
		boolean isPrimary = (relationship == UpdateRelationship.PRIMARY);

		logger.debug("creating media with title " + title + ", primary=" + isPrimary + ", image number " + imageNumber + ", blob " + blobCsid);
		
		Media media = new Media();
		media.common.title = title;
		media.common.blobCsid = blobCsid;
		media.bampfa.primaryDisplay = isPrimary;
		media.bampfa.imageNumber = imageNumber;
		media.bampfa.computedOrderNumber = computeOrderNumber(isPrimary, imageNumber);
	
		URI location = restTemplate.postForLocation(getServicesUrlTemplate(), media, MEDIA_SERVICE_NAME, null);
		String csid = uriToCsid(location);
		
		logger.info("created media with csid " + csid);

		return csid;
	}
	
	private String computeOrderNumber(boolean isPrimary, Integer imageNumber) {
		String orderNumber = StringUtils.leftPad(imageNumber.toString(), 5, '0');
		
		if (!isPrimary) {
			orderNumber = "alt " + orderNumber;
		}
		
		return orderNumber;
	}
	
	private Media readMedia(String csid) {
		Media media = null;
		
		try {
			media = restTemplate.getForObject(getServicesUrlTemplate(), Media.class, MEDIA_SERVICE_NAME, csid);
			media.csid = csid;
		}
		catch(HttpClientErrorException e) {
			logger.error("could not read media with csid " + csid, e);
		}
		
		return media;
	}
	
	private void updateMedia(Media media) {
		logger.debug("updating media with csid " + media.csid);

		try {
			restTemplate.put(getServicesUrlTemplate(), media, MEDIA_SERVICE_NAME, media.csid);
		}
		catch(HttpClientErrorException e) {
			logger.error("could not update media with csid " + media.csid, e);
		}
		
		logger.info("updated media with csid " + media.csid);
	}
	
	private void deleteMedia(String csid) {
		// This also deletes the associated blob.
		
		logger.debug("deleting media with csid " + csid);

		restTemplate.delete(getServicesUrlTemplate(), MEDIA_SERVICE_NAME, csid);

		logger.info("deleted media with csid " + csid);
	}
	
	private List<String> createRelations(CollectionObject collectionObject, Media media) {
		logger.debug("creating relations between collection object " + collectionObject.csid + " and media " + media.csid);
		logger.debug("creating forward relation");
		
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
		
		URI location = restTemplate.postForLocation(getServicesUrlTemplate(), relation, RELATION_SERVICE_NAME, null);
		String csid = uriToCsid(location);
		csids.add(csid);
		
		logger.info("created relation with csid " + csid);
		logger.debug("creating backward relation");
		
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
		
		URI backLocation = restTemplate.postForLocation(getServicesUrlTemplate(), backRelation, RELATION_SERVICE_NAME, null);
		String backCsid = uriToCsid(backLocation); 
		csids.add(backCsid);
		
		logger.info("created relation with csid " + csid);
		
		return csids;
	}

	private void deleteCollectionObjectMediaRelations(String collectionObjectCsid, String mediaCsid) {
		logger.debug("deleting relations between collection object " + collectionObjectCsid + " and media " + mediaCsid);

		deleteRelations(collectionObjectCsid, mediaCsid);
		deleteRelations(mediaCsid, collectionObjectCsid);
	}

	private void deleteRelations(String subjectCsid, String objectCsid) {
		logger.debug("finding relations between subject " + subjectCsid + " and object " + objectCsid);
		
		RelationList relationList = restTemplate.getForObject(getServicesUrlTemplate() + RELATION_SEARCH_TEMPLATE, RelationList.class, RELATION_SERVICE_NAME, null, subjectCsid, objectCsid);
		List<String> csids = new ArrayList<String>();
		
		for (RelationList.Item item : relationList.items) {
			csids.add(item.csid);
		}
		
		logger.debug("found " + csids.size() + " relations");
		
		for (String csid : csids) {
			deleteRelation(csid);
		}
	}
	
	private void deleteRelation(String csid) {
		logger.debug("deleting relation with csid " + csid);

		restTemplate.delete(getServicesUrlTemplate(), RELATION_SERVICE_NAME, csid);

		logger.info("deleted relation with csid " + csid);
	}
	
	private CollectionObject readCollectionObject(String csid) {
		CollectionObject collectionObject = null;
		
		try {
			collectionObject = restTemplate.getForObject(getServicesUrlTemplate(), CollectionObject.class, COLLECTION_OBJECT_SERVICE_NAME, csid);
			collectionObject.csid = csid;
		}
		catch(HttpClientErrorException e) {
			logger.error("could not read collection object with csid " + csid, e);
		}
		
		return collectionObject;
	}

	private String uriToCsid(URI uri) {
		String uriString = uri.toString();
		int index = uriString.lastIndexOf('/') + 1;
		
		return uriString.substring(index);
	}

	private Integer getImageNumber(Update update) {
		Integer imageNumber = update.getImageNumber();

		if (imageNumber == null) {
			logger.warn("image number is null for update " + update.getId() + ", setting to 0");
			
			imageNumber = 0;
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

	public String getServicesUrlTemplate() {
		return servicesUrlTemplate;
	}

	public void setServicesUrlTemplate(String servicesUrlTemplate) {
		this.servicesUrlTemplate = servicesUrlTemplate;
	}

	public UriTemplate getMediaRefNameTemplate() {
		return mediaRefNameTemplate;
	}

	public void setMediaRefNameTemplate(String mediaRefNameTemplate) {
		this.mediaRefNameTemplate = new UriTemplate(mediaRefNameTemplate);
	}

	public int getPauseBetweenUpdatesMillis() {
		return pauseBetweenUpdatesMillis;
	}

	public void setPauseBetweenUpdatesMillis(int pauseBetweenUpdatesMillis) {
		this.pauseBetweenUpdatesMillis = pauseBetweenUpdatesMillis;
	}
}
