package com.ams.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ams.io.IByteBufferReader;

public class ByteBufferArray implements IByteBufferReader, IByteBufferWriter {
	private List<ByteBuffer> buffers;
	private int index = 0;

	public ByteBufferArray() {
		this.buffers = new ArrayList<ByteBuffer>();
	}

	public ByteBufferArray(ArrayList<ByteBuffer> buffers) {
		if (buffers == null) throw new NullPointerException();
		this.buffers = buffers;
		init();
	}
	
	public ByteBufferArray(ByteBuffer[] buffers) {
		if (buffers == null) throw new NullPointerException();
		this.buffers = Arrays.asList(buffers);
		init();
	}
	
	private void init() {
		index = 0;
		for (ByteBuffer buf : buffers) {
			if (buf.hasRemaining()) break;
			index++;
		}
	}
	
	public ByteBuffer[] getBuffers() {
		return buffers.toArray(new ByteBuffer[buffers.size()]);
	}
	
	public boolean hasRemaining() {
		boolean hasRemaining = false;
		for (ByteBuffer buf : buffers) {
			if (buf.hasRemaining()) {
				hasRemaining = true;
				break;
			}
		}
		return hasRemaining;
	}

	public int size() {
		int dataSize = 0;
		for(ByteBuffer buf : buffers) {
			dataSize += buf.remaining();
		}
		return dataSize;
	}

	public ByteBufferArray duplicate() {
		ArrayList<ByteBuffer> dup = new ArrayList<ByteBuffer>();
		for(ByteBuffer buf : buffers) {
			dup.add(buf.duplicate());
		}
		return new ByteBufferArray(dup);
	}

	public ByteBuffer[] read(int size) throws IOException {
		if (index >= buffers.size()) return null;
		ArrayList<ByteBuffer> list = new ArrayList<ByteBuffer>();
		int length = size;
		while (length > 0 && index < buffers.size()) {
			// read a buffer
			ByteBuffer buffer = buffers.get(index);
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

	public void write(ByteBuffer[] data) throws IOException {
		for (ByteBuffer buf : data) {
			buffers.add(buf);
		}
	}
}
