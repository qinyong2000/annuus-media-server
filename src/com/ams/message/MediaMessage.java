package com.ams.message;

public class MediaMessage<T> {
	protected long timestamp = 0;
	protected T data;

	public MediaMessage(long timestamp, T data) {
		this.timestamp = timestamp;
		this.data = data;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public T getData() {
		return data;
	}
	
}
