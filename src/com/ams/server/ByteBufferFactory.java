package com.ams.server;

import java.nio.ByteBuffer;

public final class ByteBufferFactory {
	private static ByteBufferAllocator allocator = new ByteBufferAllocator();
	
	public static ByteBuffer allocate(int size) {
		return allocator.allocate(size);
	}

	public static void setPageSize(int pageSize) {
		allocator.setPageSize(pageSize);
	}
}
