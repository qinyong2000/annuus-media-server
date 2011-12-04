package com.ams.flv;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.ams.io.ByteBufferInputStream;
import com.ams.io.ByteBufferOutputStream;
import com.ams.util.ByteBufferHelper;

public class FlvTag {
	public static final int FLV_AUDIO = 0;
	public static final int FLV_VIDEO = 1;
	public static final int FLV_META = 2;

	protected int tagType;
	protected ByteBuffer[] data;
	protected long timestamp;

	public FlvTag(int tagType, ByteBuffer[] data, long timestamp) {
		super();
		this.tagType = tagType;
		this.data = data;
		this.timestamp = timestamp;
	}

	public ByteBuffer[] getData() {
		return data;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public int getTagType() {
		return tagType;
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
		// if( streamId != 0 ) {
		// throw new FlvException("Invalid stream Id: " + streamId);
		// }
		ByteBuffer[] data = in.readByteBuffer(dataSize);

		int previousTagSize = (int) in.read32Bit();
		// if( previousTagSize != 0 && previousTagSize != dataSize + 11 ) {
		// throw new FlvException("Invalid previous tag size (" + dataSize +
		// " != " + previousTagSize + ")");
		// }

		switch (tagType) {
		case 0x08:
			return new AudioTag(data, timestamp);
		case 0x09:
			return new VideoTag(data, timestamp);
		case 0x12:
			return new MetaTag(data, timestamp);
		default:
			throw new FlvException("Invalid FLV tag " + tagType);
		}
	}

	public static void write(ByteBufferOutputStream out, FlvTag flvTag)
			throws IOException {
		byte tagType = -1;
		switch (flvTag.getTagType()) {
		case FlvTag.FLV_AUDIO:
			tagType = 0x08;
			break;
		case FlvTag.FLV_VIDEO:
			tagType = 0x09;
			break;
		case FlvTag.FLV_META:
			tagType = 0x12;
			break;
		}
		// tag type
		out.writeByte(tagType);

		ByteBuffer[] data = flvTag.getData();
		// data size
		int dataSize = ByteBufferHelper.size(data);

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

	public boolean isVideoKeyFrame() {
		return (data[0].get(0) >>> 4) == 1;
	}

	public boolean isAudioTag() {
		return tagType == FLV_AUDIO;
	}
	
	public boolean isVideoTag() {
		return tagType == FLV_VIDEO;
	}

	public boolean isMetaTag() {
		return tagType == FLV_META;
	}

	public void getParameters() throws IOException {
	}
}
