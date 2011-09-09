package com.ams.rtmp.message;

public class RtmpMessageAck extends RtmpMessage {
	private int bytes;

	public RtmpMessageAck(int bytes) {
		super(MESSAGE_ACK);
		this.bytes = bytes;
	}

	public int getBytes() {
		return bytes;
	}
}
