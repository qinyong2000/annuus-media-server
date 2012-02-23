package com.ams.rtmp.message;

import com.ams.io.ByteBufferArray;

public class RtmpMessageAudio extends RtmpMessage {
	private ByteBufferArray data;

	public RtmpMessageAudio(ByteBufferArray data) {
		super(MESSAGE_AUDIO);
		this.data = data;
	}

	public ByteBufferArray getData() {
		return data.duplicate();
	}
	
}
