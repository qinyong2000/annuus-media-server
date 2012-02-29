package com.ams.flv;

import java.io.IOException;
import com.ams.io.ByteBufferArray;
import com.ams.io.ByteBufferOutputStream;
import com.ams.io.RandomAccessFileWriter;

public class FlvSerializer {
	private ByteBufferOutputStream out;		//record to file stream
	private boolean headerWrite = false;
	
	public FlvSerializer(RandomAccessFileWriter writer) {
		super();
		this.out = new ByteBufferOutputStream(writer);
	}

	public void write(int type, ByteBufferArray data, long time) throws IOException {
		if (!headerWrite) {
			FlvHeader header = new FlvHeader(true, true);
			FlvHeader.write(out, header);
			headerWrite = true;
		}
		
		Sample flvTag = new Sample(type, time, data);
		write(out, flvTag);
		out.flush();
	}

	private void write(ByteBufferOutputStream out, Sample flvTag) throws IOException {
		byte tagType = -1;
		switch (flvTag.getSampleType()) {
		case Sample.SAMPLE_AUDIO:
			tagType = 0x08;
			break;
		case Sample.SAMPLE_VIDEO:
			tagType = 0x09;
			break;
		case Sample.SAMPLE_META:
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
