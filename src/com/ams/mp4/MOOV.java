package com.ams.mp4;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;

public final class MOOV {
	private ArrayList<TRAK> traks = new ArrayList<TRAK>();
	
	public void read(DataInputStream in) throws IOException {
		try {
			while(true) {
				int size = in.readInt();
				byte[] b = new byte[4];
				in.read(b);
				String box = new String(b);
				TRAK trak = null;;
				if ("trak".equalsIgnoreCase(box)) {
					trak = new TRAK();
					traks.add(trak);
				} else if ("mdhd".equalsIgnoreCase(box)) {
					MDHD mdhd = new MDHD();
					mdhd.read(in);
					trak.setMdhd(mdhd);
				} else if ("stco".equalsIgnoreCase(box)) {
					STCO stco = new STCO();
					stco.read(in);
					trak.setStco(stco);
				} else if ("co64".equalsIgnoreCase(box)) {
					STCO stco = new STCO();
					stco.read64(in);
					trak.setStco(stco);
				} else if ("stsc".equalsIgnoreCase(box)) {
					STSC stsc = new STSC();
					stsc.read(in);
					trak.setStsc(stsc);
				} else if ("stsd".equalsIgnoreCase(box)) {
					STSD stsd = new STSD();
					stsd.read(in);
					trak.setStsd(stsd);
				} else if ("stss".equalsIgnoreCase(box)) {
					STSS stss = new STSS();
					stss.read(in);
					trak.setStss(stss);
				} else if ("stsz".equalsIgnoreCase(box)) {
					STSZ stsz = new STSZ();
					stsz.read(in);
					trak.setStsz(stsz);
				} else if ("stts".equalsIgnoreCase(box)) {
					STTS stts = new STTS();
					stts.read(in);
					trak.setStts(stts);
				} else {
					// skip size - 8 bytes
					in.skipBytes(size -8);
				}
			}
		} catch(EOFException e) {
		}	
	}

	public TRAK getVideoTrak() {
		for(TRAK trak : traks) {
			if ("avc1".equalsIgnoreCase(trak.getType())) {
				return trak;
			}
		}
		return null;
	}

	public TRAK getAudioTrak() {
		for(TRAK trak : traks) {
			if ("mp4a".equalsIgnoreCase(trak.getType())) {
				return trak;
			}
		}
		return null;
	}
}
