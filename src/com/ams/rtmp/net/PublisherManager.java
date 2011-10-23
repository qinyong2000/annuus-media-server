package com.ams.rtmp.net;

import com.ams.message.IMsgPublisher;
import com.ams.util.ObjectCache;

public class PublisherManager {
	private static int DEFAULT_EXPIRE_TIME = 24 * 60 * 60;
	
	private static ObjectCache<IMsgPublisher> streamPublishers = new ObjectCache<IMsgPublisher>();
	
	public static IMsgPublisher getPublisher(String publishName) {
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
