package com.ams.mp4;

import java.io.DataInputStream;
import java.io.IOException;

public final class STSS {
	private int[] syncTable;

	public void read(DataInputStream in) throws IOException {
		int count = in.readInt();
		syncTable = new int[count];
		for (int i=0; i < count; i++) {
			syncTable[i] = in.readInt();
		}
	}
	
}
