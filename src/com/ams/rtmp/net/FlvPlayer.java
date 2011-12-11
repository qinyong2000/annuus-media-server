package com.ams.rtmp.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import com.ams.amf.AmfValue;
import com.ams.flv.*;
import com.ams.io.*;
import com.ams.message.*;
import com.ams.rtmp.message.*;

public class FlvPlayer implements IPlayer{
	private static int BUFFER_TIME = 2 * 1000; // x seconds of buffering
	private NetStream stream = null;
	private RandomAccessFileReader reader;
	private FlvDeserializer deserializer;
	private long startTime = -1;
	private long currentTime = 0;
	private long blockedTime = -1;
	private long pausedTime = -1;
	private boolean audioPlaying = true;
	private boolean videoPlaying = true;
	private LinkedList<MediaMessage> readMsgQueue;

	public FlvPlayer(String fileName, NetStream stream) throws IOException {
		this.stream = stream;
		this.reader = new RandomAccessFileReader(fileName, 0);
		this.deserializer = new FlvDeserializer(reader);
		this.readMsgQueue = new LinkedList<MediaMessage>();
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
		long now = System.currentTimeMillis();
		startTime =  now - BUFFER_TIME - seekTime;
		if( pausedTime != -1 ) {
			pausedTime = now;
		}
		blockedTime = -1;

		readMsgQueue.clear();
		try {
			deserializer.seek(seekTime);
		} catch(FlvException e) {
			throw new IOException(e.getMessage());
		}
		writeStartData();
	}
	

	public void play() throws IOException {
		long now = System.currentTimeMillis();
		if (pausedTime != -1) {
			return;
		}
		if (blockedTime != -1) {
			startTime += (now - blockedTime);
			blockedTime = -1;
		}
		
		try {
			while(now - startTime > currentTime) {
				FlvTag flvTag = deserializer.readNext();
				if( flvTag == null ) {	// eof
					stream.setPlayer(null);
					return;
				}

				ByteBuffer[] data = flvTag.getData();
				long timeStamp = flvTag.getTimestamp();
				currentTime = timeStamp;
				if (flvTag.isAudioTag() && audioPlaying) {
					stream.writeMessage(timeStamp, new RtmpMessageAudio(data));
				}
				if (flvTag.isVideoTag() && videoPlaying) {
					stream.writeMessage(timeStamp, new RtmpMessageVideo(data));
				}
				if (flvTag.isMetaTag()) {
					stream.writeMessage(timeStamp, new RtmpMessageData(data));
				}
	
				if( stream.isWriteBlocking() ) {
					blockedTime = now;
					return;
				}
			}
		} catch(FlvException e) {
			throw new IOException(e.getMessage());
		}
	}
	
	public void pause(boolean pause) {
		if (pause) {
			long now = System.currentTimeMillis();
			if( pausedTime == -1 ) {
				pausedTime = now;
			}
		} else {
			if( pausedTime != -1 ) {
				pausedTime = -1;
			}
		}
	}

	public boolean isPaused() {
		return pausedTime != -1;
	}

	public void audioPlaying(boolean flag) {
		this.audioPlaying = flag;
		
	}

	public void videoPlaying(boolean flag) {
		this.videoPlaying = flag;
	}

}
