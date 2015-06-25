package edu.berkeley.cspace.piction;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class PictionBridge {
	public static final String DEFAULT_CONFIG_FILE = "bampfa-config.xml";
	
	public static void main(String[] args) {
		try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(DEFAULT_CONFIG_FILE)) {
			UpdateProcessor updateProcessor = context.getBean("updateProcessor", UpdateProcessor.class);
			updateProcessor.processUpdates();
		}
	}
}