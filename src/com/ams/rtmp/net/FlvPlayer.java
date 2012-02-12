package com.ams.rtmp.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import com.ams.amf.AmfValue;
import com.ams.flv.*;
import com.ams.rtmp.message.*;

public class FlvPlayer implements IPlayer{
	private static int BUFFER_TIME = 3 * 1000; // x seconds of buffering
	private NetStream stream = null;
	private SampleDeserializer deserializer;
	private long startTime = -1;
	private long currentTime = 0;
	private long bufferTime = BUFFER_TIME;
	private boolean pause = false;
	private boolean audioPlaying = true;
	private boolean videoPlaying = true;

	public FlvPlayer(SampleDeserializer deserializer, NetStream stream) throws IOException {
		this.deserializer = deserializer;
		this.stream = stream;
	}
	
	public void close() throws IOException {
		deserializer.close();
	}
	
	public void writeStartData(long timestamp) throws IOException {
		//|RtmpSampleAccess
		stream.writeDataMessage(timestamp, AmfValue.array("|RtmpSampleAccess", false, false));
		
		//NetStream.Data.Start
		stream.writeDataMessage(timestamp, AmfValue.array("onStatus", AmfValue.newObject().put("code", "NetStream.Data.Start")));
		
		AmfValue value = deserializer.metaData();
		if (value != null) {
			stream.writeDataMessage(timestamp, AmfValue.array("onMetaData", value));
		}
		
		ByteBuffer[] headerData = deserializer.videoHeaderData();
		if (headerData != null) {
			stream.writeMessage(timestamp, new RtmpMessageVideo(deserializer.videoHeaderData()));
		}
		headerData = deserializer.audioHeaderData();
		if (headerData != null) {
			stream.writeMessage(timestamp, new RtmpMessageAudio(deserializer.audioHeaderData()));
		}
		
	}
	
	public void seek(long seekTime) throws IOException {
		Sample sample = deserializer.seek(seekTime);
		if (sample == null) return;
		currentTime = sample.getTimestamp();
		startTime =  System.currentTimeMillis() - bufferTime - currentTime;
		writeStartData(currentTime);
	}
	

	public void play() throws IOException {
		if (pause) return;

		long durationTime = System.currentTimeMillis() - startTime;
		while(currentTime < durationTime ) {
			Sample sample = deserializer.readNext();
			if( sample == null ) {	// eof
				stream.setPlayer(null);
				break;
			}
			long timestamp = sample.getTimestamp();
			ByteBuffer[] data = sample.getData();
			if (sample.isAudioTag() && audioPlaying) {
				stream.writeAudioMessage(timestamp, new RtmpMessageAudio(data));
			}
			if (sample.isVideoTag() && videoPlaying) {
				stream.writeVideoMessage(timestamp, new RtmpMessageVideo(data));
			}
			if (sample.isMetaTag()) {
				stream.writeMessage(timestamp, new RtmpMessageData(data));
			}
			currentTime = timestamp;
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
