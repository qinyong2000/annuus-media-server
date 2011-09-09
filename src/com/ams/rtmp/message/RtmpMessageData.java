package com.ams.rtmp.message;

import java.nio.ByteBuffer;

import com.ams.util.ByteBufferHelper;

public class RtmpMessageData extends RtmpMessage {
	private ByteBuffer[] data;

	public RtmpMessageData(ByteBuffer[] data) {
		super(MESSAGE_AMF0_DATA);
		this.data = data;
	}

	public ByteBuffer[] getData() {
		return ByteBufferHelper.duplicate(data);
	}
}
