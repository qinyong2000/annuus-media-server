package com.ams.rtmp.message;

import com.ams.io.ByteBufferArray;

public class RtmpMessageVideo extends RtmpMessage {
	private ByteBufferArray data;
	public RtmpMessageVideo(ByteBufferArray data) {
		super(MESSAGE_VIDEO);
		this.data = data;
	}

	public ByteBufferArray getData() {
		return data.duplicate();
	}

}
