package com.ams.util;

import java.nio.ByteBuffer;

public final class ByteBufferHelper {
	public static boolean hasRemaining(ByteBuffer[] buffers) {
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
	
	public static int size(ByteBuffer[] buffers) {
		if (buffers == null) return 0;
		int dataSize = 0;
		for(ByteBuffer buf : buffers) {
			dataSize += buf.remaining();
		}
		return dataSize;
	}

	public static ByteBuffer[] duplicate(ByteBuffer[] buffers) {
		if (buffers == null) {
			return null;
		}
		ByteBuffer[] bufferDup = new ByteBuffer[buffers.length];
		for(int i =0 ; i < bufferDup.length; i++) {
			bufferDup[i] = buffers[i].duplicate();
		}
		return bufferDup;
	}
	
	public static ByteBuffer cut(ByteBuffer buffer, int length) {
		ByteBuffer slice = buffer.slice();
		slice.limit(length);
		buffer.position(buffer.position() + length);
		return slice;
	}
	
}
