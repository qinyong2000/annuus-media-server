package com.ams.rtmp.net;

import java.io.IOException;
import com.ams.amf.AmfValue;
import com.ams.io.ByteBufferArray;
import com.ams.message.IMediaDeserializer;
import com.ams.message.MediaSample;
import com.ams.rtmp.message.*;

public class StreamPlayer {
	private static int BUFFER_TIME = 3 * 1000; // x seconds of buffering
	private NetStream stream = null;
	private IMediaDeserializer deserializer;
	private long startTime = -1;
	private long bufferTime = BUFFER_TIME;
	private boolean pause = false;
	private boolean audioPlaying = true;
	private boolean videoPlaying = true;

	public StreamPlayer(IMediaDeserializer deserializer, NetStream stream) throws IOException {
		this.deserializer = deserializer;
		this.stream = stream;
	}
	
	public void close() throws IOException {
		deserializer.close();
	}
	
	private void writeStartData() throws IOException {
		//|RtmpSampleAccess
		stream.writeDataMessage(AmfValue.array("|RtmpSampleAccess", false, false));
		
		//NetStream.Data.Start
		stream.writeDataMessage(AmfValue.array("onStatus", AmfValue.newObject().put("code", "NetStream.Data.Start")));
		
		MediaSample metaData = deserializer.metaData();
		if (metaData != null) {
			stream.writeMessage(metaData.toRtmpMessage());
		}
		
		MediaSample videoHeaderData = deserializer.videoHeaderData();
		if (videoHeaderData != null) {
			stream.writeVideoMessage(videoHeaderData.toRtmpMessage());
		}
		MediaSample audioHeaderData = deserializer.audioHeaderData();
		if (audioHeaderData != null) {
			stream.writeAudioMessage(audioHeaderData.toRtmpMessage());
		}
		
	}
	
	public void seek(long seekTime) throws IOException {
		MediaSample sample = deserializer.seek(seekTime);
		if (sample != null) {
			long currentTime = sample.getTimestamp();
			startTime =  System.currentTimeMillis() - bufferTime - currentTime;
			stream.setTimeStamp(currentTime);
		}
		writeStartData();
	}

	public void play() throws IOException {
		if (pause) return;
		long durationTime = System.currentTimeMillis() - startTime;
		while(stream.getTimeStamp() < durationTime ) {
			MediaSample sample = deserializer.readNext();
			if( sample == null ) {
				break;
			} 
			long timestamp = sample.getTimestamp();
			stream.setTimeStamp(timestamp);
			ByteBufferArray data = sample.getData();
			if (sample.isAudioSample() && audioPlaying) {
				stream.writeAudioMessage(new RtmpMessageAudio(data));
			} else if (sample.isVideoSample() && videoPlaying) {
				stream.writeVideoMessage(new RtmpMessageVideo(data));
			} else if (sample.isMetaSample()) {
				stream.writeMessage(new RtmpMessageData(data));
			}
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
