package com.ams.rtmp.net;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import com.ams.io.*;
import com.ams.message.*;
import com.ams.mp4.Mp4Deserializer;
import com.ams.mp4.Mp4Sample;
import com.ams.rtmp.message.*;

public class F4vPlayer implements IPlayer{
	private static int BUFFER_TIME = 2 * 1000; // x seconds of buffering
	private NetStream stream = null;
	private RandomAccessFileReader reader;
	private Mp4Deserializer deserializer;
	private long startTime = -1;
	private long currentTime = 0;
	private long blockedTime = -1;
	private long pausedTime = -1;
	private boolean audioPlaying = true;
	private boolean videoPlaying = true;
	private LinkedList<MediaMessage> readMsgQueue;

	public F4vPlayer(String fileName, NetStream stream) throws IOException {
		this.stream = stream;
		this.reader = new RandomAccessFileReader(fileName, 0);
		this.deserializer = new Mp4Deserializer(reader);
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

		readMsgQueue.clear();
		
		Mp4Sample[] samples = deserializer.seek(seekTime);
		Mp4Sample videoSample = samples[0];
		Mp4Sample audioSample = samples[1];

		if (videoSample != null) {
			readMsgQueue.offer(new MediaMessage(0, new RtmpMessageVideo(deserializer.getVideoHeaderTag())));

			long time = videoSample.getTimeStamp() / deserializer.getVideoTimeScale();
			ByteBuffer[] data = deserializer.getVideoTag(videoSample);
			if(videoPlaying) {
				readMsgQueue.offer(new MediaMessage(time, new RtmpMessageVideo(data)));
			}
		}

		if (audioSample != null) {
			readMsgQueue.offer(new MediaMessage(0, new RtmpMessageAudio(deserializer.getAudioHeaderTag())));

			long time = audioSample.getTimeStamp() / deserializer.getAudioTimeScale();
			ByteBuffer[] data = deserializer.getAudioTag(audioSample);
			if(audioPlaying) {
				readMsgQueue.offer(new MediaMessage(time, new RtmpMessageAudio(data)));
			}
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
		
		long relativeTime = now - startTime;
		while(relativeTime > currentTime) {

			Mp4Sample[] samples = deserializer.readNext();
			if( samples[0] == null && samples[1] == null) {	// eof
				stream.setPlayer(null);
				throw new EOFException();
			}

			Mp4Sample videoSample = samples[0];
			Mp4Sample audioSample = samples[1];

			if (videoSample != null) {
				long time = 1000 * videoSample.getTimeStamp() / deserializer.getVideoTimeScale();
				currentTime = time;
				ByteBuffer[] data = deserializer.getVideoTag(videoSample);
				if(videoPlaying) {
					readMsgQueue.offer(new MediaMessage(time, new RtmpMessageVideo(data)));
				}
			}

			if (audioSample != null) {
				long time = 1000 * audioSample.getTimeStamp() / deserializer.getAudioTimeScale();
				ByteBuffer[] data = deserializer.getAudioTag(audioSample);
				if(audioPlaying) {
					readMsgQueue.offer(new MediaMessage(time, new RtmpMessageAudio(data)));
				}
			}
			

			if( stream.isWriteBlocking() ) {
				blockedTime = now;
				return;
			}
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
