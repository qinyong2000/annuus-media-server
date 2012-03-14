package com.ams.util;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectCache<T> {
	private static int DEFAULT_EXPIRE_TIME = 60 * 60;

	private class CacheItem {
		private int expire = DEFAULT_EXPIRE_TIME;
		private long accessTime = 0;
		private T object = null;

		public CacheItem(T object) {
			this.object = object;
			this.accessTime = System.currentTimeMillis();
		}

		public CacheItem(T object, int expire) {
			this.object = object;
			this.expire = expire;
			this.accessTime = System.currentTimeMillis();
		}

		public void access() {
			accessTime = System.currentTimeMillis();
		}

		public boolean isExpired() {
			if (expire == -1)
				return false;
			else
				return (accessTime + expire * 1000) < System
						.currentTimeMillis();
		}

		public T getObject() {
			return object;
		}

	}

	private class CacheCollector extends Thread {
		public CacheCollector() {
			super();
			try {
				setDaemon(true);
			} catch (Exception e) {
			}
		}

		private void collect() {
			// check all cache item
			Iterator<String> it = items.keySet().iterator();
			while (it.hasNext()) {
				String key = it.next();
				CacheItem item = items.get(key);

				// timeout
				if (item.isExpired()) {
					it.remove();
				}
			}

		}

		public void run() {
			try {
				while (! Thread.interrupted()) {
					sleep(30000);
					collect();
				}
			} catch (InterruptedException e) {
				interrupt();
			}
		}
	}

	private CacheCollector collector;
	private ConcurrentHashMap<String, CacheItem> items = new ConcurrentHashMap<String, CacheItem>();

	public ObjectCache() {
		collector = new CacheCollector();
		collector.start();
	}

	public T get(String key) {
		T obj = null;
		if (items.containsKey(key)) {
			CacheItem item = items.get(key);
			// check for timeout
			if (!item.isExpired()) {
				item.access();
				obj = item.getObject();
			} else {
				items.remove(key);
			}
		}

		return obj;
	}

	public void put(String key, T obj) {
		items.put(key, new CacheItem(obj));
	}

	public void put(String key, T obj, int expire) {
		items.put(key, new CacheItem(obj, expire));
	}

	public void remove(String key) {
		items.remove(key);
	}
}
