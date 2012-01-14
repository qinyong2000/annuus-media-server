package com.ams.mp4;

import java.io.DataInputStream;
import java.io.IOException;

public final class STTS extends BOX {
	public final class STTSRecord {
		public int sampleCount;
		public int sampleDelta;
	}
	private STTSRecord[] entries;

	public STTS(int version) {
		super(version);
	}
	
	public void read(DataInputStream in) throws IOException {
		int count = in.readInt();
		entries = new STTSRecord[count];
		for (int i=0; i < count; i++) {
			entries[i] = new STTSRecord();
			entries[i].sampleCount = in.readInt();
			entries[i].sampleDelta = in.readInt();
		}
	}

	public STTSRecord[] getEntries() {
		return entries;
	}
	
}
