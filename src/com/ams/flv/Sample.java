package com.ams.flv;

import java.nio.ByteBuffer;

public class Sample {
	public static final int SAMPLE_AUDIO = 0;
	public static final int SAMPLE_VIDEO = 1;
	public static final int SAMPLE_META = 2;

	protected int sampleType;
	protected long offset;
	protected int size;
	protected long timestamp;
	protected boolean keyframe = true;
	protected ByteBuffer[] data = null;

	public Sample(int sampleType, ByteBuffer[] data, long timestamp) {
		this.sampleType = sampleType;
		this.data = data;
		this.timestamp = timestamp;
	}
	
	public Sample(int sampleType, long offset, int size, boolean keyframe, long timestamp) {
		this.sampleType = sampleType;
		this.offset = offset;
		this.size = size;
		this.keyframe = keyframe;
		this.timestamp = timestamp;
	}
	
	public int getSampleType() {
		return sampleType;
	}
	
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
	
	public ByteBuffer[] getData() {
		return data;
	}

	public boolean isAudioTag() {
		return sampleType == SAMPLE_AUDIO;
	}
	
	public boolean isVideoTag() {
		return sampleType == SAMPLE_VIDEO;
	}

	public boolean isMetaTag() {
		return sampleType == SAMPLE_META;
	}
}
