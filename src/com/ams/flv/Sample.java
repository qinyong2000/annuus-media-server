package com.ams.flv;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.ams.io.ByteBufferArray;
import com.ams.io.RandomAccessFileReader;
import com.ams.message.MediaMessage;
import com.ams.rtmp.message.RtmpMessage;
import com.ams.rtmp.message.RtmpMessageAudio;
import com.ams.rtmp.message.RtmpMessageData;
import com.ams.rtmp.message.RtmpMessageVideo;

public class Sample extends MediaMessage<ByteBufferArray> {
	public static final int SAMPLE_AUDIO = 0;
	public static final int SAMPLE_VIDEO = 1;
	public static final int SAMPLE_META = 2;

	protected int sampleType;
	protected boolean keyframe;
	protected long offset;
	protected int size;

	public Sample(int sampleType, long timestamp, ByteBufferArray data) {
		super(timestamp, data);
		this.sampleType = sampleType;
		this.data = data;
	}
	
	public Sample(int sampleType, long timestamp, boolean keyframe, long offset, int size) {
		super(timestamp, null);
		this.sampleType = sampleType;
		this.keyframe = keyframe;
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

	public int getDataSize() {
		return data.size();
	}
	
	public boolean isKeyframe() {
		return keyframe;
	}
	
	public boolean isAudioSample() {
		return sampleType == SAMPLE_AUDIO;
	}
	
	public boolean isVideoSample() {
		return sampleType == SAMPLE_VIDEO;
	}

	public boolean isMetaSample() {
		return sampleType == SAMPLE_META;
	}

	public boolean isVideoKeyframe() {
		ByteBuffer buf = data.getBuffers()[0];
		int firstByte = (buf.get(0) & 0xFF);
		return isVideoSample() && ((firstByte >>> 4) == 1 || firstByte == 0x17);
	}
	
	public boolean isH264VideoSample() {
		ByteBuffer buf = data.getBuffers()[0];
		return (buf.get(0) & 0xFF) == 0x17 || (buf.get(0) & 0xFF) == 0x27;
	}

	public boolean isH264AudioHeader() {
		ByteBuffer buf = data.getBuffers()[0];
		return (buf.get(0) & 0xFF) == 0xAF && (buf.get(1) & 0xFF) == 0x00;
	}

	public boolean isH264VideoHeader() {
		ByteBuffer buf = data.getBuffers()[0];
		return (buf.get(0) & 0xFF) == 0x17 && (buf.get(1) & 0xFF) == 0x00;
	}

	public RtmpMessage toRtmpMessage() {
		RtmpMessage msg = null;
		switch(sampleType) {
		case SAMPLE_META:
			msg = new RtmpMessageData(data);
			break;
		case SAMPLE_VIDEO:
			msg = new RtmpMessageVideo(data);
			break;
		case SAMPLE_AUDIO:
			msg = new RtmpMessageAudio(data);
			break;
		}
		return msg;
	}

}
