package com.ams.rtmp.net;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.ams.flv.Sample;
import com.ams.message.IMsgSubscriber;

public class StreamPlayer implements IPlayer, IMsgSubscriber<Sample> {
	private NetStream stream;
	private StreamPublisher publisher;
	private long pausedTime = -1;
	private boolean audioPlaying = true;
	private boolean videoPlaying = true;
	private ConcurrentLinkedQueue<Sample> receivedEventQueue;
	private static int MAX_EVENT_QUEUE_LENGTH = 100;
	
	public StreamPlayer(NetStream stream, StreamPublisher publisher) {
		this.stream = stream;
		this.publisher = publisher;
		this.receivedEventQueue = new ConcurrentLinkedQueue<Sample>();
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
		do {
			sample = receivedEventQueue.peek();
			if (sample.isVideoKeyframe()) break;
			sample = receivedEventQueue.poll();
		} while(sample != null);
	}

	public void play() throws IOException {
		Sample sample;
		while ((sample = receivedEventQueue.poll()) != null) {
			if ((sample.isAudioSample() && audioPlaying)
				|| (sample.isVideoSample() && videoPlaying)) {
				stream.writeMessage(sample.getTimestamp(), sample.toRtmpMessage());
			}
		}
	}

	public void messageNotify(Sample msg) {
		if (!isPaused()) {
			if (receivedEventQueue.size() > MAX_EVENT_QUEUE_LENGTH && msg.isVideoKeyframe()) {
				receivedEventQueue.clear();
			}
			receivedEventQueue.offer(msg);
		}
	}

	public void pause(boolean pause) {
		if (pause) {
			pausedTime = System.currentTimeMillis();
		} else {
			pausedTime = -1;
		}
	}

	public boolean isPaused() {
		return pausedTime != -1;
	}

	public synchronized void close() {
		receivedEventQueue.clear();
		// remove from publisher
		publisher.removeSubscriber(this);
	}

	public void audioPlaying(boolean flag) {
		this.audioPlaying = flag;

	}

	public void videoPlaying(boolean flag) {
		this.videoPlaying = flag;
	}

}
