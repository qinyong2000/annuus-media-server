package com.ams.mp4;

import com.ams.flv.Sample;

public class Mp4Sample extends Sample {
    private int descriptionIndex;
	
	public Mp4Sample(int sampleType, long offset, int size, long timestamp, boolean keyframe, int sampleDescIndex) {
		super(sampleType, offset, size, keyframe, timestamp);
		this.descriptionIndex = sampleDescIndex;
	}

	public int getDescriptionIndex() {
		return descriptionIndex;
	}

}
