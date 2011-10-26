package com.ams.mp4;

import java.io.DataInputStream;
import java.io.IOException;

public final class STSC {
	public final class STSCRecord {
		public int firstChunk;
		public int samplesPerChunk;
		public int sampleDescIndex;
	}

	private STSCRecord[] entries;
	
	public void read(DataInputStream in) throws IOException {
		int count = in.readInt();
		entries = new STSCRecord[count];
		for (int i=0; i < count; i++) {
			entries[i] = new STSCRecord();
			entries[i].firstChunk = in.readInt();
			entries[i].samplesPerChunk = in.readInt();
			entries[i].sampleDescIndex = in.readInt();
		}
	}

	public STSCRecord[] getEntries() {
		return entries;
	}
	
}
