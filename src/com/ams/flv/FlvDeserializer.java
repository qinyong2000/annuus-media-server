package com.ams.flv;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import com.ams.amf.AmfValue;
import com.ams.io.ByteBufferArray;
import com.ams.io.ByteBufferInputStream;
import com.ams.io.RandomAccessFileReader;
import com.ams.server.ByteBufferFactory;

public class FlvDeserializer implements ISampleDeserializer {
	private RandomAccessFileReader reader;
	private ArrayList<Sample> samples = new ArrayList<Sample>();
	private long videoFrames = 0, audioFrames = 0;
	private long videoDataSize = 0, audioDataSize = 0;
	private long lastTimestamp = 0;
	private VideoTag firstVideoTag = null;
	private AudioTag firstAudioTag = null;
	private MetaTag firstMetaTag = null;
	private VideoTag lastVideoTag = null;
	private AudioTag lastAudioTag = null;
	private MetaTag lastMetaTag = null;

	private static final byte[] H264_VIDEO_HEADER=
		{(byte)0x01,(byte)0x4d,(byte)0x40,(byte)0x1e,(byte)0xff,(byte)0xe1,(byte)0x00,(byte)0x17,
		(byte)0x67,(byte)0x4d,(byte)0x40,(byte)0x1e,(byte)0x92,(byte)0x42,(byte)0x01,(byte)0x40,
		(byte)0x5f,(byte)0xd4,(byte)0xb0,(byte)0x80,(byte)0x00,(byte)0x01,(byte)0xf4,(byte)0x80,
		(byte)0x00,(byte)0x75,(byte)0x30,(byte)0x07,(byte)0x8b,(byte)0x17,(byte)0x24,(byte)0x01,
		(byte)0x00,(byte)0x04,(byte)0x68,(byte)0xee,(byte)0x3c,(byte)0x8};

	private static final byte[] H264_AUDIO_HEADER= {(byte)0x12, (byte)0x10};
	
	private class SampleTimestampComparator implements java.util.Comparator<Sample> {
		public int compare(Sample s, Sample t) {
			return (int)(s.getTimestamp() - t.getTimestamp());
		}
	};
	
	public FlvDeserializer(RandomAccessFileReader reader) {
		this.reader = reader;
		getAllSamples();
	}
	
	private Sample readSampleData(ByteBufferInputStream in) throws IOException,FlvException {
		int tagType;
		try {
			tagType = in.readByte() & 0xFF;
		} catch (EOFException e) {
			return null;
		}
		int dataSize = in.read24Bit(); // 24Bit read
		long timestamp = in.read24Bit(); // 24Bit read
		timestamp |= (in.readByte() & 0xFF) << 24; // time stamp extended
		
		int streamId = in.read24Bit(); // 24Bit read
		ByteBuffer[] data = in.readByteBuffer(dataSize);
		
		int previousTagSize = (int) in.read32Bit();
		
		switch (tagType) {
		case 0x08:
			return new AudioTag(timestamp, new ByteBufferArray(data));
		case 0x09:
			return new VideoTag(timestamp, new ByteBufferArray(data));
		case 0x12:
			return new MetaTag(timestamp, new ByteBufferArray(data));
		default:
			throw new FlvException("Invalid FLV tag " + tagType);
		}
	}

	private Sample readSampleOffset(RandomAccessFileReader reader) throws IOException, FlvException {
		int tagType;
		ByteBufferInputStream in = new ByteBufferInputStream(reader);
		try {
			tagType = in.readByte() & 0xFF;
		} catch (EOFException e) {
			return null;
		}
		int dataSize = in.read24Bit(); // 24Bit read
		long timestamp = in.read24Bit(); // 24Bit read
		timestamp |= (in.readByte() & 0xFF) << 24; // time stamp extended
		
		int streamId = in.read24Bit(); // 24Bit read
		long offset = reader.getPosition();
		
		int header = in.readByte();
		boolean keyframe = (header >>> 4) == 1 || header == 0x17;
		
		reader.seek(offset + dataSize);
		int previousTagSize = (int) in.read32Bit();
		switch (tagType) {
		case 0x08:
			return new AudioTag(timestamp, offset, dataSize);
		case 0x09:
			return new VideoTag(timestamp, keyframe, offset, dataSize);
		case 0x12:
			return new MetaTag(timestamp, offset, dataSize);
		default:
			throw new FlvException("Invalid FLV tag " + tagType);
		}
	}
	
