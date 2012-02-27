package com.ams.message;

public class MediaMessage {
	protected long timestamp = 0;
	protected boolean keyframe = false;
	protected Object data;
	
	public MediaMessage(long timestamp, Object data) {
		this.timestamp = timestamp;
		this.data = data;
	}
	
	public MediaMessage(long timestamp, String message) {
		this.timestamp = timestamp;
		this.data = message;
	}

	public long getTimestamp() {
		return timestamp;
	}
	
	public Object getData() {
		return data;
	}

	public boolean isKeyframe() {
		return keyframe;
	}

	public void setKeyframe(boolean keyframe) {
		this.keyframe = keyframe;
	}
	
}
