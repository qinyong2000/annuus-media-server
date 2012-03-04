package com.ams.rtmp.net;

import java.io.IOException;
import java.util.LinkedList;
import com.ams.flv.*;
import com.ams.message.*;
import com.ams.rtmp.message.*;

public class StreamPublisher implements IMsgPublisher<MediaMessage<RtmpMessage>, Sample> {
	private String publishName = null;
	private int bytes = 0;
	private int lastPing = 0;
	private boolean ping = false;
	private Sample videoHeader = null;
	private Sample audioHeader = null;
	private Sample meta = null;

	private LinkedList<IMsgSubscriber<Sample>> subscribers = new LinkedList<IMsgSubscriber<Sample>>();

	private FlvSerializer recorder = null; // record to file stream

	public StreamPublisher(String publishName) {
		this.publishName = publishName;
		String tokens[] = publishName.split(":");
		if (tokens.length >= 2) {
			this.publishName = tokens[1];
		}
	}

	public synchronized void publish(MediaMessage<RtmpMessage> msg) throws IOException {
		long timestamp = msg.getTimestamp();
		Sample sample = null;
		RtmpMessage message = msg.getData();
		switch (message.getType()) {
		case RtmpMessage.MESSAGE_AUDIO:
			RtmpMessageAudio audio = (RtmpMessageAudio) message;
			sample = new Sample(Sample.SAMPLE_AUDIO, timestamp, audio.getData());
			if (audioHeader == null) {
				if (sample.isH264AudioHeader()) audioHeader = sample;
			}

			break;
		case RtmpMessage.MESSAGE_VIDEO:
			RtmpMessageVideo video = (RtmpMessageVideo) message;
			sample = new Sample(Sample.SAMPLE_VIDEO, timestamp, video.getData());
			if (videoHeader == null) {
				if (sample.isH264VideoHeader()) videoHeader = sample;
			}
			
			break;
		case RtmpMessage.MESSAGE_AMF0_DATA:
			RtmpMessageData m = (RtmpMessageData) message;
			sample = new Sample(Sample.SAMPLE_META, timestamp, m.getData());
			meta = sample;
			break;
		}

		// record to file
		if (recorder != null) {
			recorder.write(sample);
		}
		// publish packet to other stream subscriber
		notify(sample);
		
		// ping
		ping(sample.getDataSize());
	}

	public synchronized void close() {
		if (recorder != null) {
			recorder.close();
		}
		subscribers.clear();
		videoHeader = null;
		audioHeader = null;
		meta = null;
	}

	private void notify(Sample sample) {
		for (IMsgSubscriber<Sample> subscriber : subscribers) {
			subscriber.messageNotify(sample);
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
	
	public void addSubscriber(IMsgSubscriber<Sample> subscrsiber) {
		synchronized (subscribers) {
			subscribers.add(subscrsiber);
		}
	}

	public void removeSubscriber(IMsgSubscriber<Sample> subscriber) {
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

	public Sample getVideoHeader() {
		return videoHeader;
	}

	public Sample getAudioHeader() {
		return audioHeader;
	}
	
	public Sample getMeta() {
		return meta;
	}
}
