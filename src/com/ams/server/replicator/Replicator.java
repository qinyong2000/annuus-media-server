package com.ams.server.replicator;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.ams.amf.AmfNull;
import com.ams.amf.AmfValue;
import com.ams.message.MediaMessage;
import com.ams.rtmp.message.RtmpMessage;
import com.ams.rtmp.message.RtmpMessageCommand;
import com.ams.rtmp.RtmpConnection;
import com.ams.server.Connector;
import com.ams.server.MulticastConnector;
import com.ams.util.Log;

class Replicator implements Runnable {
	private String slaveHost = null;
	private int slavePort; 
	private Connector conn = null;
	private RtmpConnection rtmp;
	private boolean running = true;
	private boolean isConnected = false;
	private static int MAX_EVENT_QUEUE_LENGTH = 100;

	private final static int CHANNEL_RTMP_COMMAND = 3;
	private final static int CHANNEL_RTMP_PUBLISH = 20;
	private static final int DEFAULT_TIMEOUT_MS = 24 * 60 * 60 * 1000;
	private static final int MAX_STREAM_ID = 10000;
	private AtomicInteger currentStreamId = new AtomicInteger(1);
	private HashMap<String, Publisher> publishMap = new HashMap<String, Publisher>();
	
	private class Publisher {
		private int streamId = -1;
		private String publishName;
		private long keepAliveTime = 0;
		private boolean start = false;
		private ConcurrentLinkedQueue<MediaMessage> msgQueue = new ConcurrentLinkedQueue<MediaMessage>();
		
		public Publisher(int streamId, String publishName) {
			this.streamId = streamId;
			this.publishName = publishName;
			this.keepAliveTime = System.currentTimeMillis();
		}

		public boolean expire() {
			long currentTime = System.currentTimeMillis();
			return (currentTime - keepAliveTime > DEFAULT_TIMEOUT_MS);
		}
		
		public void addEvent(MediaMessage event) {
			if (msgQueue.size() > MAX_EVENT_QUEUE_LENGTH) {
				msgQueue.clear();
			}
			msgQueue.offer(event);
		}
		
		public void replicate() throws IOException {
			ConcurrentLinkedQueue<MediaMessage> queue = msgQueue;
			MediaMessage msg = null;
			while((msg = queue.poll()) != null) {
				keepAliveTime = System.currentTimeMillis();
				rtmp.writeRtmpMessage(CHANNEL_RTMP_PUBLISH, streamId, msg.getTimestamp(), (RtmpMessage)msg.getData());
			}
		}
		
		public void publish() throws IOException {
			AmfValue[] args = {new AmfNull(), new AmfValue(publishName), new AmfValue("live")};
			RtmpMessage message = new RtmpMessageCommand("publish", 1, args);
			rtmp.writeRtmpMessage(CHANNEL_RTMP_COMMAND, streamId, System.currentTimeMillis(), message);
		}
		
		public void closeStream() throws IOException {
			AmfValue[] args = {new AmfNull()};
			RtmpMessage message = new RtmpMessageCommand("closeStream", 0, args);
			rtmp.writeRtmpMessage(CHANNEL_RTMP_COMMAND, streamId, System.currentTimeMillis(), message);
		}
		
	}

	public Replicator(Connector connector, String host, int port) throws IOException {
		this.slaveHost = host;
		this.slavePort = port;
		conn = connector;
		conn.setTimeout(DEFAULT_TIMEOUT_MS);
		rtmp = new RtmpConnection(conn);
	}
	
	public void run() {
		long publishTime = System.currentTimeMillis();
		while (running) {
			try {
				if (conn.isClosed()) {
					Log.logger.info("connect to " + slaveHost + ":" + slavePort);
					conn.connect(slaveHost, slavePort);
					isConnected = true;
					Log.logger.info("connected to " + slaveHost + ":" + slavePort);
				}
				
				try {
					synchronized (this) {
						wait();
					}
				} catch (InterruptedException e) {}
				
				for (Publisher publisher : publishMap.values()) {
					if (!publisher.start) {
						publisher.publish();
						publisher.start = true;
						continue;
					}
					
					// send publish command every 3 seconds.
					if (conn instanceof MulticastConnector) {
						long now = System.currentTimeMillis();
						if (now - publishTime > 5000) {
							publishTime = now;
							publisher.publish();
						}
					}
					
					if (!publisher.expire()) {
						publisher.replicate();
					} else {
						publisher.closeStream();
						publishMap.remove(publisher.publishName);
					}
				}
				
				conn.flush();
			} catch (IOException e) {
				e.printStackTrace();
				Log.logger.warning(e.toString());
				clear();
			}
		}
		
		clear();
	}
	
	private void closeAllStreams() {
		for(Publisher publisher : publishMap.values()) {
			try {
				publisher.closeStream();
			} catch (IOException e) {}
		}
		try {
			conn.flush();
		} catch (IOException e) {}
	}
	
	private void clear() {
		currentStreamId.set(1);
		publishMap.clear();
		if (!conn.isClosed()) {
			closeAllStreams();
			conn.close();
		}
	}
	
	private int allocStreamId() {
		int id = currentStreamId.addAndGet(1);
		if (id > MAX_STREAM_ID) {
			currentStreamId.set(1);
		}
		return id;
	}
	
	public void publishMessage(String publishName, MediaMessage event) {
		if (!isConnected) return;
		Publisher publisher = publishMap.get(publishName);
		if (publisher != null) {
			publisher.addEvent(event);
		} else {
			int streamId = allocStreamId();
			publishMap.put(publishName, new Publisher(streamId, publishName));
		}
		
		synchronized (this) {
			notifyAll();
		}
	}
	
	public void close() {
		running = false;
	}
}