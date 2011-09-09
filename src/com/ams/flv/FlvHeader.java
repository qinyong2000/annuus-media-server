package com.ams.flv;

import java.io.IOException;

import com.ams.io.ByteBufferInputStream;
import com.ams.io.ByteBufferOutputStream;

public class FlvHeader {
	private byte[] signature = { 'F', 'L', 'V' };
	private int version = 0x01;
	private boolean hasAudio;
	private boolean hasVideo;
	private int dataOffset = 0x09;

	public FlvHeader(boolean hasAudio, boolean hasVideo) {
		this.hasAudio = hasAudio;
		this.hasVideo = hasVideo;
	}

	public boolean isHasAudio() {
		return hasAudio;
	}

	public boolean isHasVideo() {
		return hasVideo;
	}

	public byte[] getSignature() {
		return signature;
	}

	public int getVersion() {
		return version;
	}

	public int getDataOffset() {
		return dataOffset;
	}

	public static FlvHeader read(ByteBufferInputStream in) throws IOException,
			FlvException {
		int b1 = in.readByte() & 0xFF;
		int b2 = in.readByte() & 0xFF;
		int b3 = in.readByte() & 0xFF;
		if (b1 != 'F' || b2 != 'L' || b3 != 'V')
			throw new FlvException("Invalid signature");
		int version = in.readByte() & 0xFF;
		if (version != 0x01)
			throw new FlvException("Invalid version");
		int flags = in.readByte() & 0xFF;
		if ((flags & 0xF2) != 0)
			throw new FlvException("Invalid tag type flags: " + flags);
		int dataOffset = (int) in.read32Bit();
		if (dataOffset != 0x09)
			throw new FlvException("Invalid data offset: " + dataOffset);
		int previousTagSize0 = (int) in.read32Bit();
		if (previousTagSize0 != 0)
			throw new FlvException("Invalid previous tag size 0: "
					+ previousTagSize0);

		boolean hasAudio = (flags & 1) != 1;
		boolean hasVideo = (flags & 4) != 1;

		return new FlvHeader(hasAudio, hasVideo);
	}

	public static void write(ByteBufferOutputStream out, FlvHeader header)
			throws IOException {
		out.writeByte('F');
		out.writeByte('L');
		out.writeByte('V');
		out.writeByte(0x01); // version
		int flgs = 0;
		flgs += header.isHasAudio() ? 1 : 0;
		flgs += header.isHasVideo() ? 4 : 0;
		out.writeByte(flgs & 0xFF); // hasAudio && hasVideo
		out.write32Bit(0x09); // dataOffset
		out.write32Bit(0x00); // previousTagSize0
	}

}