package com.ams.io;

import java.io.*;
import java.nio.ByteBuffer;
import com.ams.util.Utils;

public class ByteBufferInputStream extends InputStream {
	protected IByteBufferReader reader = null;
	protected byte[] line = new byte[4096];

	public ByteBufferInputStream(ByteBuffer[] buffers) {
		this.reader = new ByteBufferArray(buffers);
	}

	public ByteBufferInputStream(IByteBufferReader reader) {
//		this.buffers = null;
		this.reader = reader;
	}

	public String readLine() throws IOException {
		// throw an exception if the stream is closed
		// closedCheck();
		int index = 0;
		boolean marked = false;
		while (true) {
			int c = read();
			if (c != -1) {
				byte ch = (byte) c;
				if (ch == '\r') { // expect next byte is CR
					marked = true;
				} else if (ch == '\n') { // have read a line, exit
					if (marked)
						index--;
					break;
				} else {
					marked = false;
				}
				line[index++] = ch;

				// need to expand the line buffer
				int capacity = line.length;
				if (index >= capacity) {
					capacity = capacity * 2 + 1;
					byte[] tmp = new byte[capacity];
					System.arraycopy(line, 0, tmp, 0, index);
					line = tmp;
				}

			} else {
				if (marked) {
					index--;
				}
				break;
			}

		} // while

		return new String(line, 0, index, "UTF-8");
	}

	public int read() throws IOException {
		byte[] one = new byte[1];

		// read 1 byte
		int amount = read(one, 0, 1);
		// return EOF / the byte
		return (amount < 0) ? -1 : one[0] & 0xff;
	}

	public int read(byte data[], int offset, int length) throws IOException {
		// check parameters
		if (data == null) {
			throw new NullPointerException();
		} else if ((offset < 0) || (offset + length > data.length)
				|| (length < 0)) { // check indices
			throw new IndexOutOfBoundsException();
		}
		ByteBuffer[] buffers = reader.read(length);
		for(ByteBuffer buffer : buffers) {
			int size = buffer.remaining();
			buffer.get(data, offset, size);
			offset += size;
		}
		return length;
	}

	public byte readByte() throws IOException {
		byte[] b = new byte[1];
		read(b, 0, 1);
		return b[0];
	}

	public int read16Bit() throws IOException {
		byte[] b = new byte[2];
		read(b, 0, 2); // 16Bit read
		return Utils.from16Bit(b);
	}

	public int read24Bit() throws IOException {
		byte[] b = new byte[3];
		read(b, 0, 3); // 24Bit read
		return Utils.from24Bit(b);
	}

	public long read32Bit() throws IOException {
		byte[] b = new byte[4];
		read(b, 0, 4); // 32Bit read
		return Utils.from32Bit(b);
	}

	public int read16BitLittleEndian() throws IOException {
		byte[] b = new byte[2];
		read(b, 0, 2);
		// 16 Bit read, LITTLE-ENDIAN
		return Utils.from16BitLittleEndian(b);
	}

	public int read24BitLittleEndian() throws IOException {
		byte[] b = new byte[3];
		read(b, 0, 3);
		// 24 Bit read, LITTLE-ENDIAN
		return Utils.from24BitLittleEndian(b);
	}

	public long read32BitLittleEndian() throws IOException {
		byte[] b = new byte[4];
		read(b, 0, 4);
		// 32 Bit read, LITTLE-ENDIAN
		return Utils.from32BitLittleEndian(b);
	}

	public ByteBuffer[] readByteBuffer(int size) throws IOException {
			return reader.read(size);
	}

}
