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
				BOX.Header header = BOX.readHeader(in);
				String box = header.type;
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

				byte[] b = new byte[(int) header.payloadSize];
				in.read(b);
				DataInputStream bin = new DataInputStream(new ByteArrayInputStream(b));
				int version = bin.readByte(); // version & flags
				bin.skipBytes(3);
				if ("mdhd".equalsIgnoreCase(box)) {
					MDHD mdhd = new MDHD(version);
					mdhd.read(bin);
					trak.setMdhd(mdhd);
				} else if ("stco".equalsIgnoreCase(box)) {
					STCO stco = new STCO(version);
					stco.read(bin);
					trak.setStco(stco);
				} else if ("co64".equalsIgnoreCase(box)) {
					STCO stco = new STCO(version);
					stco.read64(bin);
					trak.setStco(stco);
				} else if ("stsc".equalsIgnoreCase(box)) {
					STSC stsc = new STSC(version);
					stsc.read(bin);
					trak.setStsc(stsc);
				} else if ("stsd".equalsIgnoreCase(box)) {
					STSD stsd = new STSD(version);
					stsd.read(bin);
					trak.setStsd(stsd);
				} else if ("stss".equalsIgnoreCase(box)) {
					STSS stss = new STSS(version);
					stss.read(bin);
					trak.setStss(stss);
				} else if ("stsz".equalsIgnoreCase(box)) {
					STSZ stsz = new STSZ(version);
					stsz.read(bin);
					trak.setStsz(stsz);
				} else if ("stts".equalsIgnoreCase(box)) {
					STTS stts = new STTS(version);
					stts.read(bin);
					trak.setStts(stts);
				}
			}
		} catch(EOFException e) {
		}	
	}

	public TRAK getVideoTrak() {
		for(TRAK trak : traks) {
			if (trak.isVideoTrack()) {
				return trak;
			}
		}
		return null;
	}

	public TRAK getAudioTrak() {
		for(TRAK trak : traks) {
			if (trak.isAudioTrack()) {
				return trak;
			}
		}
		return null;
	}
}
