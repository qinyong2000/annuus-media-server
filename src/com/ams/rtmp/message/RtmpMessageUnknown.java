package com.ams.rtmp.message;

import java.nio.ByteBuffer;

public class RtmpMessageUnknown extends RtmpMessage {
	private int messageType;
	private ByteBuffer[] data;
	
	public RtmpMessageUnknown(int type, ByteBuffer[] data) {
		super(MESSAGE_UNKNOWN);
		this.messageType = type;
		this.data = data;
	}

	public int getMessageType() {
		return messageType;
	}
	
	public ByteBuffer[] getData() {
		return data;
	}

}
