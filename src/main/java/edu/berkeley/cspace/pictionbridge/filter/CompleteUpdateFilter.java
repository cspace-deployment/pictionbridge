package edu.berkeley.cspace.pictionbridge.filter;

import org.apache.commons.lang3.StringUtils;

import edu.berkeley.cspace.pictionbridge.update.Update;
import edu.berkeley.cspace.pictionbridge.update.UpdateAction;

/**
 * A filter that accepts complete updates.
 */
public class CompleteUpdateFilter extends AbstractUpdateFilter implements UpdateFilter {

	@Override
	public boolean accept(Update update) {
		boolean hasRequiredProperties = (
			update.getId() != null &&
			update.getPictionId() != null &&
			StringUtils.isNotEmpty(update.getFilename()) &&
			StringUtils.isNotEmpty(update.getMimeType()) &&
			update.getImgSize() != null &&
			update.getImgHeight() != null &&
			update.getImgWidth() != null &&
			StringUtils.isNotEmpty(update.getObjectCsid()) &&
			update.getAction() != null &&
			update.getRelationship() != null &&
			update.getDateTimeAddedToPiction() != null &&
			update.getDateTimeUploaded() != null
		);

		if (update.getAction() != UpdateAction.NEW) {
			hasRequiredProperties = (
				hasRequiredProperties &&
				StringUtils.isNotEmpty(update.getMediaCsid()) &&
				StringUtils.isNotEmpty(update.getBlobCsid())
			);
		}
	
		return hasRequiredProperties;
	}
}