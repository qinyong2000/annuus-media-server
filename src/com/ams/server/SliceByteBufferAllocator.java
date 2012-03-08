package com.ams.server;

import java.nio.ByteBuffer;

public class SliceByteBufferAllocator {
	private ByteBuffer byteBuffer = null;
	private int poolSize = 8192 * 1024;

	public synchronized ByteBuffer allocate(int size) {
		if (byteBuffer == null
				|| byteBuffer.capacity() - byteBuffer.limit() < size) {
			byteBuffer = ByteBuffer.allocateDirect(poolSize);
		}
		byteBuffer.limit(byteBuffer.position() + size);
		ByteBuffer slice = byteBuffer.slice();
		byteBuffer.position(byteBuffer.limit());
		return slice;
	}

	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
		byteBuffer = null;
	}
}
