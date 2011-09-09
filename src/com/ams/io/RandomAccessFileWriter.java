package com.ams.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class RandomAccessFileWriter implements IByteBufferWriter {
	private RandomAccessFile file;
	private FileChannel channel = null;

	private RandomAccessFile openFile(String fileName) throws IOException {
		RandomAccessFile raFile = null;
		try {
			raFile = new RandomAccessFile(new File(fileName), "rw");
		} catch (Exception e) {
			if (raFile != null) {
				raFile.close();
				throw new IOException("Corrupted File '" + fileName + "'");
			}
			throw new IOException("File not found '" + fileName + "'");
		}
		return raFile;
	}

	public RandomAccessFileWriter(String fileName, boolean append)
			throws IOException {
		this.file = openFile(fileName);
		if (append) {
			this.file.seek(this.file.length());
		}
		this.channel = file.getChannel();
	}

	public void write(ByteBuffer[] data) throws IOException {
		channel.write(data);
	}

	public void close() throws IOException {
		file.close();
	}

	public void write(byte[] data, int offset, int len) throws IOException {
	}

}
