package com.ams.server;

import java.nio.ByteBuffer;

public interface IByteBufferAllocator {
	ByteBuffer allocate(int size);
}
