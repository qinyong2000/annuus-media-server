package com.ams.rtmp.message;

import com.ams.so.SoMessage;

public class RtmpMessageSharedObject extends RtmpMessage {
	private SoMessage data;

	public RtmpMessageSharedObject(SoMessage data) {
		super(MESSAGE_SHARED_OBJECT);
		this.data = data;
	}

	public SoMessage getData() {
		return data;
	}
}
