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
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;

import edu.berkeley.cspace.pictionbridge.update.Update;
import edu.berkeley.cspace.pictionbridge.update.UpdateAction;
import edu.berkeley.cspace.pictionbridge.update.UpdateRelationship;
import edu.berkeley.cspace.record.CollectionObject;
import edu.berkeley.cspace.record.Media;
import edu.berkeley.cspace.record.RecordList;
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

	/*
	 * This method should mirror the method here:
	 * 	https://github.com/cspace-deployment/cspace-ui-plugin-profile-bampfa.js/blob/master/src/plugins/recordTypes/media/utils.js
	 */
	public static String computeOrderNumber(boolean isPrimary, Integer imageNumber) {
		if (imageNumber == null) {
			imageNumber = 0;
		}
		
		String orderNumber = StringUtils.leftPad(imageNumber.toString(), 5, '0');
		
		if (!isPrimary) {
			orderNumber = "alt " + orderNumber;
		}
		
		return orderNumber;
	}
	
	public CollectionSpaceRestUploader() {
		
	}

	@Override
	public boolean supportsAction(UpdateAction action) {
		return (action == UpdateAction.NEW || action == UpdateAction.UPDATE);
	}

	@Override
	public void close() {
		
	}
	
	@Override
	public void send(List<Update> updates) throws UploadException {
		for (Update update : updates) {
			try {
				if (send(update)) {
					update.setUploadedToCollectionSpace(true);
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
			
			try {
				Thread.sleep(pauseBetweenUpdatesMillis);
			} catch (Exception e) {
				logger.debug(e);
			}
		}
	}

	private boolean send(Update update) throws Exception {
		switch(update.getAction()) {
			case NEW:
				// NEW should not be used any more, but if it appears it should be treated like UPDATE.
			case UPDATE:
				return doNewOrUpdate(update);
			case DELETE:
				// DELETE is not currently supported.
				// return doDelete(update);
			default:
				logger.warn("skipping update " +  update.getId() + ": unhandled action " + update.getAction());
				return false;
		}
	}
	
	/**
	 * Handles updates that represent new/updated images.
	 * 
	 * @param update The update
	 * @return       True if successful, false otherwise.
	 * @throws Exception 
	 */
	private boolean doNewOrUpdate(Update update) throws Exception {
		logger.info("checking for existing media records from Piction with filename " + update.getFilename());
		
		List<Media> existingMedia = findMediaFromPiction(update.getFilename());
		
		if (existingMedia.size() > 1) {
			// More than one existing media record that came from Piction has this filename.
			// That shouldn't happen, so bail out.
			
			logger.error("Found " + existingMedia.size() + " existing Piction media records for filename " + update.getFilename());
			
			return false;
		}
		
		if (existingMedia.size() > 0) {
			// It's an update to an image that's already been pushed from Piction.
			// Set the media and blob csids in the update, and dispatch to doUpdate.
			
			Media media = existingMedia.get(0);

			update.setMediaCsid(media.csid);
			update.setBlobCsid(media.common.blobCsid);

			logger.debug("found existing Piction media record for filename " + update.getFilename() + ": csid=" + media.csid + " blobCsid=" + media.common.blobCsid + " pictionId=" + media.piction.pictionId);

			String existingImageHash = media.piction.pictionImageHash;

			if (existingImageHash != null && existingImageHash.equals(update.getHash())) {
				logger.debug("existing image hash " + existingImageHash + " is equal to the update hash -- performing metadata update only");
				
				return doMetadataUpdate(update);
			}
			
			return doUpdate(update);
		}
		else {
			// It's a new image (or at least, one that's coming from Piction from the first time).
			// Dispatch to doNew.
			
			logger.debug("no existing Piction media record found for filename " + update.getFilename());

			return doNew(update);
		}
	}
	
	/**
	 * Handles updates that represent new images.
	 * 
	 * @param update The update
	 * @return       True if successful, false otherwise.
	 * @throws Exception 
	 */
	private boolean doNew(Update update) throws Exception {
		String objectNumber = update.getObjectNumber() != null ? update.getObjectNumber().trim() : null;

		if (update.getObjectCsid() == null && objectNumber == null) {
			logger.warn("Skipping update " +  update.getId() + " (" + update.getAction() + "): object CSID and object number are both null.");
			return false;
		}

		boolean foundWithCsid = false;
		CollectionObject collectionObject = null;
		
		// First, try to find it with the CSID
		String csid = update.getObjectCsid();
		if (csid != null && !csid.trim().isEmpty()) {
			collectionObject = readCollectionObject(update.getObjectCsid());
		}
		
		// Next, try to find with object number
		if (collectionObject == null && (objectNumber != null && !objectNumber.trim().isEmpty())) {
			collectionObject = this.findCollectionObjectById(objectNumber);
		} else {
			foundWithCsid = true;
		}
		
		if (collectionObject == null) {
			csid = update.getObjectCsid() != null ? update.getObjectCsid() : "<empty>";
			objectNumber = objectNumber != null ? objectNumber : "<empty>";
			String message = String.format("Update %d failed: Could not find collection object by CSID='%s' and could not find using object number '%s'.",
					update.getId(), csid, objectNumber);
			logger.error(message);
			return false;
		}
		
		// Ensure the object CSID is set in the update object
		update.setObjectCsid(collectionObject.csid);
		
		if (foundWithCsid == true) {
			logger.debug("Found collection object for csid " + update.getObjectCsid() + ": " + collectionObject.toString());
		} else {
			logger.debug("Found collection object for objectNumber " + objectNumber + ": " + collectionObject.toString());
		}

		update.setBlobCsid(createBlob(update.getFilename(), update.getBinaryFile()));

		String mediaCsid = createMedia(update);
		
		// Synthesize a Media object with just the fields needed to make a relation.
		// This saves a call to read the full media record from the REST API.
		
		Media media = new Media();
		media.csid = mediaCsid;
		media.core.uri = "/" + MEDIA_SERVICE_NAME + "/" + mediaCsid;
		media.core.refName = getMediaRefNameTemplate().expand(mediaCsid).toString();
		
		createRelations(collectionObject, media);
		
		// Delete any non-Piction media records with the same filename.
		
		deleteMediaNotFromPiction(update.getFilename());

		return true;
	}
	
	/**
	 * Handles updates that represent updated image metadata (but not updated image content).
	 * 
	 * @param update The update
	 * @return       True if successful, false otherwise.
	 */
	private boolean doMetadataUpdate(Update update) {
		// Create a sparse media update.
		
		Media media = createMediaFromUpdate(update);
		updateMedia(media);
		
		return true;
	}

	/**
	 * Handles updates that represent updated images.
	 * 
	 * @param update The update
	 * @return       True if successful, false otherwise.
	 */
	private boolean doUpdate(Update update) {
		// Blobs currently can't have their binary updated (CSPACE-6633).
		// Instead, create a new blob, set the media blobCsid to point
		// to the new blob, and then delete the old blob.
		
		String oldBlobCsid = update.getBlobCsid();
		
		if (oldBlobCsid == null) {
			if (update.getMediaCsid() == null) {
				logger.warn("skipping update " +  update.getId() + " (" + update.getAction() + "): media csid is null");
			
				return false;
			}
	
			Media media = readMedia(update.getMediaCsid());
	
			if (media == null) {
				logger.error("update " + update.getId() + " failed: could not find media with csid " + update.getMediaCsid());
			
				return false;
			}
			
			oldBlobCsid = media.common.blobCsid;
		}
		
		update.setBlobCsid(createBlob(update.getFilename(), update.getBinaryFile()));

		// Create a sparse media update.
		
		Media media = createMediaFromUpdate(update);
		updateMedia(media);
		
		// Delete the previous blob.
		
		deleteBlob(oldBlobCsid);
		
		// Delete any non-Piction media records with the same filename.
		
		deleteMediaNotFromPiction(update.getFilename());
		
		return true;
	}
	
	/**
	 * Handle updates that represent deletions.
	 * 
	 * This is a first pass that won't work in production, because we are not supplied the media csid and
	 * blob csid. Need to get these from a search in order for this code to work.
	 * 
	 * @param update The update
	 * @return       True if successful, false otherwise.
	 */
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
	
	private List<Media> findMediaFromPiction(String filename) {
		return findMedia(filename, true);
	}

	private List<Media> findMediaNotFromPiction(String filename) {
		return findMedia(filename, false);
	}

	/**
	 * Find media records by filename. 
	 * 
	 * @param filename      The filename
	 * @param isFromPiction If true, return only media records that originated from Piction.
	 *                      If false, return only media records that did not originate from Piction.
	 *                      If null, return all media records with the filename.
	 * @return              The matching media records. If no media records match,
	 *                      an empty list is returned.
	 */
	private List<Media> findMedia(String filename, Boolean isFromPiction) {
		List<Media> found = new ArrayList<Media>();
		
		// The filename is stored in the title for BAMPFA.
		
		String searchQuery = "(media_common:title=\"" + nxqlEscapeString(filename) + "\")";
		
		String url = UriComponentsBuilder.fromUriString(getServicesUrlTemplate())
			.queryParam("as", searchQuery)
			.queryParam("wf_deleted", "false")
			.queryParam("pgSz", 0)
			.build()
			.toString();
		
		logger.debug("finding media: url=" + url + " isFromPiction=" + isFromPiction);
		
		RecordList recordList = restTemplate.getForObject(url, RecordList.class, MEDIA_SERVICE_NAME, null);
		
		if (recordList.totalItems > 0) {
			for (RecordList.Item item : recordList.items) {
				Media candidateMedia = readMedia(item.csid);
				boolean isMatch = true;
				
				if (isFromPiction != null) {
					// A media record originated from Piction if it has a non-null pictionId.
					
					boolean candidateIsFromPiction = (candidateMedia.piction.pictionId != null);
					isMatch = (candidateIsFromPiction == isFromPiction);
				}
				
				if (isMatch) {
					found.add(candidateMedia);
				}
			}
		}
		
		return found;
	}

	/*
	 * Example cURL equivalent:
	 * 		curl -X GET 'https://core.collectionspace.org/cspace-services/collectionobjects?as=(collectionobjects_common:objectNumber = "2019")' -i -u admin@core.collectionspace.org:Administrator
	 */
	private CollectionObject findCollectionObjectById(String objectNumber) {
		CollectionObject found = null;
		
		String searchQuery = "(collectionobjects_common:objectNumber=\"" + nxqlEscapeString(objectNumber) + "\")";
		
		String url = UriComponentsBuilder.fromUriString(getServicesUrlTemplate())
			.queryParam("as", searchQuery)
			.queryParam("wf_deleted", "false")
			.queryParam("pgSz", 0)
			.build()
			.toString();
		
		logger.debug("searching for cataloging/object: url=" + url);
		
		RecordList recordList = restTemplate.getForObject(url, RecordList.class, COLLECTION_OBJECT_SERVICE_NAME, null);
		
		if (recordList.totalItems == 1) {
			for (RecordList.Item item : recordList.items) {
				found = readCollectionObject(item.csid);
			}
		} else if (recordList.totalItems > 1) {
			logger.error(String.format("Expected to find a single record when searching for objectNumber='%s', but found %d records instead.",
					objectNumber, recordList.totalItems));
		} else {
			logger.error(String.format("Expected to find a record with objectNumber='%s', but found %d records instead.",
					objectNumber, recordList.totalItems));
		}
		
		return found;
	}	
	
	private String nxqlEscapeString(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
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
	
	private String createMedia(Update update) {
		logger.debug("creating media");
		
		Media media = createMediaFromUpdate(update);
		
		URI location = restTemplate.postForLocation(getServicesUrlTemplate(), media, MEDIA_SERVICE_NAME, null);
		String csid = uriToCsid(location);
		
		logger.info("created media with csid " + csid);

		return csid;
	}
	
	private Media createMediaFromUpdate(Update update) {
		Media media = new Media();
		
		String title = update.getFilename();
		boolean isPrimary = (update.getRelationship() == UpdateRelationship.PRIMARY);

		media.csid = update.getMediaCsid();
		media.common.title = title;
		media.common.blobCsid = update.getBlobCsid();
		media.piction.primaryDisplay = isPrimary;
		media.piction.imageNumber = getImageNumber(update);
		media.piction.pictionId = update.getPictionId();
		media.piction.pictionImageHash = update.getHash();
		media.piction.computedOrderNumber = computeOrderNumber(isPrimary, media.piction.imageNumber);
		media.piction.websiteDisplayLevel = normalizeWebsiteDisplayLevel(update.getWebsiteDisplayLevel());

		logger.debug("media csid=" + media.csid + ", title=" + media.common.title + ", primaryDisplay=" + media.piction.primaryDisplay + ", imageNumber=" + media.piction.imageNumber + ", websiteDisplayLevel=" + media.piction.websiteDisplayLevel + ", blobCsid=" + media.common.blobCsid);

		return media;
	}
	
	private String normalizeWebsiteDisplayLevel(String websiteDisplayLevel) {
		String normalized;
		
		switch (websiteDisplayLevel.toLowerCase()) {
			case "display thumbnail only":
			case "display thumbnails only":
				normalized = "Display thumbnail only";
				break;
			case "no public display":
				normalized = "No public display";
				break;
			case "display larger size":
				normalized = "Display larger size";
				break;
			default:
				normalized = websiteDisplayLevel;
		}
		
		return normalized;
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

		try {
			restTemplate.delete(getServicesUrlTemplate(), MEDIA_SERVICE_NAME, csid);
			logger.info("deleted media with csid " + csid);
		} catch (Exception e) {
			logger.warn(String.format("Attempted to but could not delete media record with csid='%s': %s",
					csid, e.getMessage()));
		}
	}
	
	/**
	 * Deletes media records that have a given filename, and did not originate from Piction.
	 * 
	 * @param filename The filename
	 */
	private void deleteMediaNotFromPiction(String filename) {
		logger.info("checking for media not from Piction with filename " + filename);
		
		List<Media> mediaList = findMediaNotFromPiction(filename);
		
		if (mediaList.size() > 0) {
			logger.info("found " + mediaList.size() + " media records that did not originate from Piction");
			
			for (Media media : mediaList) {
				deleteMedia(media.csid);
			}
		}
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
		
		logger.info("created relation with csid " + backCsid);
		
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
			String errMsg = String.format("Expected to but did not find cataloging/object record with CSID='%s'.",
					csid);
			throw new HttpClientErrorException(e.getStatusCode(), errMsg);
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
