package com.ams.rtmp;

import java.io.IOException;
import java.util.HashMap;

import com.ams.io.ByteBufferInputStream;

public class RtmpHeaderDeserializer {
	private HashMap<Integer, RtmpHeader> chunkHeaderMap;
	private ByteBufferInputStream in;

	public RtmpHeaderDeserializer(ByteBufferInputStream in) {
		super();
		this.in = in;
		this.chunkHeaderMap = new HashMap<Integer, RtmpHeader>();
	}

	private RtmpHeader getLastHeader(int chunkStreamId) {
		RtmpHeader h = chunkHeaderMap.get(chunkStreamId);
		if( h == null ) {
			h = new RtmpHeader(	chunkStreamId, 0, 0, 0, 0);
			chunkHeaderMap.put(chunkStreamId, h);
		}
		return h;
	}
	
	public RtmpHeader read() throws IOException {
		int h = in.readByte() & 0xFF;			// Chunk Basic Header
		int chunkStreamId = h & 0x3F;			// 1 byte version
		if (chunkStreamId == 0) {				// 2 byte version
			chunkStreamId = in.readByte() & 0xFF + 64;
		} else if (chunkStreamId == 1) {		// 3 byte version
			chunkStreamId = in.read16BitLittleEndian() + 64;
		}

		RtmpHeader lastHeader = getLastHeader(chunkStreamId);
		int fmt = h >>> 6;
		if (fmt == 0 || fmt == 1 || fmt == 2) {			// type 0, type 1, type 2 header
			int ts = in.read24Bit();
			lastHeader.setTimestamp(ts);
		}
		if (fmt == 0 || fmt == 1 ) {					// type 0, type 1 header
			int size = in.read24Bit();
			lastHeader.setSize(size);
			lastHeader.setType(in.readByte() & 0xFF);
		}
		if (fmt == 0 ) {								// type 0 header	
			int streamId = (int)in.read32BitLittleEndian();
			lastHeader.setStreamId(streamId);
		}
		if (fmt == 3) {									// type 3
			// 0 bytes
		}
		
		// extended time stamp
		if ((fmt ==0 || fmt == 1 || fmt == 2) && lastHeader.getTimestamp() == 0x00FFFFFF ) {
			long ts = in.read32Bit();
			lastHeader.setTimestamp(ts);
		}
		
		return new RtmpHeader(	chunkStreamId,
								lastHeader.getTimestamp(),
								lastHeader.getSize(),
								lastHeader.getType(),
								lastHeader.getStreamId()
								);
	}

}
