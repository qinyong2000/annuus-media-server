package com.ams.flv;

import java.io.IOException;
import java.util.ArrayList;

import com.ams.amf.AmfValue;
import com.ams.io.ByteBufferInputStream;
import com.ams.io.RandomAccessFileReader;

public class FlvDeserializer {
	private RandomAccessFileReader reader;
	private ArrayList<FlvTag> samples = new ArrayList<FlvTag>();
	private long videoFrames = 0, audioFrames = 0;
	private long videoDataSize = 0, audioDataSize = 0;
	private long lastTimestamp = 0;
	private VideoTag firstVideoTag = null;
	private AudioTag firstAudioTag = null;
	private MetaTag firstMetaTag = null;
	private VideoTag lastVideoTag = null;
	private AudioTag lastAudioTag = null;
	private MetaTag lastMetaTag = null;
	
	public FlvDeserializer(RandomAccessFileReader reader) {
		this.reader = reader;
		getAllSamples();
	}
	
	private void getAllSamples() {
		try {
			reader.seek(0);
			ByteBufferInputStream in = new ByteBufferInputStream(reader);
			FlvHeader.read(in);
			FlvTag tag = null;
			while((tag = FlvTag.read(reader)) != null) {
				if (tag.isVideoTag()) {
					videoFrames++;
					videoDataSize += tag.size;
					if (firstVideoTag == null)
						firstVideoTag = (VideoTag)tag;
					if (tag.isKeyframe())
						samples.add(tag);
					lastVideoTag = (VideoTag)tag;	
				}
				if (tag.isAudioTag()) {
					audioFrames++;
					audioDataSize += tag.size;
					if (firstAudioTag == null)
						firstAudioTag = (AudioTag)tag;
					lastAudioTag = (AudioTag)tag;	
				}

				if (tag.isMetaTag()) {
					if (firstMetaTag == null)
						firstMetaTag = (MetaTag)tag;
					lastMetaTag = (MetaTag)tag;
				}
				
				lastTimestamp = tag.timestamp;
			}
			
			if (firstVideoTag != null) {
				firstVideoTag.readData(reader);
				firstVideoTag.getParameters();
			}
			if (firstAudioTag != null) {
				firstAudioTag.readData(reader);
				firstAudioTag.getParameters();
			}
			if (firstMetaTag != null) {
				firstMetaTag.readData(reader);
				firstMetaTag.getParameters();
			}
		} catch (Exception e) {
		}
	}

	public AmfValue onMetaData() {
		if (firstMetaTag != null && "onMetaData".equals(firstMetaTag.getEvent())) {
			return firstMetaTag.getMetaData();
		}
		AmfValue value = AmfValue.newObject();
		value.setEcmaArray(true);
		value.put("duration", lastTimestamp / 1000)
			.put("width", firstVideoTag.getWidth())
			.put("height", firstVideoTag.getHeight())
			.put("videodatarate", videoDataSize * 8 / lastTimestamp / 1000 / 1024) //kBits/sec
			.put("canSeekToEnd", lastVideoTag.isKeyframe())
			.put("videocodecid", firstVideoTag.getCodecId())
			.put("audiodatarate", audioDataSize * 8 / lastTimestamp / 1000 / 1024) //kBits/sec
			.put("audiocodecid", firstAudioTag.getSoundFormat())
			.put("framerate", videoFrames / lastTimestamp / 1000);
		
		return value;
	}
	
	public FlvTag seek(long seekTime) throws IOException, FlvException {
		FlvTag flvTag = null;
		for(FlvTag tag : samples) {
			if( tag.getTimestamp() >= seekTime ) {
				flvTag = tag;
				break;
			}
		}
		if (flvTag != null) {
			reader.seek(flvTag.offset);
		}
		return flvTag;
	}
	
	public FlvTag readNext() throws IOException, FlvException {
		return FlvTag.read(new ByteBufferInputStream(reader));
	}

}
