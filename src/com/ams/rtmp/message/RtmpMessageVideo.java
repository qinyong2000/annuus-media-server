package com.ams.rtmp.message;

import java.nio.ByteBuffer;

import com.ams.util.ByteBufferHelper;

public class RtmpMessageVideo extends RtmpMessage {
	private ByteBuffer[] data;
	public RtmpMessageVideo(ByteBuffer[] data) {
		super(MESSAGE_VIDEO);
		this.data = data;
	}

	public ByteBuffer[] getData() {
		return ByteBufferHelper.duplicate(data);
	}

}
