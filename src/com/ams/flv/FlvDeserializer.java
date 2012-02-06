package com.ams.flv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;

import com.ams.amf.AmfValue;
import com.ams.io.ByteBufferInputStream;
import com.ams.io.RandomAccessFileReader;

public class FlvDeserializer implements SampleDeserializer {
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

	private class SampleTimestampComparator implements java.util.Comparator {
		public int compare(Object s, Object t) {
			return (int)((FlvTag) s).getTimestamp() - (int)((FlvTag) t).getTimestamp();
		}
	};
	
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

	public AmfValue metaData() {
		if (firstMetaTag != null && "onMetaData".equals(firstMetaTag.getEvent()) && firstMetaTag.getMetaData() != null) {
			return firstMetaTag.getMetaData();
		}
		AmfValue value = AmfValue.newEcmaArray();
		float duration = (float)lastTimestamp / 1000;
		value.put("duration", duration)
			.put("width", firstVideoTag.getWidth())
			.put("height", firstVideoTag.getHeight())
			.put("videodatarate", (float)videoDataSize * 8 / duration / 1024) //kBits/sec
			.put("canSeekToEnd", lastVideoTag.isKeyframe())
			.put("videocodecid", firstVideoTag.getCodecId())
			.put("audiodatarate", (float)audioDataSize * 8 / duration / 1024) //kBits/sec
			.put("audiocodecid", firstAudioTag.getSoundFormat())
			.put("framerate", (float)videoFrames / duration);
		return value;
	}

	public ByteBuffer[] videoHeaderData() {
		return null;
	}

	public ByteBuffer[] audioHeaderData() {
		return null;
	}
	
	public FlvTag seek(long seekTime) throws IOException {
		FlvTag flvTag = firstVideoTag;
		int idx = Collections.binarySearch(samples, new FlvTag(Sample.SAMPLE_VIDEO, 0, 0, true, seekTime) , new SampleTimestampComparator());
		int i = (idx >= 0) ? idx : -(idx + 1);
		while(i < samples.size()) {
			FlvTag tag = samples.get(i);
			if( tag.getTimestamp() >= seekTime ) {
				break;
			}
			flvTag = tag;
		}
		reader.seek(flvTag.offset - 11);
		return flvTag;
	}
	
	public FlvTag readNext() throws IOException {
		try {
			return FlvTag.read(new ByteBufferInputStream(reader));
		} catch (FlvException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void close() throws IOException {
		reader.close();
	}

}
