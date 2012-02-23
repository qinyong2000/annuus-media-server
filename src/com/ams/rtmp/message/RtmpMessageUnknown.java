package com.ams.rtmp.message;

import com.ams.io.ByteBufferArray;

public class RtmpMessageUnknown extends RtmpMessage {
	private int messageType;
	private ByteBufferArray data;
	
	public RtmpMessageUnknown(int type, ByteBufferArray data) {
		super(MESSAGE_UNKNOWN);
		this.messageType = type;
		this.data = data;
	}

	public int getMessageType() {
		return messageType;
	}
	
	public ByteBufferArray getData() {
		return data;
	}

}
