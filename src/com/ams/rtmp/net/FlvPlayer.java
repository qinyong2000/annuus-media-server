package com.ams.rtmp.net;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import com.ams.flv.*;
import com.ams.io.*;
import com.ams.rtmp.message.*;
import com.ams.event.*;

public class FlvPlayer implements IPlayer{
	private static int FLV_BUFFER_TIME = 5 * 1000; // 5 seconds of buffering
	private NetStream stream = null;
	private String fileName = null;
	private ByteBufferInputStream input = null;
	private long startTime = -1;
	private long currentTime = 0;
	private long blockedTime = -1;
	private long pausedTime = -1;
	private boolean audioPlaying = true;
	private boolean videoPlaying = true;
	private LinkedList<Event> readMsgQueue;

	public FlvPlayer(String fileName, NetStream stream) {
		this.fileName = fileName;
		this.stream = stream;
		this.readMsgQueue = new LinkedList<Event>();
	}

	public void close() throws IOException {
		if (input != null) {
			input.close();
		}
		input = null;
	}
	
	public void seek(long seekTime) throws IOException {
		// reset infos
		long now = System.currentTimeMillis();
		startTime =  now - FLV_BUFFER_TIME - seekTime;
		if( pausedTime != -1 ) {
			pausedTime = now;
		}
		blockedTime = -1;

		// close the flv file
		if( input != null ) {
			input.close();
		}
		
		// open the flv file
		input = new ByteBufferInputStream(new RandomAccessFileReader(fileName, 0));
		readMsgQueue.clear();
		
		try {
			// prepare to send first audio + video chunk (with null time stamp)
			FlvHeader flvHeader = FlvHeader.read(input);
			
			boolean hasAudio = flvHeader.isHasAudio();
			boolean hasVideo = flvHeader.isHasVideo();
			
			// TODO seek key frame from index file
			//long startPosition = 0;
			//input = new RandomAccessFileInputStream(raInputFile, startPosition, raInputFile.length());

			while(hasAudio || hasVideo) {
				FlvTag flvTag = FlvTag.read(input);
				if( flvTag == null ) {
					break;
				}
				ByteBuffer[] data = flvTag.getData();
				long time = flvTag.getTimestamp();
				switch( flvTag.getTagType() ) {
				case FlvTag.FLV_AUDIO:
					if( time < seekTime ) {
						continue;
					}
					if(audioPlaying) {
						readMsgQueue.offer(new Event(time, new RtmpMessageAudio(data)));
					}
					
					hasAudio = false;
					break;
				case FlvTag.FLV_VIDEO:
					if(FlvTag.isVideoKeyFrame(data)) {
						readMsgQueue.clear();
					}
					if(videoPlaying) {
						readMsgQueue.offer(new Event(time, new RtmpMessageVideo(data)));
					}
					if(time < seekTime) {
						continue;
					}
					hasVideo = false;
					break;
				case FlvTag.FLV_META:
					if(time < seekTime) {
						continue;
					}
					readMsgQueue.offer(new Event(time, new RtmpMessageData(data)));
				}
				
			}
		} catch(FlvException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage());
		}
		
	}
	

	public void play() throws IOException {
		long now = System.currentTimeMillis();
		if (input == null) {
			return;
		}
		if (pausedTime != -1) {
			return;
		}
		if (blockedTime != -1) {
			startTime += (now - blockedTime);
			blockedTime = -1;
		}
		
		while (true) {
			Event msg = readMsgQueue.poll();
			if (msg == null) {
				break;
			}
			stream.writeMessage(msg.getTimestamp(), (RtmpMessage)msg.getEvent());
			currentTime =  msg.getTimestamp();
			if( stream.isWriteBlocking() ) {
				blockedTime = now;
				return;
			}
		}
		
		try {
			long relativeTime = now - startTime;
			while(relativeTime > currentTime) {
				FlvTag flvTag = FlvTag.read(input);
				if( flvTag == null ) {	// eof
					input.close();
					stream.setPlayer(null);
					throw new EOFException();
				}

				ByteBuffer[] data = flvTag.getData();
				long time = flvTag.getTimestamp();
				currentTime = time;
				switch(flvTag.getTagType()) {
				case FlvTag.FLV_AUDIO:
					if(audioPlaying) {
						stream.writeMessage(time, new RtmpMessageAudio(data));
					}
					break;
				case FlvTag.FLV_VIDEO:
					if(videoPlaying) {
						stream.writeMessage(time, new RtmpMessageVideo(data));
					}
					break;
				case FlvTag.FLV_META:
					stream.writeMessage(time, new RtmpMessageData(data));
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
