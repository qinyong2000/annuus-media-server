package com.ams.server;

import java.nio.ByteBuffer;

public final class ByteBufferFactory {
	private static IByteBufferAllocator allocator = null;
	
	public static ByteBuffer allocate(int size) {
		if (allocator == null) {
			return ByteBuffer.allocateDirect(size);
		}
		return allocator.allocate(size);
	}

	public static void setAllocator(IByteBufferAllocator alloc) {
		allocator = alloc;
	}
	
}
