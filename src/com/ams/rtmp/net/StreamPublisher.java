package com.ams.rtmp.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import com.ams.flv.*;
import com.ams.message.*;
import com.ams.rtmp.message.*;
import com.ams.util.ByteBufferHelper;

public class StreamPublisher implements IMsgPublisher {
	private String publishName = null;
	private int bytes = 0;
	private int lastPing = 0;
	private boolean ping = false;

	private LinkedList<IMsgSubscriber> subscribers = new LinkedList<IMsgSubscriber>();

	private FlvSerializer recorder = null; // record to file stream

	public StreamPublisher(String publishName) {
		this.publishName = publishName;
	}

	public synchronized void publish(MediaMessage msg) throws IOException {
		long timestamp = msg.getTimestamp();
		int type = 0;
		ByteBuffer[] data = null;
		RtmpMessage message = (RtmpMessage) msg.getData();
		switch (message.getType()) {
		case RtmpMessage.MESSAGE_AUDIO:
			RtmpMessageAudio audio = (RtmpMessageAudio) message;
			data = audio.getData();
			type = FlvTag.FLV_AUDIO;
			break;
		case RtmpMessage.MESSAGE_VIDEO:
			RtmpMessageVideo video = (RtmpMessageVideo) message;
			data = video.getData();
			type = FlvTag.FLV_VIDEO;
			break;
		case RtmpMessage.MESSAGE_AMF0_DATA:
			RtmpMessageData meta = (RtmpMessageData) message;
			data = meta.getData();
			type = FlvTag.FLV_META;
			break;
		}

		// record to file
		if (recorder != null) {
			recorder.write(type, data, timestamp);
		}

		// publish packet to other stream subscriber
		notify(msg);

		// ping
		ping(ByteBufferHelper.size(data));
	}

	public synchronized void close() {
		if (recorder != null) {
			recorder.close();
		}
		subscribers.clear();
	}

	private void notify(MediaMessage msg) {
		for (IMsgSubscriber subscriber : subscribers) {
			subscriber.messageNotify(msg);
		}
	}
	
	private void ping(int dataSize) {
		bytes += dataSize;
		// ping
		ping = false;
		if (bytes - lastPing > 100000) {
			// if (bytes - lastPing > 1024*20) {
			lastPing = bytes;
			ping = true;
		}
	}
	
	public void addSubscriber(IMsgSubscriber subscrsiber) {
		synchronized (subscribers) {
			subscribers.add(subscrsiber);
		}
	}

	public void removeSubscriber(IMsgSubscriber subscriber) {
		synchronized (subscribers) {
			subscribers.remove(subscriber);
		}
	}

	public void setRecorder(FlvSerializer recorder) {
		this.recorder = recorder;
	}

	public boolean isPing() {
		return ping;
	}

	public int getBytes() {
		return bytes;
	}

	public String getPublishName() {
		return publishName;
	}

}
