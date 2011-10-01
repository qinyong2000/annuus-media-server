package com.ams.mp4;

import java.util.ArrayList;
import java.util.List;

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
	
	public List<Mp4Sample> getSamples() {
		ArrayList<Mp4Sample> list = new ArrayList<Mp4Sample>();
		int sample = 0;
		for(long chunkOffset : stco.getOffsets()) {
			
			//list.add(new Mp4Sample());
		}
		return list;
	}
}
