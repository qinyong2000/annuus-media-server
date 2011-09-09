package com.ams.rtmp.message;

import java.nio.ByteBuffer;

import com.ams.util.ByteBufferHelper;

public class RtmpMessageAudio extends RtmpMessage {
	private ByteBuffer[] data;

	public RtmpMessageAudio(ByteBuffer[] data) {
		super(MESSAGE_AUDIO);
		this.data = data;
	}

	public ByteBuffer[] getData() {
		return ByteBufferHelper.duplicate(data);
	}
	
}
