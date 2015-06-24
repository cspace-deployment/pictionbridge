package edu.berkeley.cspace.piction;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class PictionBridge {
	public static final String DEFAULT_CONFIG_FILE = "piction-bridge.xml";
	
	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext(DEFAULT_CONFIG_FILE);
		
		UpdateProcessor updateProcessor = context.getBean("updateProcessor", UpdateProcessor.class);
		updateProcessor.processUpdates();
	}
}
