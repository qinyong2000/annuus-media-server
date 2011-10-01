package com.ams.mp4;

import java.io.DataInputStream;
import java.io.IOException;

import com.ams.io.ByteBufferInputStream;
import com.ams.io.RandomAccessFileReader;

public class Mp4Reader {
	public MOOV readMoov(RandomAccessFileReader reader) {
		MOOV moov = null;
		try {
			reader.seek(0);
			for(;;) {
				// find moov box
				byte[] b = new byte[4];
				reader.read(b, 0, 4);
				int size = ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16)
							| ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
				b = new byte[4];
				reader.read(b, 0, 4);
				String box = new String(b);
				if ("moov".equalsIgnoreCase(box)) {
					ByteBufferInputStream in = new ByteBufferInputStream(reader);
					moov = new MOOV();
					moov.read(new DataInputStream(in));
					break;
				} else {
					reader.skip(size);
				}
			}
		} catch(IOException e) {
			moov = null;
		}	
		return moov;
	}	
}
