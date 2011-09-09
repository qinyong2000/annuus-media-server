package com.ams.rtmp.message;

public class RtmpMessageWindowAckSize extends RtmpMessage {
	private int size;

	public RtmpMessageWindowAckSize(int size) {
		super(MESSAGE_WINDOW_ACK_SIZE);
		this.size = size;
	}

	public int getSize() {
		return size;
	}
}
