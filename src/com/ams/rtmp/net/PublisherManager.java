package com.ams.rtmp.net;

import com.ams.event.IEventPublisher;
import com.ams.util.ObjectCache;

public class PublisherManager {
	private static int DEFAULT_EXPIRE_TIME = 24 * 60 * 60;
	
	private static ObjectCache<IEventPublisher> streamPublishers = new ObjectCache<IEventPublisher>();
	
	public static IEventPublisher getPublisher(String publishName) {
		return streamPublishers.get(publishName);
	}

	public static void addPublisher(StreamPublisher publisher) {
		String publishName = publisher.getPublishName();
		streamPublishers.put(publishName, publisher, DEFAULT_EXPIRE_TIME);
	}

	public static void removePublisher(String publishName) {
		streamPublishers.remove(publishName);
	}
	
}
