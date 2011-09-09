package com.ams.io;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.ams.util.ByteBufferHelper;

public class ByteBufferInputStream extends InputStream {
	protected IByteBufferReader reader = null;

	// buffer queue
	protected ByteBuffer[] buffers = null;
	protected int index = 0;
	protected int mark = -1;

	protected byte[] line = new byte[4096];

	public ByteBufferInputStream(ByteBuffer[] buffers) {
		this.buffers = buffers;
		this.reader = null;
	}

	public ByteBufferInputStream(IByteBufferReader reader) {
		this.buffers = null;
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
		if (buffers == null) {
			ByteBuffer[] buffers = reader.read(length);
			for(ByteBuffer buffer : buffers) {
				int size = buffer.remaining();
				buffer.get(data, offset, size);
				offset += size;
			}
			return length;
		}

		if (index >= buffers.length) {
			return -1;
		} else if (index == buffers.length - 1
				&& !buffers[index].hasRemaining()) {
			return -1;
		} else if (length == 0 || data.length == 0) {
			return (available() > 0 ? 0 : -1);
		}

		int readBytes = 0;
		while (index < buffers.length) {
			ByteBuffer buf = buffers[index];
			int size = Math.min(length - readBytes, buf.remaining());

			buf.get(data, offset + readBytes, size);
			readBytes += size;

			if (readBytes >= length)
				break;
			index++;
		}
		return (readBytes > 0 ? readBytes : -1);
	}

	public long skip(long n) {
		if (buffers == null)
			return -1;

		long cnt = 0;
		while (index < buffers.length) {
			ByteBuffer buf = buffers[index];
			long size = Math.min(buf.remaining(), n - cnt);
			buf.position(buf.position() + (int) size);
			cnt += size;
			if (cnt == n) {
				break;
			}
			index++;
		}
		return cnt;
	}

	public int available() {
		if (buffers == null)
			return -1;

		int available = 0;
		for (int i = buffers.length - 1; i >= index; i--) {
			available += buffers[i].remaining();
		}
		return available;
	}

	public void close() throws IOException {
		index = 0;
		mark = -1;
		buffers = null;
		// if (reader != null) reader.close();
	}

	public void mark(final int readlimit) {
		if (buffers == null)
			return;

		if (index < buffers.length) {
			mark = index;
			for (int i = buffers.length - 1; i >= mark; i--) {
				buffers[i].mark();
			}
		}
	}

	public void reset() throws IOException {
		if (buffers == null)
			return;

		if (mark != -1) {
			index = mark;
			for (int i = buffers.length - 1; i >= index; i--) {
				buffers[i].reset();
			}
			mark = -1;
		}
	}

	public boolean markSupported() {
		if (buffers == null)
			return false;
		return true;
	}

	public byte readByte() throws IOException {
		byte[] b = new byte[1];
		read(b, 0, 1);
		return b[0];
	}

	public int read16Bit() throws IOException {
		byte[] b = new byte[2];
		read(b, 0, 2); // 16Bit read
		return ((b[0] & 0xFF) << 8) | (b[1] & 0xFF);
	}

	public int read24Bit() throws IOException {
		byte[] b = new byte[3];
		read(b, 0, 3); // 24Bit read
		return ((b[0] & 0xFF) << 16) | ((b[1] & 0xFF) << 8) | (b[2] & 0xFF);
	}

	public long read32Bit() throws IOException {
		byte[] b = new byte[4];
		read(b, 0, 4); // 32Bit read
		return ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16)
				| ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
	}

	public int read16BitLittleEndian() throws IOException {
		byte[] b = new byte[2];
		read(b, 0, 2);
		// 16 Bit read, LITTLE-ENDIAN
		return ((b[1] & 0xFF) << 8) | (b[0] & 0xFF);
	}

	public int read24BitLittleEndian() throws IOException {
		byte[] b = new byte[3];
		read(b, 0, 3);
		// 24 Bit read, LITTLE-ENDIAN
		return ((b[2] & 0xFF) << 16) | ((b[1] & 0xFF) << 8) | (b[0] & 0xFF);
	}

	public long read32BitLittleEndian() throws IOException {
		byte[] b = new byte[4];
		read(b, 0, 4);
		// 32 Bit read, LITTLE-ENDIAN
		return ((b[3] & 0xFF) << 24) | ((b[2] & 0xFF) << 16)
				| ((b[1] & 0xFF) << 8) | (b[0] & 0xFF);
	}

	public ByteBuffer[] readByteBuffer(int size) throws IOException {
		if (reader != null) {
			return reader.read(size);
		} else {
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
					ByteBuffer slice = ByteBufferHelper.cut(buffer, length);
					list.add(slice);
					length = 0;
				}
			}
			return list.toArray(new ByteBuffer[list.size()]);
		}
	}

}
