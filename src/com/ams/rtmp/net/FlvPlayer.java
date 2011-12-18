package com.ams.rtmp.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import com.ams.amf.AmfValue;
import com.ams.flv.*;
import com.ams.io.*;
import com.ams.rtmp.message.*;

public class FlvPlayer implements IPlayer{
	private static int BUFFER_TIME = 5 * 1000; // x seconds of buffering
	private NetStream stream = null;
	private RandomAccessFileReader reader;
	private FlvDeserializer deserializer;
	private long startTime = -1;
	private long currentTime = 0;
	private long bufferTime = BUFFER_TIME;
	private boolean pause = false;
	private boolean audioPlaying = true;
	private boolean videoPlaying = true;

	public FlvPlayer(String fileName, NetStream stream) throws IOException {
		this.stream = stream;
		this.reader = new RandomAccessFileReader(fileName, 0);
		this.deserializer = new FlvDeserializer(reader);
	}

	public void close() throws IOException {
		reader.close();
	}
	
	public void writeStartData() throws IOException {
		//|RtmpSampleAccess
		stream.writeDataMessage(AmfValue.array("|RtmpSampleAccess", false, false));
		
		//NetStream.Data.Start
		stream.writeDataMessage(AmfValue.array("onStatus", AmfValue.newObject().put("code", "NetStream.Data.Start")));
		
		AmfValue value = deserializer.onMetaData();
		stream.writeDataMessage(AmfValue.array("onMetaData", value));
	}
	
	public void seek(long seekTime) throws IOException {
		FlvTag flvTag = null;
		try {
			flvTag = deserializer.seek(seekTime);
		} catch(FlvException e) {
			throw new IOException(e.getMessage());
		}
		if (flvTag == null) return;
		currentTime = flvTag.getTimestamp();
		startTime =  System.currentTimeMillis() - bufferTime - currentTime;
		writeStartData();
	}
	

	public void play() throws IOException {
		if (pause) return;

		long time = System.currentTimeMillis() - startTime;
		try {
			while(currentTime < time ) {
				FlvTag flvTag = deserializer.readNext();
				if( flvTag == null ) {	// eof
					stream.setPlayer(null);
					break;
				}
				currentTime = flvTag.getTimestamp();
				ByteBuffer[] data = flvTag.getData();
				if (flvTag.isAudioTag() && audioPlaying) {
					stream.writeMessage(currentTime, new RtmpMessageAudio(data));
				}
				if (flvTag.isVideoTag() && videoPlaying) {
					stream.writeMessage(currentTime, new RtmpMessageVideo(data));
				}
				if (flvTag.isMetaTag()) {
					stream.writeMessage(currentTime, new RtmpMessageData(data));
				}
	
			}
		} catch(FlvException e) {
			throw new IOException(e.getMessage());
		}
	}
	
	public void pause(boolean pause) {
		this.pause = pause;
	}

	public boolean isPaused() {
		return pause;
	}

	public void audioPlaying(boolean flag) {
		this.audioPlaying = flag;
		
	}

	public void videoPlaying(boolean flag) {
		this.videoPlaying = flag;
	}

	public void setBufferTime(long bufferTime) {
		this.bufferTime = bufferTime;
	}

}
