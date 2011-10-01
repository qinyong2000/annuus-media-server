package com.ams.mp4;

public class Mp4Sample {
	private int offset;
	private int size;
	private long timeStamp;
	private boolean keyframe;
	
	public Mp4Sample(int offset, int size, long timeStamp, boolean keyframe) {
		this.offset = offset;
		this.size = size;
		this.timeStamp = timeStamp;
		this.keyframe = keyframe;
	}

	public int getOffset() {
		return offset;
	}

	public int getSize() {
		return size;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public boolean isKeyframe() {
		return keyframe;
	}
}
