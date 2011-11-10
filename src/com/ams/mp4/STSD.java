package com.ams.mp4;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.ams.io.ByteBufferOutputStream;

public final class STSD {
	private SampleDescription[] descriptions;
	public final class SampleDescription {
		public String type;
		public byte[] description;
	}
	
	public void read(DataInputStream in) throws IOException {
		int count = in.readInt();
		descriptions = new SampleDescription[count];
		for (int i = 0 ; i < count; i++) {
			int length = in.readInt();
			byte[] b = new byte[4];
			in.read(b);
			String type = new String(b);
			byte[] description = new byte[length];
			in.read(description);
			descriptions[i] = new SampleDescription();
			descriptions[i].type = type;
			descriptions[i].description = description; 
		}
	}

	public SampleDescription[] getDescriptions() {
		return descriptions;
	}
	
	public static ByteBuffer[] getVideoDecoderConfigure(SampleDescription desc) throws IOException {
		ByteBufferOutputStream bos = new ByteBufferOutputStream();
		int pos = 78; // read avcC box
		byte[] b = desc.description;
		int size = ((b[pos] & 0xFF) << 24) | ((b[pos + 1] & 0xFF) << 16) | ((b[pos + 2] & 0xFF) << 8) | (b[pos + 3] & 0xFF);
		//video decoder config
		bos.write(b, pos + 8, size - 8);
		return bos.toByteBufferArray();
	}
	
}
