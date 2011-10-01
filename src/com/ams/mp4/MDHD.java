package com.ams.mp4;

import java.io.DataInputStream;
import java.io.IOException;

public final class MDHD {
	private int crationTime;
	private int modificationTime;
	private int timeScale;
	private int duration;
	private String language;

	public void read(DataInputStream in) throws IOException {
		crationTime = in.readInt();
		modificationTime = in.readInt();
		timeScale = in.readInt();
		duration = in.readInt();
	}

	public int getCrationTime() {
		return crationTime;
	}

	public int getModificationTime() {
		return modificationTime;
	}

	public int getTimeScale() {
		return timeScale;
	}

	public int getDuration() {
		return duration;
	}

	public String getLanguage() {
		return language;
	}
}
