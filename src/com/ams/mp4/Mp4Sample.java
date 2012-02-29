package com.ams.mp4;

import com.ams.flv.Sample;

public class Mp4Sample extends Sample {
    private int descriptionIndex;
	
	public Mp4Sample(int sampleType, long timestamp, boolean keyframe, long offset, int size,  int sampleDescIndex) {
		super(sampleType, timestamp, keyframe, offset, size);
		this.descriptionIndex = sampleDescIndex;
	}

	public int getDescriptionIndex() {
		return descriptionIndex;
	}

}
