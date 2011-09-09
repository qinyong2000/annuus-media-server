package com.ams.rtmp.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import com.ams.flv.FlvHeader;
import com.ams.flv.FlvTag;
import com.ams.io.ByteBufferOutputStream;
import com.ams.io.RandomAccessFileWriter;

public class FlvRecorder {
	private ByteBufferOutputStream outputStream;		//record to file stream
	private boolean headerWrite = false;
	
	public FlvRecorder(RandomAccessFileWriter writer) {
		super();
		this.outputStream = new ByteBufferOutputStream(writer);
	}

	public void record(int type, ByteBuffer[] data, long time) throws IOException {
		if (!headerWrite) {
			FlvHeader header = new FlvHeader(true, true);
			FlvHeader.write(outputStream, header);
			headerWrite = true;
		}
		
		FlvTag flvTag = new FlvTag(type, data, time);
		FlvTag.write(outputStream, flvTag);
		outputStream.flush();
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
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
