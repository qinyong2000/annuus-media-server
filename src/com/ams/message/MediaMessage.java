package com.ams.message;

public class MediaMessage<T> {
	protected long timestamp = 0;
	protected boolean keyframe = false;
	protected T data;

	public MediaMessage(long timestamp, boolean keyframe) {
		this.timestamp = timestamp;
		this.keyframe = keyframe;
	}
	
	public MediaMessage(long timestamp, T data) {
		this.timestamp = timestamp;
		this.data = data;
	}

	public MediaMessage(long timestamp, boolean keyframe, T data) {
		this.timestamp = timestamp;
		this.keyframe = keyframe;
		this.data = data;
	}
	
	public long getTimestamp() {
		return timestamp;
	}

	public boolean isKeyframe() {
		return keyframe;
	}
	
	public T getData() {
		return data;
	}

	public void setKeyframe(boolean keyframe) {
		this.keyframe = keyframe;
	}
	
}
