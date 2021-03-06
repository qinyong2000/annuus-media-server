package com.ams.flv;

import java.io.IOException;
import com.ams.io.ByteBufferArray;
import com.ams.io.ByteBufferInputStream;
import com.ams.message.MediaSample;

public class AudioTag extends MediaSample {
	private int soundFormat = -1;
	private int soundRate = 0;
	private int soundSize = 0;
	private int soundType = -1;
	
	public AudioTag(long timestamp, ByteBufferArray data) {
		super(MediaSample.SAMPLE_AUDIO, timestamp, data);
	}

	public AudioTag(long timestamp, long offset, int size) {
		super(MediaSample.SAMPLE_AUDIO, timestamp, true, offset, size);
	}
	
	public void getParameters() throws IOException {
		ByteBufferInputStream bi = new ByteBufferInputStream(data.duplicate());
		byte b = bi.readByte();
		soundFormat = (b & 0xF0) >>> 4;
		soundRate = (b & 0x0C) >>> 2;
		soundRate = ((b & 0x02) >>> 1) == 0 ? 8 : 16;
		soundType = b & 0x01;
	}

	public int getSoundFormat() {
		return soundFormat;
	}

	public int getSoundRate() {
		return soundRate;
	}

	public int getSoundSize() {
		return soundSize;
	}

	public int getSoundType() {
		return soundType;
	}	
}
