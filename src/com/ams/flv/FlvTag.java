package com.ams.flv;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import com.ams.io.ByteBufferArray;
import com.ams.io.ByteBufferInputStream;
import com.ams.io.ByteBufferOutputStream;
import com.ams.io.RandomAccessFileReader;

public class FlvTag extends Sample {

	public FlvTag(int tagType, ByteBufferArray data, long timestamp) {
		super(tagType, data, timestamp);
	}

	public FlvTag(int tagType, long offset, int size, boolean keyframe, long timestamp) {
		super(tagType, offset, size, keyframe, timestamp);
	}

	public static FlvTag read(ByteBufferInputStream in) throws IOException,
			FlvException {
		int tagType;
		try {
			tagType = in.readByte() & 0xFF;
		} catch (EOFException e) {
			return null;
		}
		int dataSize = in.read24Bit(); // 24Bit read
		long timestamp = in.read24Bit(); // 24Bit read
		timestamp |= (in.readByte() & 0xFF) << 24; // time stamp extended

		int streamId = in.read24Bit(); // 24Bit read
		ByteBuffer[] data = in.readByteBuffer(dataSize);

		int previousTagSize = (int) in.read32Bit();

		switch (tagType) {
		case 0x08:
			return new AudioTag(new ByteBufferArray(data), timestamp);
		case 0x09:
			return new VideoTag(new ByteBufferArray(data), timestamp);
		case 0x12:
			return new MetaTag(new ByteBufferArray(data), timestamp);
		default:
			throw new FlvException("Invalid FLV tag " + tagType);
		}
	}

	public static FlvTag read(RandomAccessFileReader reader) throws IOException, FlvException {
		int tagType;
		ByteBufferInputStream in = new ByteBufferInputStream(reader);
		try {
			tagType = in.readByte() & 0xFF;
		} catch (EOFException e) {
			return null;
		}
		int dataSize = in.read24Bit(); // 24Bit read
		long timestamp = in.read24Bit(); // 24Bit read
		timestamp |= (in.readByte() & 0xFF) << 24; // time stamp extended
		
		int streamId = in.read24Bit(); // 24Bit read
		long offset = reader.getPosition();
		
		int header = in.readByte();
		boolean keyframe = (header >>> 4) == 1;
		
		reader.seek(offset + dataSize);
		int previousTagSize = (int) in.read32Bit();
		switch (tagType) {
		case 0x08:
			return new AudioTag(offset, dataSize, timestamp);
		case 0x09:
			return new VideoTag(offset, dataSize, keyframe, timestamp);
		case 0x12:
			return new MetaTag(offset, dataSize, timestamp);
		default:
			throw new FlvException("Invalid FLV tag " + tagType);
		}
	}
	
	public void readData(RandomAccessFileReader reader) throws IOException {
		reader.seek(offset);
		data = new ByteBufferArray(reader.read(size));
	}
	
	public static void write(ByteBufferOutputStream out, FlvTag flvTag)
			throws IOException {
		byte tagType = -1;
		switch (flvTag.getSampleType()) {
		case Sample.SAMPLE_AUDIO:
			tagType = 0x08;
			break;
		case Sample.SAMPLE_VIDEO:
			tagType = 0x09;
			break;
		case Sample.SAMPLE_META:
			tagType = 0x12;
			break;
		}
		// tag type
		out.writeByte(tagType);

		ByteBufferArray data = flvTag.getData();
		// data size
		int dataSize = data.size();

		out.write24Bit(dataSize); // 24Bit write
		// time stamp
		int timestamp = (int) flvTag.getTimestamp();
		out.write24Bit(timestamp); // 24Bit write
		out.writeByte((byte) ((timestamp & 0xFF000000) >>> 32));
		// stream ID
		out.write24Bit(0);
		// data
		out.writeByteBuffer(data);
		// previousTagSize
		out.write32Bit(dataSize + 11);
	}

	public void getParameters() throws IOException {
	}
}
