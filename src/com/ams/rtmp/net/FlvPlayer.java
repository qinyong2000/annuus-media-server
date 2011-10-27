package com.ams.rtmp.net;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
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
	
	public void seek(long seekTime) throws IOException {
		// reset infos
		long now = System.currentTimeMillis();
		startTime =  now - BUFFER_TIME - seekTime;
		if( pausedTime != -1 ) {
			pausedTime = now;
		}
		blockedTime = -1;

		// close the flv file
		readMsgQueue.clear();
		
		try {
			FlvTag flvTag = deserializer.seek(seekTime);
			if (flvTag == null) return;
			ByteBuffer[] data = flvTag.getData();
			long time = flvTag.getTimestamp();
			switch( flvTag.getTagType() ) {
			case FlvTag.FLV_AUDIO:
				if(audioPlaying) {
					readMsgQueue.offer(new MediaMessage(time, new RtmpMessageAudio(data)));
				}
				break;
			case FlvTag.FLV_VIDEO:
				if(flvTag.isVideoKeyFrame()) {
					readMsgQueue.clear();
				}
				if(videoPlaying) {
					readMsgQueue.offer(new MediaMessage(time, new RtmpMessageVideo(data)));
				}
				break;
			case FlvTag.FLV_META:
				readMsgQueue.offer(new MediaMessage(time, new RtmpMessageData(data)));
			}
		} catch(FlvException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage());
		}
		
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
		
		while (true) {
			MediaMessage msg = readMsgQueue.poll();
			if (msg == null) {
				break;
			}
			stream.writeMessage(msg.getTimestamp(), (RtmpMessage)msg.getData());
			currentTime =  msg.getTimestamp();
			if( stream.isWriteBlocking() ) {
				blockedTime = now;
				return;
			}
		}
		
		try {
			long relativeTime = now - startTime;
			while(relativeTime > currentTime) {
				FlvTag flvTag = deserializer.readNext();
				if( flvTag == null ) {	// eof
					stream.setPlayer(null);
					throw new EOFException();
				}

				ByteBuffer[] data = flvTag.getData();
				long timeStamp = flvTag.getTimestamp();
				currentTime = timeStamp;
				switch(flvTag.getTagType()) {
				case FlvTag.FLV_AUDIO:
					if(audioPlaying) {
						stream.writeMessage(timeStamp, new RtmpMessageAudio(data));
					}
					break;
				case FlvTag.FLV_VIDEO:
					if(videoPlaying) {
						stream.writeMessage(timeStamp, new RtmpMessageVideo(data));
					}
					break;
				case FlvTag.FLV_META:
					stream.writeMessage(timeStamp, new RtmpMessageData(data));
					break;

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
