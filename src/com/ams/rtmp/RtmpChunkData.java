package com.ams.rtmp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.ams.io.ByteBufferArray;
import com.ams.io.ByteBufferInputStream;

class RtmpChunkData {
	private RtmpHeader header;
	private ArrayList<ByteBuffer> chunkData = new ArrayList<ByteBuffer>(); 
	private int chunkSize;

	public RtmpChunkData(RtmpHeader header) {
		this.header = header;
		this.chunkSize = header.getSize();
	}
	
	public void read(ByteBufferInputStream in, int length) throws IOException {
		ByteBuffer[] buffers = in.readByteBuffer(length);
		if (buffers != null) {
			for (ByteBuffer buffer : buffers) {
				chunkData.add(buffer);
			}
		}
		
		chunkSize -= length;
	}

	public ByteBufferArray getChunkData() {
		return new ByteBufferArray(chunkData.toArray(new ByteBuffer[chunkData.size()]));
	}

	public int getRemainBytes() {
		return chunkSize;
	}

	public RtmpHeader getHeader() {
		return header;
	}
}
