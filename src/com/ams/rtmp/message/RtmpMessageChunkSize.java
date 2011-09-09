package com.ams.rtmp.message;

public class RtmpMessageChunkSize extends RtmpMessage {
	private int chunkSize;

	public RtmpMessageChunkSize(int chunkSize) {
		super(MESSAGE_CHUNK_SIZE);
		this.chunkSize = chunkSize;
	}

	public int getChunkSize() {
		return chunkSize;
	}
}
