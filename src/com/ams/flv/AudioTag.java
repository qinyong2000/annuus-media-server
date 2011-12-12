package com.ams.flv;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.ams.io.ByteBufferInputStream;
import com.ams.util.ByteBufferHelper;

public class AudioTag extends FlvTag {
	private int soundFormat = -1;
	private int soundRate = 0;
	private int soundSize = 0;
	private int soundType = -1;
	
	public AudioTag(ByteBuffer[] data, long timestamp) {
		super(FlvTag.FLV_AUDIO, data, timestamp);
	}

	public AudioTag(long offset, int size, long timestamp) {
		super(FlvTag.FLV_AUDIO, offset, size, true, timestamp);
	}
	
	public void getParameters() throws IOException {
		ByteBufferInputStream bi = new ByteBufferInputStream(ByteBufferHelper.duplicate(data));
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
