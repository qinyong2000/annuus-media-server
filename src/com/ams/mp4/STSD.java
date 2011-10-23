package com.ams.mp4;

import java.io.DataInputStream;
import java.io.IOException;

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
			descriptions[i].type = type;
			descriptions[i].description = description; 
		}
	}

	public SampleDescription[] getDescriptions() {
		return descriptions;
	}
	
}
