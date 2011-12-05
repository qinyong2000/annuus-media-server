package com.ams.mp4;

import com.ams.flv.Sample;

public class Mp4Sample extends Sample {
    private int descriptionIndex;
	
	public Mp4Sample(long offset, int size, long timestamp, boolean keyframe, int sampleDescIndex) {
		this.offset = offset;
		this.size = size;
		this.timestamp = timestamp;
		this.descriptionIndex = sampleDescIndex;
	}

	public int getDescriptionIndex() {
		return descriptionIndex;
	}

}
