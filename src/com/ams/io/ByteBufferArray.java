package com.ams.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.ams.io.IByteBufferReader;

public class ByteBufferArray implements IByteBufferReader {
	private ByteBuffer[] buffers;
	private int index = 0;
	
	public ByteBufferArray(ByteBuffer[] buffers) {
		this.buffers = buffers;
	}
	
	public ByteBuffer[] getBuffers() {
		return buffers;
	}
/*	
	public boolean hasRemaining() {
		boolean hasRemaining = false;
		if (buffers != null) {
			for (ByteBuffer buf : buffers) {
				if (buf.hasRemaining()) {
					hasRemaining = true;
					break;
				}
			}
		}
		return hasRemaining;
	}
*/	
	public int size() {
		if (buffers == null) return 0;
		int dataSize = 0;
		for(ByteBuffer buf : buffers) {
			dataSize += buf.remaining();
		}
		return dataSize;
	}

	public ByteBufferArray duplicate() {
		if (buffers == null) {
			return null;
		}
		ByteBuffer[] bufferDup = new ByteBuffer[buffers.length];
		for(int i = 0 ; i < bufferDup.length; i++) {
			bufferDup[i] = buffers[i].duplicate();
		}
		return new ByteBufferArray(bufferDup);
	}

	public ByteBuffer[] read(int size) throws IOException {
		ArrayList<ByteBuffer> list = new ArrayList<ByteBuffer>();
		int length = size;
		while (length > 0 && index < buffers.length) {
			// read a buffer
			ByteBuffer buffer = buffers[index];
			int remain = buffer.remaining();
			if (length >= remain) {
				list.add(buffer);
				index++;
				length -= remain;
			} else {
				ByteBuffer slice = buffer.slice();
				slice.limit(length);
				buffer.position(buffer.position() + length);
				list.add(slice);
				length = 0;
			}
		}
		return list.toArray(new ByteBuffer[list.size()]);
	}

	
//	public ByteBufferArray get(int length) {
//		ByteBuffer slice = buffer.slice();
//		slice.limit(length);
//		buffer.position(buffer.position() + length);
//		return slice;
//	}
	
}
