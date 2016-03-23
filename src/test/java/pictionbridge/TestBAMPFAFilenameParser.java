package pictionbridge;

import org.junit.Assert;
import org.junit.Test;

import edu.berkeley.cspace.pictionbridge.parser.BAMPFAFilenameParser;
import edu.berkeley.cspace.pictionbridge.update.Update;

public class TestBAMPFAFilenameParser {
	private BAMPFAFilenameParser parser = new BAMPFAFilenameParser();
	
	@Test
	public void parsesFilenamesCorrectly() {
		testFilename("bampfa_1968-45_001_2.jpg", "1968.45", 1);
		testFilename("bampfa_1965-3-b_1_1.jpg", "1965.3.b", 1);
		testFilename("bampfa_1992-4-235_1_t.jpg", "1992.4.235", 1);
		testFilename("bampfa_2016-1-2-a-1_3_3.jpg", "2016.1.2.a-1", 3);
	}

	private void testFilename(String filename, String expectedObjectNumber, Integer expectedImageNumber) {
		Update update = new Update();
		update.setFilename(filename);
		
		parser.parse(update);
		
		String actualObjectNumber = update.getObjectNumber();
		Integer actualImageNumber = update.getImageNumber();
		
		Assert.assertEquals(expectedObjectNumber, actualObjectNumber);
		Assert.assertEquals(expectedImageNumber, actualImageNumber);
	}
}
