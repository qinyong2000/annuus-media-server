package com.ams.flv;

import java.io.IOException;

import com.ams.io.ByteBufferArray;
import com.ams.io.RandomAccessFileReader;
import com.ams.message.MediaMessage;

public class Sample extends MediaMessage<ByteBufferArray> {
	public static final int SAMPLE_AUDIO = 0;
	public static final int SAMPLE_VIDEO = 1;
	public static final int SAMPLE_META = 2;

	protected int sampleType;
	protected long offset;
	protected int size;

	public Sample(int sampleType, long timestamp, ByteBufferArray data) {
		super(timestamp, data);
		this.sampleType = sampleType;
	}
	
	public Sample(int sampleType, long timestamp, boolean keyframe, long offset, int size) {
		super(timestamp, keyframe);
		this.sampleType = sampleType;
		this.offset = offset;
		this.size = size;
	}
	
	public void readData(RandomAccessFileReader reader) throws IOException {
		reader.seek(offset);
		data = new ByteBufferArray(reader.read(size));
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
	
	public ByteBufferArray getData() {
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
