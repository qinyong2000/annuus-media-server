package com.ams.rtmp.message;

public class RtmpMessageAbort extends RtmpMessage {
	private int streamId;

	public RtmpMessageAbort(int streamId) {
		super(MESSAGE_ABORT);
		this.streamId = streamId;
	}

	public int getStreamId() {
		return streamId;
	}
}
