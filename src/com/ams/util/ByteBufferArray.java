package com.ams.util;

import java.nio.ByteBuffer;

public class ByteBufferArray {
	private ByteBuffer[] buffers;
	
	public ByteBufferArray(ByteBuffer[] buffers) {
		this.buffers = buffers;
	}
	
	public ByteBuffer[] getBuffers() {
		return buffers;
	}
	
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
		for(int i =0 ; i < bufferDup.length; i++) {
			bufferDup[i] = buffers[i].duplicate();
		}
		return new ByteBufferArray(bufferDup);
	}
	
//	public ByteBufferArray get(int length) {
//		ByteBuffer slice = buffer.slice();
//		slice.limit(length);
//		buffer.position(buffer.position() + length);
//		return slice;
//	}
	
}
