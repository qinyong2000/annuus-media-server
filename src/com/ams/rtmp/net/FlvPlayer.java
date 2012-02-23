package com.ams.rtmp.net;

import java.io.EOFException;
import java.io.IOException;
import com.ams.amf.AmfValue;
import com.ams.flv.*;
import com.ams.io.ByteBufferArray;
import com.ams.rtmp.message.*;

public class FlvPlayer implements IPlayer{
	private static int BUFFER_TIME = 3 * 1000; // x seconds of buffering
	private NetStream stream = null;
	private SampleDeserializer deserializer;
	private long startTime = -1;
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
	
	private void writeStartData() throws IOException {
		//|RtmpSampleAccess
		stream.writeDataMessage(AmfValue.array("|RtmpSampleAccess", false, false));
		
		//NetStream.Data.Start
		stream.writeDataMessage(AmfValue.array("onStatus", AmfValue.newObject().put("code", "NetStream.Data.Start")));
		
		AmfValue value = deserializer.metaData();
		if (value != null) {
			stream.writeDataMessage(AmfValue.array("onMetaData", value));
		}
		
		ByteBufferArray headerData = deserializer.videoHeaderData();
		if (headerData != null) {
			stream.writeMessage(new RtmpMessageVideo(headerData));
		}
		headerData = deserializer.audioHeaderData();
		if (headerData != null) {
			stream.writeMessage(new RtmpMessageAudio(headerData));
		}
		
	}
	
	public void seek(long seekTime) throws IOException {
		Sample sample = deserializer.seek(seekTime);
		if (sample == null) return;
		long currentTime = sample.getTimestamp();
		startTime =  System.currentTimeMillis() - bufferTime - currentTime;
		stream.setTimeStamp(currentTime);
		writeStartData();
	}

	public void play() throws IOException {
		if (pause) return;
		long durationTime = System.currentTimeMillis() - startTime;
		while(stream.getTimeStamp() < durationTime ) {
			Sample sample = deserializer.readNext();
			if( sample == null ) {	// eof
				stream.setPlayer(null);
				throw new EOFException("End Of Media Stream");
			}
			long timestamp = sample.getTimestamp();
			if (timestamp - stream.getTimeStamp() > 1000) {
				throw new EOFException("End Of Media Stream");
			}
			stream.setTimeStamp(timestamp);
			ByteBufferArray data = sample.getData();
			if (sample.isAudioTag() && audioPlaying) {
				stream.writeAudioMessage(new RtmpMessageAudio(data));
			}
			if (sample.isVideoTag() && videoPlaying) {
				stream.writeVideoMessage(new RtmpMessageVideo(data));
			}
			if (sample.isMetaTag()) {
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
