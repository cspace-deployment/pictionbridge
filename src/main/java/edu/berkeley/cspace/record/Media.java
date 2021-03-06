package edu.berkeley.cspace.record;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="document")
public class Media {
	public static final String DOCTYPE = "Media";

	public String csid;
	
	@XmlAttribute
	private String name = "media";
	
	@XmlElement(name="media_common", namespace="http://collectionspace.org/services/media")
	public Common common = new Common();
	
	@XmlElement(name="media_piction", namespace="http://collectionspace.org/services/media/domain/piction")
	public PictionMediaExtension piction = new PictionMediaExtension();

	@XmlElement(name="collectionspace_core", namespace="http://collectionspace.org/collectionspace_core/")
	public Core core = new Core();

	public static class Common {
		@XmlElement
		public String title;

		@XmlElement
		public String blobCsid;
	}
	
	public static class PictionMediaExtension {
		@XmlElement
		public Integer imageNumber;

		@XmlElement
		public Boolean primaryDisplay;
		
		@XmlElement
		public String computedOrderNumber;
		
		@XmlElement
		public Integer pictionId;

		@XmlElement
		public String pictionImageHash;

		@XmlElement
		public String websiteDisplayLevel;
	}
	
	public static class Core {
		@XmlElement
		public String refName;
		
		@XmlElement
		public String uri;
	}
}

