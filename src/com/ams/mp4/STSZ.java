package com.ams.mp4;

import java.io.DataInputStream;
import java.io.IOException;

public final class STSZ {
	private int constantSize;
	private int[] sizeTable;
	
	public void read(DataInputStream in) throws IOException {
		constantSize = in.readInt();
		int sizeCount  = in.readInt();
		if (sizeCount > 0) {
			sizeTable = new int[sizeCount];
			for (int i = 0; i < sizeCount; i++) {
				sizeTable[i] = in.readInt();
			}
		}
	}

	public int getConstantSize() {
		return constantSize;
	}

	public int[] getSizeTable() {
		return sizeTable;
	}
}
