package edu.berkeley.cspace.record;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="relations-common-list", namespace="http://collectionspace.org/services/relation")
public class RelationList {
	@XmlElement(name="relation-list-item")
	public List<Item> items = new ArrayList<Item>();
	
	public static class Item {
		@XmlElement
		public String csid;
	}
}
