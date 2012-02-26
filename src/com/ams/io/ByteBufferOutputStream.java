package com.ams.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import com.ams.server.ByteBufferFactory;
import com.ams.util.Utils;

public class ByteBufferOutputStream extends OutputStream {
	protected static final int WRITE_BUFFER_SIZE = 512;
	protected ByteBuffer writeBuffer = null;
	protected IByteBufferWriter writer = null;

	public ByteBufferOutputStream(IByteBufferWriter writer) {
		this.writer = writer;
	}

	public void flush() throws IOException {
		if (writeBuffer != null) {
			writeBuffer.flip();
			writer.write(new ByteBuffer[] {writeBuffer});
			writeBuffer = null;
		}
	}
	
	public void write(byte[] data, int offset, int len) throws IOException {
		while (true) {
			if (writeBuffer == null) {
				int size = Math.max(len, WRITE_BUFFER_SIZE);
				writeBuffer = ByteBufferFactory.allocate(size);
			}
			if (writeBuffer.remaining() >= len) {
				writeBuffer.put(data, offset, len);
				break;
			}
			flush();
		}
	}

	public void write(byte[] data) throws IOException {
		write(data, 0, data.length);
	}

	public void write(int data) throws IOException {
		byte[] b = new byte[1];
		b[0] = (byte) (data & 0xFF);
		write(b, 0, 1);
	}

	public void writeByte(int v) throws IOException {
		byte[] b = new byte[1];
		b[0] = (byte) (v & 0xFF);
		write(b, 0, 1);
	}

	public void write16Bit(int v) throws IOException {
		write(Utils.to16Bit(v));
	}

	public void write24Bit(int v) throws IOException {
		write(Utils.to24Bit(v)); // 24bit
	}

	public void write32Bit(long v) throws IOException {
		write(Utils.to32Bit(v)); // 32bit
	}

	public void write16BitLittleEndian(int v) throws IOException {
		// 16bit write, LITTLE-ENDIAN
		write(Utils.to16BitLittleEndian(v));
	}

	public void write24BitLittleEndian(int v) throws IOException {
		write(Utils.to24BitLittleEndian(v)); // 24bit
	}

	public void write32BitLittleEndian(long v) throws IOException {
		// 32bit write, LITTLE-ENDIAN
		write(Utils.to32BitLittleEndian(v));
	}

	public void writeByteBuffer(ByteBufferArray data) throws IOException {
		writeByteBuffer(data.getBuffers());
	}
	
	public void writeByteBuffer(ByteBuffer[] data) throws IOException {
		flush();
		writer.write(data);
	}

}
