package edu.berkeley.cspace.jaxb;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="document")
public class CollectionObject {
	public static final String DOCTYPE = "CollectionObject";
	
	public String csid;
	
	@XmlAttribute
	private String name = "collectionobjects";

	@XmlElement(name="collectionspace_core", namespace="http://collectionspace.org/collectionspace_core/")
	public Core core = new Core();
	
	public String toString() {
		return ("refName: " + core.refName + " uri: " + core.uri);
	}
	
	public static class Core {
		@XmlElement
		public String refName;
		
		@XmlElement
		public String uri;
	}
}
