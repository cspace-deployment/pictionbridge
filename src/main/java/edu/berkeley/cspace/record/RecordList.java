package edu.berkeley.cspace.record;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="abstract-common-list", namespace="http://collectionspace.org/services/jaxb")
public class RecordList {
	@XmlElement
	public Integer pageNum;

	@XmlElement
	public Integer pageSize;

	@XmlElement
	public Integer itemsInPage;

	@XmlElement
	public Integer totalItems;

	@XmlElement
	public String fieldsReturned;
	
	@XmlElement(name="list-item")
	public List<Item> items = new ArrayList<Item>();

	public static class Item {
		@XmlElement
		public String csid;
	}
}
