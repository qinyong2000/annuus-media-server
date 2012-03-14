package com.ams.rtmp.net;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.ams.message.IMediaDeserializer;
import com.ams.message.MediaSample;
import com.ams.message.IMsgSubscriber;

public class StreamSubscriber implements IMediaDeserializer, IMsgSubscriber<MediaSample> {
	private StreamPublisher publisher;
	private ConcurrentLinkedQueue<MediaSample> receivedQueue;
	private static int MAX_QUEUE_LENGTH = 100;
	
	public StreamSubscriber(StreamPublisher publisher) {
		this.publisher = publisher;
		this.receivedQueue = new ConcurrentLinkedQueue<MediaSample>();
	}

	public MediaSample metaData() {
		return publisher.getMetaData();
	}

	public MediaSample videoHeaderData() {
		return publisher.getVideoHeader();
	}

	public MediaSample audioHeaderData() {
		return publisher.getAudioHeader();
	}

	public MediaSample seek(long seekTime) throws IOException {
		// start from a keyframe
		MediaSample sample;
		while ((sample = receivedQueue.peek()) != null) {
			if (sample.isVideoKeyframe()) break;
			receivedQueue.poll();
		}
		
		return null;
	}

	public MediaSample readNext() throws IOException {
		return receivedQueue.poll();
	}
	
	public void messageNotify(MediaSample msg) {
		if (receivedQueue.size() > MAX_QUEUE_LENGTH && msg.isVideoKeyframe()) {
			receivedQueue.clear();
		}
		receivedQueue.offer(msg);
	}

	public synchronized void close() {
		receivedQueue.clear();
		// remove from publisher
		publisher.removeSubscriber(this);
	}

	

}
