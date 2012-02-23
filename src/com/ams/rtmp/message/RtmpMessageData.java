package com.ams.rtmp.message;

import com.ams.io.ByteBufferArray;

public class RtmpMessageData extends RtmpMessage {
	private ByteBufferArray data;

	public RtmpMessageData(ByteBufferArray data) {
		super(MESSAGE_AMF0_DATA);
		this.data = data;
	}

	public ByteBufferArray getData() {
		return data.duplicate();
	}
}
