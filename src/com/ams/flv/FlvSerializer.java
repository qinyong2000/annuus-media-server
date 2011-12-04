package com.ams.flv;

import java.io.IOException;
import java.nio.ByteBuffer;
import com.ams.io.ByteBufferOutputStream;
import com.ams.io.RandomAccessFileWriter;

public class FlvSerializer {
	private ByteBufferOutputStream out;		//record to file stream
	private boolean headerWrite = false;
	
	public FlvSerializer(RandomAccessFileWriter writer) {
		super();
		this.out = new ByteBufferOutputStream(writer);
	}

	public void write(int type, ByteBuffer[] data, long time) throws IOException {
		if (!headerWrite) {
			FlvHeader header = new FlvHeader(true, true);
			FlvHeader.write(out, header);
			headerWrite = true;
		}
		
		FlvTag flvTag = new FlvTag(type, data, time);
		FlvTag.write(out, flvTag);
		out.flush();
	}

	public synchronized void close() {
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
