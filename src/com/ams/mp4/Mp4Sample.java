package com.ams.mp4;

public class Mp4Sample {
	private long offset;
	private int size;
	private long timeStamp;
	private boolean keyframe;
    private int descriptionIndex;
	
	public Mp4Sample(long offset, int size, long timeStamp, boolean keyframe, int sampleDescIndex) {
		this.offset = offset;
		this.size = size;
		this.timeStamp = timeStamp;
		this.keyframe = keyframe;
		this.descriptionIndex = sampleDescIndex;
	}

	public long getOffset() {
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

	public int getDescriptionIndex() {
		return descriptionIndex;
	}

}
