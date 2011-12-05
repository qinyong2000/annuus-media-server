package com.ams.flv;

public class Sample {
	protected long offset;
	protected int size;
	protected long timestamp;
	protected boolean keyframe = true;
	
	public long getOffset() {
		return offset;
	}

	public int getSize() {
		return size;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public boolean isKeyframe() {
		return keyframe;
	}
	
}
