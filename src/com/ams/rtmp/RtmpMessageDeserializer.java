package com.ams.rtmp;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import com.ams.amf.Amf0Deserializer;
import com.ams.amf.Amf3Deserializer;
import com.ams.amf.AmfException;
import com.ams.amf.AmfSwitchToAmf3Exception;
import com.ams.amf.AmfValue;
import com.ams.io.ByteBufferInputStream;
import com.ams.rtmp.message.RtmpMessage;
import com.ams.rtmp.message.RtmpMessageAbort;
import com.ams.rtmp.message.RtmpMessageAck;
import com.ams.rtmp.message.RtmpMessageAudio;
import com.ams.rtmp.message.RtmpMessageChunkSize;
import com.ams.rtmp.message.RtmpMessageCommand;
import com.ams.rtmp.message.RtmpMessageData;
import com.ams.rtmp.message.RtmpMessagePeerBandwidth;
import com.ams.rtmp.message.RtmpMessageSharedObject;
import com.ams.rtmp.message.RtmpMessageUnknown;
import com.ams.rtmp.message.RtmpMessageUserControl;
import com.ams.rtmp.message.RtmpMessageVideo;
import com.ams.rtmp.message.RtmpMessageWindowAckSize;
import com.ams.so.SoMessage;

public class RtmpMessageDeserializer {
	private int readChunkSize = 128;
	protected HashMap<Integer, RtmpChunkData> chunkDataMap;
	protected ByteBufferInputStream in;

	public RtmpMessageDeserializer(ByteBufferInputStream in) {
		super();
		this.in = in;
		this.chunkDataMap = new HashMap<Integer, RtmpChunkData>();
	}

	public RtmpMessage read(RtmpHeader header) throws IOException, AmfException, RtmpException {
		int chunkStreamId = header.getChunkStreamId();
		RtmpChunkData chunkData = chunkDataMap.get(chunkStreamId);
		if( chunkData == null ) {
			chunkData = new RtmpChunkData(header);
			int remain = chunkData.getRemainBytes();
			if( header.getSize() <= readChunkSize ) {
				chunkData.read(in, remain);
				return parseChunkData(chunkData);
			}
			// continue to read	
			chunkData.read(in, readChunkSize);
			chunkDataMap.put(chunkStreamId, chunkData); 
		} else {
			int remain = chunkData.getRemainBytes();
			if(remain <= readChunkSize) {
				chunkData.read(in, remain);
				chunkDataMap.remove(chunkStreamId);
				return parseChunkData(chunkData);
			}
			chunkData.read(in, readChunkSize);
		}
		return null;
	}

	private RtmpMessage parseChunkData(RtmpChunkData chunk) throws IOException, AmfException, RtmpException {
		RtmpHeader header = chunk.getHeader();
		ByteBuffer[] data = chunk.getChunkData();
		ByteBufferInputStream bis = new ByteBufferInputStream(data);
		switch(header.getType()) {
		case RtmpMessage.MESSAGE_USER_CONTROL:
			int event = bis.read16Bit();
			int streamId = -1;
			int timestamp = -1;
			switch(event) {
			case RtmpMessageUserControl.EVT_STREAM_BEGIN:
			case RtmpMessageUserControl.EVT_STREAM_EOF:
			case RtmpMessageUserControl.EVT_STREAM_DRY:
			case RtmpMessageUserControl.EVT_STREAM_IS_RECORDED:
				streamId = (int)bis.read32Bit();
				break;
			case RtmpMessageUserControl.EVT_SET_BUFFER_LENGTH:
				streamId = (int)bis.read32Bit();
				timestamp = (int)bis.read32Bit();	// buffer length
				break;
			case RtmpMessageUserControl.EVT_PING_REQUEST:
			case RtmpMessageUserControl.EVT_PING_RESPONSE:
				timestamp = (int)bis.read32Bit();	// timestamp
				break;
			default:
				event = RtmpMessageUserControl.EVT_UNKNOW;
			}
			return new RtmpMessageUserControl(event, streamId, timestamp);
		case RtmpMessage.MESSAGE_AMF3_COMMAND:
			bis.readByte();		// no used byte, continue to amf0 parsing
		case RtmpMessage.MESSAGE_AMF0_COMMAND:
		{	
			DataInputStream dis = new DataInputStream(bis);
			Amf0Deserializer amf0Deserializer = new Amf0Deserializer(dis);
			Amf3Deserializer amf3Deserializer = new Amf3Deserializer(dis);
			
			String name = amf0Deserializer.read().string();
			int transactionId = amf0Deserializer.read().integer();
			ArrayList<AmfValue> argArray = new ArrayList<AmfValue>();
			boolean amf3Object = false;
			while(true) {
				try {
				if (amf3Object) {
					argArray.add(amf3Deserializer.read());
				} else {
					argArray.add(amf0Deserializer.read());
				}
				} catch (AmfSwitchToAmf3Exception e){
					amf3Object = true;
				}
				catch(IOException e) {
					break;
				}
			}
			
			AmfValue[] args = new AmfValue[argArray.size()];
			argArray.toArray(args);
			return new RtmpMessageCommand(name, transactionId, args);
		}	

		case RtmpMessage.MESSAGE_VIDEO:
			return new RtmpMessageVideo(data);
			
		case RtmpMessage.MESSAGE_AUDIO:
			return new RtmpMessageAudio(data);
			
		case RtmpMessage.MESSAGE_AMF0_DATA:
		case RtmpMessage.MESSAGE_AMF3_DATA:
			return new RtmpMessageData(data);
		case RtmpMessage.MESSAGE_SHARED_OBJECT:
			SoMessage so = SoMessage.read(new DataInputStream(bis));
			return new RtmpMessageSharedObject(so);

		case RtmpMessage.MESSAGE_CHUNK_SIZE:
			readChunkSize = (int)bis.read32Bit();
			return new RtmpMessageChunkSize(readChunkSize);
			
		case RtmpMessage.MESSAGE_ABORT:
			return new RtmpMessageAbort((int)bis.read32Bit());
			
		case RtmpMessage.MESSAGE_ACK:
			return new RtmpMessageAck((int)bis.read32Bit());

		case RtmpMessage.MESSAGE_WINDOW_ACK_SIZE:
			return new RtmpMessageWindowAckSize((int)bis.read32Bit());

		case RtmpMessage.MESSAGE_PEER_BANDWIDTH:
			int windowAckSize = (int)bis.read32Bit();
			byte limitType = bis.readByte();
			return new RtmpMessagePeerBandwidth(windowAckSize, limitType);
			
		default:
			System.out.println("UNKNOWN HEADER TYPE:" + header.getType());
			return new RtmpMessageUnknown(header.getType(), data);
		}
	}

	public int getReadChunkSize() {
		return readChunkSize;
	}

	public void setReadChunkSize(int readChunkSize) {
		this.readChunkSize = readChunkSize;
	}
}
