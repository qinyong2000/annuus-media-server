package com.ams.mp4;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import com.ams.util.Utils;

public class BOX {
	public static class Header {
		public String type; 
		public long headerSize;
		public long payloadSize;
	};
	protected int version;
	
	public BOX(int version) {
		super();
		this.version = version;
	}
	
	public static Header readHeader(InputStream in) throws IOException {
		Header header = new Header();
		byte[] b = new byte[4];
		int bytes = in.read(b, 0, 4);
		if (bytes == -1) throw new EOFException();
		long size = Utils.from32Bit(b);
		b = new byte[4];
		in.read(b, 0, 4);
		header.type = new String(b);
		header.headerSize = 8;
		if (size == 1) {	// extended size
			header.headerSize = 16;
			b = new byte[4];
			in.read(b, 0, 4);
			long size0 = Utils.from32Bit(b);
			in.read(b, 0, 4);
			size = (size0 << 32) + Utils.from32Bit(b);
		}
		header.payloadSize = size - header.headerSize;
		return header;
	}
	
}
