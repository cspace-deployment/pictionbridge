package edu.berkeley.cspace.pictionbridge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.berkeley.cspace.pictionbridge.update.Update;

public class PictionBridgeUtils {
	private static final Logger logger = LogManager.getLogger(PictionBridgeUtils.class);

	public static boolean verifyHash(Update update) {
		boolean verified = false;
		String expectedHash = update.getHash();
		
		if (expectedHash != null) {
			File binaryFile = update.getBinaryFile();
			
			if (binaryFile != null) {
				FileInputStream fileInputStream = null;
				
				try {
					fileInputStream = new FileInputStream(binaryFile);
				} catch (FileNotFoundException e) {
					logger.error("binary file not found at " + binaryFile.getAbsolutePath());
				}
				
				if (fileInputStream != null) {
					String algorithm = "SHA-1";
					MessageDigest digest = null;
					
					try {
						digest = MessageDigest.getInstance(algorithm);
					} catch (NoSuchAlgorithmException e) {
						logger.error("no digest algorithm " + algorithm);
					}
					
					if (digest != null) {
						DigestInputStream digestInputStream = new DigestInputStream(fileInputStream, digest);
						
						try {
							while(digestInputStream.read() != -1);
						}
						catch(IOException e) {
							logger.error("error reading binary file " + binaryFile.getAbsolutePath(), e);
						}
						
						byte[] digestResult = digest.digest();
						String actualHash = Hex.encodeHexString(digestResult);
						
						logger.debug("computed hash: " + actualHash);
						
						verified = actualHash.equals(expectedHash);
					}
				}
			}
		}
		
		return verified;
	}
}
