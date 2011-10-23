package com.ams.rtmp.net;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.ams.message.MediaMessage;
import com.ams.message.IMsgSubscriber;
import com.ams.rtmp.message.*;

public class StreamPlayer implements IPlayer, IMsgSubscriber {
	private NetStream stream;
	private StreamPublisher publisher;
	private long pausedTime = -1;
	private boolean audioPlaying = true;
	private boolean videoPlaying = true;
	private ConcurrentLinkedQueue<MediaMessage> receivedEventQueue;
	private static int MAX_EVENT_QUEUE_LENGTH = 100;
	
	public StreamPlayer(NetStream stream, StreamPublisher publisher) {
		this.stream = stream;
		this.publisher = publisher;
		this.receivedEventQueue = new ConcurrentLinkedQueue<MediaMessage>();
	}

	public void seek(long seekTime) throws IOException {
	}

	public void play() throws IOException {
		MediaMessage msg;
		while ((msg = receivedEventQueue.poll()) != null) {
			RtmpMessage message = (RtmpMessage) msg.getData();
			if ((message instanceof RtmpMessageAudio && audioPlaying)
					|| (message instanceof RtmpMessageVideo && videoPlaying)) {
				stream.writeMessage(msg.getTimestamp(), message);
			}

			if (stream.isWriteBlocking()) {
				break;
			}
		}
	}

	public void messageNotify(MediaMessage msg) {
		if (!isPaused()) {
			if (receivedEventQueue.size() > MAX_EVENT_QUEUE_LENGTH) {
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
