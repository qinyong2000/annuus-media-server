package com.ams.mp4;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;

public final class MOOV {
	private ArrayList<TRAK> traks = new ArrayList<TRAK>();
	
	public void read(DataInputStream in) throws IOException {
		try {
			TRAK trak = null;;
			while(true) {
				int size = in.readInt();
				byte[] b = new byte[4];
				in.read(b);
				String box = new String(b);
				if ("trak".equalsIgnoreCase(box)) {
					trak = new TRAK();
					traks.add(trak);
					continue;
				} else if ("mdia".equalsIgnoreCase(box)) {
					continue;
				} else if ("minf".equalsIgnoreCase(box)) {
					continue;
				} else if ("stbl".equalsIgnoreCase(box)) {
					continue;
				}

				// read size - 8 bytes
				b = new byte[size - 8];
				in.read(b);
				DataInputStream bin = new DataInputStream(new ByteArrayInputStream(b));
				bin.readInt(); // version & flags
				if ("mdhd".equalsIgnoreCase(box)) {
					MDHD mdhd = new MDHD();
					mdhd.read(bin);
					trak.setMdhd(mdhd);
				} else if ("stco".equalsIgnoreCase(box)) {
					STCO stco = new STCO();
					stco.read(bin);
					trak.setStco(stco);
				} else if ("co64".equalsIgnoreCase(box)) {
					STCO stco = new STCO();
					stco.read64(bin);
					trak.setStco(stco);
				} else if ("stsc".equalsIgnoreCase(box)) {
					STSC stsc = new STSC();
					stsc.read(bin);
					trak.setStsc(stsc);
				} else if ("stsd".equalsIgnoreCase(box)) {
					STSD stsd = new STSD();
					stsd.read(bin);
					trak.setStsd(stsd);
				} else if ("stss".equalsIgnoreCase(box)) {
					STSS stss = new STSS();
					stss.read(bin);
					trak.setStss(stss);
				} else if ("stsz".equalsIgnoreCase(box)) {
					STSZ stsz = new STSZ();
					stsz.read(bin);
					trak.setStsz(stsz);
				} else if ("stts".equalsIgnoreCase(box)) {
					STTS stts = new STTS();
					stts.read(bin);
					trak.setStts(stts);
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
