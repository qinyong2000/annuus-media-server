package com.ams.mp4;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.ams.mp4.STSC.STSCRecord;
import com.ams.mp4.STSD.AudioSampleDescription;
import com.ams.mp4.STSD.SampleDescription;
import com.ams.mp4.STSD.VideoSampleDescription;
import com.ams.mp4.STTS.STTSRecord;
import com.ams.server.ByteBufferFactory;

public final class TRAK {
	private MDHD mdhd;
	private STSD stsd;
	private STSC stsc;
	private STTS stts;
	private STCO stco;
	private STSS stss;
	private STSZ stsz;
	
	public void setMdhd(MDHD mdhd) {
		this.mdhd = mdhd;
	}
	public void setStsd(STSD stsd) {
		this.stsd = stsd;
	}
	public void setStsc(STSC stsc) {
		this.stsc = stsc;
	}
	public void setStts(STTS stts) {
		this.stts = stts;
	}
	public void setStco(STCO stco) {
		this.stco = stco;
	}
	public void setStss(STSS stss) {
		this.stss = stss;
	}
	public void setStsz(STSZ stsz) {
		this.stsz = stsz;
	}
	
	public int getSampleIndex(long timeStamp) {
		long duration = 0;
		int index = 0;
		for (STTSRecord entry : stts.getEntries()) {
			int delta = entry.sampleDelta * entry.sampleCount;
			if (duration + delta >= timeStamp) {
				index += (timeStamp - duration) / entry.sampleDelta; 
				break;
			}
			duration += delta;	
			index += entry.sampleCount;
		}
		return index;
	}
	
	private long getSampleTimeStamp(int index) {
		long timeStamp = 0;
		int sampleCount = 0;
		for (STTSRecord entry : stts.getEntries()) {
			int delta = entry.sampleCount; 
			if (sampleCount + delta >= index) {
				timeStamp += (index - sampleCount) * entry.sampleDelta; 
				break;
			}
			timeStamp += entry.sampleDelta * entry.sampleCount;
			sampleCount += delta;
 		}
		return timeStamp;
	}

	private long getChunkOffset(int chunk) {
		return stco.getOffsets()[chunk]; 
	}

	private int getSampleSize(int index) {
		return stsz.getSizeTable()[index]; 
	}
	
	private boolean isKeyFrameSample(int index) {
		if (stss == null) return false;
		for(int sync : stss.getSyncTable()) {
			if (index < sync - 1) return false;
			if (index == sync - 1) return true;
		}
		return true;
	}
		
	public Mp4Sample[] getAllSamples(int sampleType) {
		ArrayList<Mp4Sample> list = new ArrayList<Mp4Sample>();
		int sampleIndex = 0;
		int prevFirstChunk = 0;
		int prevSamplesPerChunk = 0;
		int prevSampleDescIndex = 0;		
		for (STSCRecord entry : stsc.getEntries()) {
			for (int chunk = prevFirstChunk; chunk < entry.firstChunk - 1; chunk++) {
				// chunk offset
				long sampleOffset = getChunkOffset(chunk);
				for (int i = 0; i < prevSamplesPerChunk; i++ ) {
					// sample size
					int sampleSize = getSampleSize(sampleIndex);
					// time stamp
					long timeStamp = getSampleTimeStamp(sampleIndex);
					// keyframe
					boolean keyframe = isKeyFrameSample(sampleIndex);
					// description index
					int sampleDescIndex = prevSampleDescIndex;
					list.add(new Mp4Sample(sampleType, sampleOffset, sampleSize, timeStamp, keyframe, sampleDescIndex));
					
					sampleOffset += sampleSize;
					sampleIndex++;	
				}
			}
			prevFirstChunk = entry.firstChunk - 1;
			prevSamplesPerChunk = entry.samplesPerChunk;
			prevSampleDescIndex = entry.sampleDescIndex;
		}

		int chunkSize = this.stco.getOffsets().length;
		for (int chunk = prevFirstChunk; chunk < chunkSize; chunk++) {
			// chunk offset
			long sampleOffset = getChunkOffset(chunk);
			for (int i = 0; i < prevSamplesPerChunk; i++ ) {
				// sample size
				int sampleSize = getSampleSize(sampleIndex);
				// time stamp
				long timeStamp = getSampleTimeStamp(sampleIndex);
				// keyframe
				boolean keyframe = isKeyFrameSample(sampleIndex);
				// description index
				int sampleDescIndex = prevSampleDescIndex;
				list.add(new Mp4Sample(sampleType, sampleOffset, sampleSize, timeStamp, keyframe, sampleDescIndex));
				
				sampleOffset += sampleSize;
				sampleIndex++;	
			}
		}
		
		return list.toArray(new Mp4Sample[list.size()]); 
	}
	
	public byte[] getVideoDecoderConfigData() {
		if (stsd.getVideoSampleDescription() == null) return null;
		return stsd.getVideoSampleDescription().decoderConfig;
	}

	public byte[] getAudioDecoderConfigData() {
		if (stsd.getAudioSampleDescription() == null) return null;
		return stsd.getAudioSampleDescription().decoderSpecificConfig;
	}
	
	public VideoSampleDescription getVideoSampleDescription() {
		return stsd.getVideoSampleDescription();
	}

	public AudioSampleDescription getAudioSampleDescription() {
		return stsd.getAudioSampleDescription();
	}
	
	public int getTimeScale() {
		return mdhd.getTimeScale();
	}

	public int getDuration() {
		return mdhd.getDuration();
	}

	public String getLanguage() {
		return mdhd.getLanguage();
	}
	
	public String getType() {
		SampleDescription desc = stsd.getDescriptions()[0];
		return desc.type;
	}
	
}
