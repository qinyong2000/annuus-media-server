package com.ams.mp4;

import java.io.DataInputStream;
import java.io.IOException;

public final class STCO {
	private long[] offsets;

	public void read(DataInputStream in) throws IOException {
		int count = in.readInt();
		offsets = new long[count];
		for (int i=0; i < count; i++) {
			offsets[i] = (int) in.readInt();
		}
	}

	public void read64(DataInputStream in) throws IOException {
		int count = in.readInt();
		offsets = new long[count];
		for (int i=0; i < count; i++) {
			offsets[i] = in.readLong();
		}
	}
	
	public long[] getOffsets() {
		return offsets;
	}
}