	private void getAllSamples() {
		try {
			reader.seek(0);
			ByteBufferInputStream in = new ByteBufferInputStream(reader);
			FlvHeader.read(in);
			Sample tag = null;
			while((tag = readSampleOffset(reader)) != null) {
				if (tag.isVideoSample()) {
					videoFrames++;
					videoDataSize += tag.size;
					if (firstVideoTag == null)
						firstVideoTag = (VideoTag)tag;
					if (tag.isKeyframe())
						samples.add(tag);
					lastVideoTag = (VideoTag)tag;	
				}
				if (tag.isAudioSample()) {
					audioFrames++;
					audioDataSize += tag.size;
					if (firstAudioTag == null)
						firstAudioTag = (AudioTag)tag;
					lastAudioTag = (AudioTag)tag;	
				}

				if (tag.isMetaSample()) {
					if (firstMetaTag == null)
						firstMetaTag = (MetaTag)tag;
					lastMetaTag = (MetaTag)tag;
				}
				
				lastTimestamp = tag.getTimestamp();
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
		value.put("duration", duration);
		if (firstVideoTag != null) {
			value.put("width", firstVideoTag.getWidth())
				 .put("height", firstVideoTag.getHeight())
				 .put("videodatarate", (float)videoDataSize * 8 / duration / 1024) //kBits/sec
				 .put("canSeekToEnd", lastVideoTag.isKeyframe())
				 .put("videocodecid", firstVideoTag.getCodecId())
				 .put("framerate", (float)videoFrames / duration);
		}
		if (firstAudioTag != null) {
			value.put("audiodatarate", (float)audioDataSize * 8 / duration / 1024) //kBits/sec
				 .put("audiocodecid", firstAudioTag.getSoundFormat());
		}
		return value;
	}

	public ByteBufferArray videoHeaderData() {
		if (firstVideoTag != null && firstVideoTag.isH264VideoSample()) {
			byte[] data = H264_VIDEO_HEADER;
			ByteBuffer[] buf = new ByteBuffer[1];
			buf[0] = ByteBufferFactory.allocate(5 + data.length);
			buf[0].put(new byte[]{0x17, 0x00, 0x00, 0x00, 0x00});
			buf[0].put(data);
			buf[0].flip();
			return new ByteBufferArray(buf);
		}
		return null;
	}

	public ByteBufferArray audioHeaderData() {
		if (firstAudioTag != null && firstVideoTag.isH264VideoSample()) {
			byte[] data = H264_AUDIO_HEADER;
			ByteBuffer[] buf = new ByteBuffer[1];
			buf[0] = ByteBufferFactory.allocate(2 + data.length);
			buf[0].put(new byte[]{(byte)0xaf, 0x00});
			buf[0].put(data);
			buf[0].flip();
			return new ByteBufferArray(buf);
		}
		return null;
	}
	
	public Sample seek(long seekTime) throws IOException {
		Sample flvTag = firstVideoTag;
		int idx = Collections.binarySearch(samples, new Sample(Sample.SAMPLE_VIDEO, seekTime, true, 0, 0) , new SampleTimestampComparator());
		int i = (idx >= 0) ? idx : -(idx + 1);
		while(i > 0) {
			flvTag = samples.get(i);
			if (flvTag.isVideoSample() && flvTag.isKeyframe()) {
				break;
			}
			i--;
		}
		reader.seek(flvTag.offset - 11);
		return flvTag;
	}
	
	public Sample readNext() throws IOException {
		try {
			return readSampleData(new ByteBufferInputStream(reader));
		} catch (FlvException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void close() throws IOException {
		reader.close();
	}

}
