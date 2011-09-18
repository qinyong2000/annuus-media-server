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
/*
		if (type == FlvTag.FLV_VIDEO && Flv.isVideoKeyFrame(data)) {	
			// add meta tag for http pseudo streaming
			HashMap<String, AmfValue> metaData = new HashMap<String, AmfValue>();
			metaData.put("duration", new AmfValue((double)time / 1000));
	
			HashMap<String, AmfValue> onMetaData = new HashMap<String, AmfValue>();
			onMetaData.put("onMetaData", new AmfValue(metaData));
	
			ByteBufferOutputStream bos = new ByteBufferOutputStream();
			AmfSerializer serializer = new Amf0Serializer(new DataOutputStream(bos));
			serializer.write(new AmfValue(onMetaData));
	
			FlvTag metaTag = new FlvTag(FlvTag.FLV_META, bos.toByteBufferArray(), time);
			Flv.writeFlvTag(outputStream, metaTag);
		}
*/
	}
	public synchronized void close() {
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
