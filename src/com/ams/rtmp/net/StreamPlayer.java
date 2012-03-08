package com.ams.rtmp.net;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.ams.flv.Sample;
import com.ams.message.IMsgSubscriber;

public class StreamPlayer implements IPlayer, IMsgSubscriber<Sample> {
	private NetStream stream;
	private StreamPublisher publisher;
	private ConcurrentLinkedQueue<Sample> receivedQueue;
	private static int MAX_QUEUE_LENGTH = 100;
	
	public StreamPlayer(NetStream stream, StreamPublisher publisher) {
		this.stream = stream;
		this.publisher = publisher;
		this.receivedQueue = new ConcurrentLinkedQueue<Sample>();
	}

	private void writeStartData() throws IOException {
		Sample sample = publisher.getMeta();
		if (sample != null) {
			stream.writeMessage(sample.toRtmpMessage());
		}
		
		sample = publisher.getVideoHeader();
		if (sample != null) {
			stream.writeMessage(sample.toRtmpMessage());
		}
		sample = publisher.getAudioHeader();
		if (sample != null) {
			stream.writeMessage(sample.toRtmpMessage());
		}
	}
	
	public void seek(long seekTime) throws IOException {
		writeStartData();
		// start from a keyframe
		Sample sample;
		while ((sample = receivedQueue.peek()) != null) {
			if (sample.isVideoKeyframe()) break;
			receivedQueue.poll();
		}
	}

	public void play() throws IOException {
		Sample sample;
		while ((sample = receivedQueue.poll()) != null) {
			stream.writeMessage(sample.getTimestamp(), sample.toRtmpMessage());
		}
	}

	public void messageNotify(Sample msg) {
		if (!isPaused()) {
			if (receivedQueue.size() > MAX_QUEUE_LENGTH && msg.isVideoKeyframe()) {
				receivedQueue.clear();
			}
			receivedQueue.offer(msg);
		}
	}

	public void pause(boolean pause) {
	}

	public boolean isPaused() {
		return false;
	}

	public synchronized void close() {
		receivedQueue.clear();
		// remove from publisher
		publisher.removeSubscriber(this);
	}

	public void audioPlaying(boolean flag) {
	}

	public void videoPlaying(boolean flag) {
	}

}
