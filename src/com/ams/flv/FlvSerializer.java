package com.ams.flv;

import java.io.IOException;
import com.ams.io.ByteBufferArray;
import com.ams.io.ByteBufferOutputStream;
import com.ams.io.RandomAccessFileWriter;
import com.ams.message.MediaSample;

public class FlvSerializer {
	private ByteBufferOutputStream out;		//record to file stream
	private boolean headerWrite = false;
	
	public FlvSerializer(RandomAccessFileWriter writer) {
		super();
		this.out = new ByteBufferOutputStream(writer);
	}

	public void write(MediaSample flvTag) throws IOException {
		write(out, flvTag);
		out.flush();
	}

	private void write(ByteBufferOutputStream out, MediaSample flvTag) throws IOException {
		if (!headerWrite) {
			FlvHeader header = new FlvHeader(true, true);
			FlvHeader.write(out, header);
			headerWrite = true;
		}

		byte tagType = -1;
		switch (flvTag.getSampleType()) {
		case MediaSample.SAMPLE_AUDIO:
			tagType = 0x08;
			break;
		case MediaSample.SAMPLE_VIDEO:
			tagType = 0x09;
			break;
		case MediaSample.SAMPLE_META:
			tagType = 0x12;
			break;
		}
		// tag type
		out.writeByte(tagType);
		
		ByteBufferArray data = flvTag.getData();
		// data size
		int dataSize = data.size();
		
		out.write24Bit(dataSize); // 24Bit write
		// time stamp
		int timestamp = (int) flvTag.getTimestamp();
		out.write24Bit(timestamp); // 24Bit write
		out.writeByte((byte) ((timestamp & 0xFF000000) >>> 32));
		// stream ID
		out.write24Bit(0);
		// data
		out.writeByteBuffer(data);
		// previousTagSize
		out.write32Bit(dataSize + 11);
	}
	
	public synchronized void close() {
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
