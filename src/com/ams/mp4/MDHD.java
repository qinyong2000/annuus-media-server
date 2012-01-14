package com.ams.mp4;

import java.io.DataInputStream;
import java.io.IOException;

public final class MDHD extends BOX {
	private long creationTime;
	private long modificationTime;
	private int timeScale;
	private long duration;
	private String language;

	public MDHD(int version) {
		super(version);
	}
	
	public void read(DataInputStream in) throws IOException {
		if (version == 0) {
			creationTime = in.readInt();
			modificationTime = in.readInt();
		} else {
			creationTime = in.readLong();
			modificationTime = in.readLong();
		}
		timeScale = in.readInt();
		if (version == 0) {
			duration = in.readInt();
		} else {
			duration = in.readLong();
		}
		short l = in.readShort();
		byte[] b = new byte[3];
		b[0] = (byte) (0x60 + (l & 0x1F));
		l >>>=5;
		b[1] = (byte) (0x60 + (l & 0x1F));
		l >>>=5;
		b[2] = (byte) (0x60 + (l & 0x1F));
		language = new String(b);
	}

	public long getCreationTime() {
		return creationTime;
	}

	public long getModificationTime() {
		return modificationTime;
	}

	public int getTimeScale() {
		return timeScale;
	}

	public long getDuration() {
		return duration;
	}

	public String getLanguage() {
		return language;
	}
}
