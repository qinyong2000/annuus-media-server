package com.ams.rtmp;

import java.io.IOException;

import com.ams.io.ByteBufferOutputStream;

public class RtmpHeaderSerializer {
	private ByteBufferOutputStream out;
	
	public RtmpHeaderSerializer(ByteBufferOutputStream out) {
		super();
		this.out = out;
	}

	public void write( RtmpHeader header) throws IOException {
		int fmt;
		if( header.getStreamId() != -1 )
			fmt = 0;
		else if( header.getType() != -1 )
			fmt = 1;
		else if( header.getTimestamp() != -1 )
			fmt = 2;
		else
			fmt = 3;
		// write Chunk Basic Header
		int chunkStreamId = header.getChunkStreamId();
		if (chunkStreamId >=2 && chunkStreamId <= 63 ) {			// 1 byte version
			out.writeByte(fmt << 6 | chunkStreamId);				// csid = 2 indicates Protocol Control Messages
		} else if (chunkStreamId >=64 && chunkStreamId <= 319 ) { 	// 2 byte version
			out.writeByte(fmt << 6 | 0 );
			out.writeByte(chunkStreamId - 64);
		} else { 										// 3 byte version
			out.writeByte(fmt << 6 | 1 );
			int h = chunkStreamId - 64;
			out.write16BitLittleEndian(h);
		}
		
		if(fmt == 0 || fmt == 1 || fmt == 2) {			// type 0, type 1, type 2 header
			long ts = header.getTimestamp();
			if (ts >= 0x00FFFFFF) ts = 0x00FFFFFF; 		// send extended time stamp
			out.write24Bit((int)ts);
		}
		if(fmt == 0 || fmt == 1) {						// type 0, type 1 header
			int size = header.getSize();
			out.write24Bit(size);
			out.writeByte(header.getType());
		}
		if(fmt == 0) {									// type 0 header
			int streamId = header.getStreamId();
			out.write32BitLittleEndian(streamId);
		}
		if(fmt == 3) {									// type 3 header
		}
		
		// write extended time stamp
		long ts = header.getTimestamp();
		if ((fmt ==0 || fmt == 1 || fmt == 2) && ts >= 0x00FFFFFF ) {
			out.write32Bit(ts);
		}
	}
}
