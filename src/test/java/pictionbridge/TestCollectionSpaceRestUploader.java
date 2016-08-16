package pictionbridge;

import org.junit.Assert;
import org.junit.Test;

import edu.berkeley.cspace.pictionbridge.uploader.CollectionSpaceRestUploader;

public class TestCollectionSpaceRestUploader {
	
	@Test
	public void testComputeOrderNumber() {
		// Should handle null imageNumber
		Assert.assertEquals("00000", CollectionSpaceRestUploader.computeOrderNumber(true, null));
	}
}
