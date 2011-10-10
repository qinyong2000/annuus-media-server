package com.ams.mp4;

import java.util.ArrayList;
import java.util.List;

import com.ams.mp4.STSC.STSCRecord;
import com.ams.mp4.STTS.STTSRecord;

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
	
	public List<Mp4Sample> getSamples(int startIndex, int endIndex) {
		ArrayList<Mp4Sample> list = new ArrayList<Mp4Sample>();
		int sample = 0;
		for(long chunkOffset : stco.getOffsets()) {
			
			//list.add(new Mp4Sample());
		}
		return list;
	}
	
	public int getSampleIndex(long timeStamp) {
		long delta = 0;
		int index = 0;
		for ( STTSRecord entry : stts.getEntries()) {
			delta += entry.sampleDelta * entry.sampleCount;
			if (delta >= timeStamp) {
				index += (delta - timeStamp) / entry.sampleDelta; 
				break;
			}
			index += entry.sampleCount;
		}
		return index;
	}

	public int getSampleChunkIndex(int index) {
		int count = 0;
		int prevFirstChunk = 0;
		int prevSamplesPerChunk = 0;
		for ( STSCRecord entry : stsc.getEntries()) {
			count += (entry.firstChunk - prevFirstChunk) * prevSamplesPerChunk;
			if (count >= index) {
				return prevFirstChunk + (count - index) / prevSamplesPerChunk;
			}
			prevFirstChunk = entry.firstChunk;
			prevSamplesPerChunk = entry.samplesPerChunk;
		}
		count += prevSamplesPerChunk;
		
		return prevFirstChunk  + (count - index) / prevSamplesPerChunk; 
	}	
}
