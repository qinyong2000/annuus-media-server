package com.ams.rtmp.net;

import java.io.IOException;

public interface IPlayer {
	public void seek(long seekTime) throws IOException;
	public void play() throws IOException;
	public void pause(boolean pause);
	public boolean isPaused();
	public void close() throws IOException;
	public void audioPlaying(boolean flag);
	public void videoPlaying(boolean flag);
	
}
