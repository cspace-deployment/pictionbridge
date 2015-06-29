package edu.berkeley.cspace.jaxb;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="document")
public class Relation {
	public static final String DOCTYPE = "Relation";

	public String csid;
	
	@XmlAttribute
	private String name = "relations";

	@XmlElement(name="relations_common", namespace="http://collectionspace.org/services/relation")
	public Common common = new Common();

	public static class Common {
		@XmlElement
		public String relationshipType;
		
		@XmlElement
		public String objectDocumentType;

		@XmlElement
		public String objectCsid;

		@XmlElement
		public String objectRefName;

		@XmlElement
		public String objectUri;

		@XmlElement
		public String subjectDocumentType;

		@XmlElement
		public String subjectCsid;

		@XmlElement
		public String subjectRefName;

		@XmlElement
		public String subjectUri;
	}
}
