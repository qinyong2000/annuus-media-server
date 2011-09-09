package com.ams.event;

import com.ams.rtmp.message.RtmpMessage;

public class Event {
	public final static int EVT_RTMP = 0;
	public final static int EVT_TEXT = 1;
	
	protected long timestamp = 0;
	protected int type = 0;
	protected Object event;
	
	public Event(long timestamp, int type, Object event) {
		this.timestamp = timestamp;
		this.type = type;
		this.event = event;
	}
	
	public Event(long timestamp, RtmpMessage message) {
		this.timestamp = timestamp;
		this.type = EVT_RTMP;
		this.event = message;
	}

	public Event(long timestamp, String message) {
		this.timestamp = timestamp;
		this.type = EVT_TEXT;
		this.event = message;
	}

	public long getTimestamp() {
		return timestamp;
	}
	
	public Object getEvent() {
		return event;
	}


	public int getType() {
		return type;
	}
}
