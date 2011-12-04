package com.ams.rtmp;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import com.ams.amf.Amf0Serializer;
import com.ams.amf.AmfValue;
import com.ams.io.ByteBufferInputStream;
import com.ams.io.ByteBufferOutputStream;
import com.ams.rtmp.message.*;
import com.ams.so.SoMessage;
import com.ams.util.ByteBufferHelper;

public class RtmpMessageSerializer {
	private int writeChunkSize = 128;
	private ByteBufferOutputStream out;
	private RtmpHeaderSerializer headerSerializer;
	
	public RtmpMessageSerializer(ByteBufferOutputStream out) {
		super();
		this.out = out;
		this.headerSerializer = new RtmpHeaderSerializer(out);
	}

	public void write(int chunkStreamId, int streamId, long timestamp, RtmpMessage message) throws IOException {
		ByteBuffer[] data = null;
		ByteBufferOutputStream bos = new ByteBufferOutputStream();
		
		int msgType = message.getType();
		
		switch(msgType) {
		case RtmpMessage.MESSAGE_USER_CONTROL:
			int event = ((RtmpMessageUserControl)message).getEvent();
			int sid = ((RtmpMessageUserControl)message).getStreamId();
			int ts = ((RtmpMessageUserControl)message).getTimestamp();
			
			bos.write16Bit(event);
			switch(event) {
			case RtmpMessageUserControl.EVT_STREAM_BEGIN:
			case RtmpMessageUserControl.EVT_STREAM_EOF:
			case RtmpMessageUserControl.EVT_STREAM_DRY:
			case RtmpMessageUserControl.EVT_STREAM_IS_RECORDED:
				bos.write32Bit(sid);
				break;
			case RtmpMessageUserControl.EVT_SET_BUFFER_LENGTH:
				bos.write32Bit(sid);
				bos.write32Bit(ts);
				break;
			case RtmpMessageUserControl.EVT_PING_REQUEST:
			case RtmpMessageUserControl.EVT_PING_RESPONSE:
				bos.write32Bit(ts);
				break;
			};
			data = bos.toByteBufferArray();
			break;
			
		case RtmpMessage.MESSAGE_AMF0_COMMAND:
		{
			Amf0Serializer serializer = new Amf0Serializer(new DataOutputStream(bos));

			String name = ((RtmpMessageCommand)message).getName();
			int transactionId = ((RtmpMessageCommand)message).getTransactionId();
			AmfValue[] args = ((RtmpMessageCommand)message).getArgs();
			serializer.write(new AmfValue(name));
			serializer.write(new AmfValue(transactionId));
			for(int i = 0; i < args.length; i++) {
				serializer.write(args[i]);
			}
			
			data = bos.toByteBufferArray();
			break;
		}	
		case RtmpMessage.MESSAGE_AMF3_COMMAND:
		{	
			bos.writeByte(0); 	// no used byte
			Amf0Serializer serializer = new Amf0Serializer(new DataOutputStream(bos));
			String name = ((RtmpMessageCommand)message).getName();
			int transactionId = ((RtmpMessageCommand)message).getTransactionId();
			AmfValue[] args = ((RtmpMessageCommand)message).getArgs();
			serializer.write(new AmfValue(name));
			serializer.write(new AmfValue(transactionId));
			for(int i = 0; i < args.length; i++) {
				serializer.write(args[i]);
			}
			
			data = bos.toByteBufferArray();
			break;
		}	
		case RtmpMessage.MESSAGE_AUDIO:
			data = ((RtmpMessageAudio)message).getData();
			break;
			
		case RtmpMessage.MESSAGE_VIDEO:
			data = ((RtmpMessageVideo)message).getData();
			break;
			
		case RtmpMessage.MESSAGE_AMF0_DATA:	
			data = ((RtmpMessageData)message).getData();
			break;
		case RtmpMessage.MESSAGE_AMF3_DATA:	
			data = ((RtmpMessageData)message).getData();
			break;
			
		case RtmpMessage.MESSAGE_SHARED_OBJECT:
			SoMessage so = ((RtmpMessageSharedObject)message).getData(); 
			SoMessage.write(new DataOutputStream(bos), so);
			data = bos.toByteBufferArray();
			break;
		case RtmpMessage.MESSAGE_CHUNK_SIZE:
			int chunkSize = ((RtmpMessageChunkSize)message).getChunkSize();
			bos.write32Bit(chunkSize);
			data = bos.toByteBufferArray();
			writeChunkSize = chunkSize;
			break;
		case RtmpMessage.MESSAGE_ABORT:
			sid = ((RtmpMessageAbort)message).getStreamId();
			bos.write32Bit(sid);
			data = bos.toByteBufferArray();
			break;
		case RtmpMessage.MESSAGE_ACK:
			int nbytes = ((RtmpMessageAck)message).getBytes();
			bos.write32Bit(nbytes);
			data = bos.toByteBufferArray();
			break;
		case RtmpMessage.MESSAGE_WINDOW_ACK_SIZE:
			int size = ((RtmpMessageWindowAckSize)message).getSize();
			bos.write32Bit(size);
			data = bos.toByteBufferArray();
			break;
		case RtmpMessage.MESSAGE_PEER_BANDWIDTH:
			int windowAckSize = ((RtmpMessagePeerBandwidth)message).getWindowAckSize();
			byte limitType = ((RtmpMessagePeerBandwidth)message).getLimitType();
			bos.write32Bit(windowAckSize);
			bos.writeByte(limitType);
			data = bos.toByteBufferArray();
			break;
		case RtmpMessage.MESSAGE_UNKNOWN:
			int t = ((RtmpMessageUnknown)message).getMessageType();
			data = ((RtmpMessageUnknown)message).getData();
			break;
		}

		int dataSize = ByteBufferHelper.size(data);
		RtmpHeader header = new RtmpHeader(
				chunkStreamId,
				(timestamp != -1)? timestamp : 0,
				dataSize,
				msgType,
				(streamId != -1)? streamId : 0
				);
		
		// write packet header + data
		headerSerializer.write(header);
		if (data == null) return;
		
		ByteBufferInputStream ds = new ByteBufferInputStream(data);
		ByteBuffer[] packet = null;
		if( dataSize <= writeChunkSize ) {
			packet = ds.readByteBuffer(dataSize);
			out.writeByteBuffer(packet);
		} else {
			packet = ds.readByteBuffer(writeChunkSize);
			out.writeByteBuffer(packet);
			int len = dataSize - writeChunkSize;
			RtmpHeader h = new RtmpHeader(chunkStreamId, -1, -1, -1, -1);
			while( len > 0 ) {
				headerSerializer.write(h);
				int bytes = ( len > writeChunkSize )? writeChunkSize : len;
				packet = ds.readByteBuffer(bytes);
				out.writeByteBuffer(packet);
				len -= bytes;
			}
		}
	}

	public int getWriteChunkSize() {
		return writeChunkSize;
	}

	public void setWriteChunkSize(int writeChunkSize) {
		this.writeChunkSize = writeChunkSize;
	}
}
