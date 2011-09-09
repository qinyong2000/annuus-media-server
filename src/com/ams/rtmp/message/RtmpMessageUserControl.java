package com.ams.rtmp.message;

public class RtmpMessageUserControl extends RtmpMessage {
	public final static int EVT_STREAM_BEGIN = 0;
	public final static int EVT_STREAM_EOF = 1;
	public final static int EVT_STREAM_DRY = 2;
	public final static int EVT_SET_BUFFER_LENGTH = 3;
	public final static int EVT_STREAM_IS_RECORDED = 4;
	public final static int EVT_PING_REQUEST = 6;
	public final static int EVT_PING_RESPONSE = 7;
	public final static int EVT_UNKNOW = 0xFF;

	private int event;
	private int streamId = -1;
	private int timestamp = -1;
	
	public RtmpMessageUserControl(int event, int streamId,  int timestamp) {
		super(MESSAGE_USER_CONTROL);
		this.event = event;
		this.streamId = streamId;
		this.timestamp = timestamp;

	}

	public RtmpMessageUserControl(int event, int streamId ) {
		super(MESSAGE_USER_CONTROL);
		this.event = event;
		this.streamId = streamId;
	}

	public int getStreamId() {
		return streamId;
	}

	public int getEvent() {
		return event;
	}

	public int getTimestamp() {
		return timestamp;
	}
}
