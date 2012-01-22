package com.ams.rtmp;

import java.io.IOException;
import com.ams.amf.*;
import com.ams.io.*;
import com.ams.server.Connector;
import com.ams.rtmp.message.*;

public class RtmpConnection {
	private Connector conn;
	private ByteBufferInputStream in;
	private ByteBufferOutputStream out;

	private RtmpHeaderDeserializer headerDeserializer;
	private RtmpMessageSerializer messageSerializer;
	private RtmpMessageDeserializer messageDeserializer;

	private RtmpHeader currentHeader = null; 
	private RtmpMessage currentMessage = null; 
	
	public RtmpConnection(Connector conn) {
		this.conn = conn;
		this.in = conn.getInputStream();
		this.out = conn.getOutputStream();
		this.headerDeserializer = new RtmpHeaderDeserializer(in);
		this.messageSerializer = new RtmpMessageSerializer(out);
		this.messageDeserializer = new RtmpMessageDeserializer(in);
	}

	public boolean readRtmpMessage() throws IOException, AmfException, RtmpException {
		if (currentHeader != null && currentMessage != null) {
			currentHeader = null;
			currentMessage = null;
		}

		if (conn.available() == 0) {
			return false;
		}
		
		// read header every time, a message maybe break into several chunks
		if (currentHeader == null) {
			currentHeader = headerDeserializer.read();
		}
		if (currentMessage == null) {
			currentMessage = messageDeserializer.read(currentHeader);
			if (currentMessage == null) {
				currentHeader = null;
			}
		}
		
		return isRtmpMessageReady();
	}

	public boolean isRtmpMessageReady() {
		return (currentHeader != null && currentMessage != null);
	}

	public void writeRtmpMessage(int chunkStreamId, int streamId, long timestamp, RtmpMessage message) throws IOException {
		messageSerializer.write(chunkStreamId, streamId, timestamp, message);
	}
	
	public void writeProtocolControlMessage(RtmpMessage message) throws IOException {
		messageSerializer.write(2, 0, 0, message);
	}
	
	public Connector getConnector() {
		return conn;
	}

	public RtmpHeader getCurrentHeader() {
		return currentHeader;
	}

	public RtmpMessage getCurrentMessage() {
		return currentMessage;
	}
	
}