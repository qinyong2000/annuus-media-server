package com.ams.server.protocol;

import java.io.IOException;
import java.util.HashMap;

import com.ams.amf.AmfException;
import com.ams.amf.AmfValue;
import com.ams.message.MediaMessage;
import com.ams.rtmp.RtmpConnection;
import com.ams.rtmp.RtmpException;
import com.ams.rtmp.RtmpHeader;
import com.ams.rtmp.message.RtmpMessage;
import com.ams.rtmp.message.RtmpMessageCommand;
import com.ams.rtmp.net.NetConnectionException;
import com.ams.rtmp.net.PublisherManager;
import com.ams.rtmp.net.StreamPublisher;
import com.ams.server.Connector;
import com.ams.util.Log;

public class SlaveHandler implements IProtocolHandler {
	private Connector conn;
	private RtmpConnection rtmp;
	private boolean keepAlive = true;
	private HashMap<Integer, String> streams = new HashMap<Integer, String>();
	
	public void readAndProcessRtmpMessage() throws NetConnectionException, IOException, RtmpException, AmfException {
		if (!rtmp.readRtmpMessage()) return;
		RtmpHeader header = rtmp.getCurrentHeader();
		RtmpMessage message = rtmp.getCurrentMessage();
		
		switch( message.getType() ) {
		case RtmpMessage.MESSAGE_AMF0_COMMAND:
			RtmpMessageCommand command = (RtmpMessageCommand)message;
			if ("publish".equals(command.getName())) {
				int streamId = header.getStreamId();
				AmfValue[] args = command.getArgs(); 
				String publishName = args[1].string();
				streams.put(streamId, publishName);
				if (PublisherManager.getPublisher(publishName) == null) {
					PublisherManager.addPublisher(new StreamPublisher(publishName));
				}
			} else if ("closeStream".equals(command.getName())) {
				int streamId = header.getStreamId();
				String publishName = streams.get(streamId);
				if (publishName == null) break;
				streams.remove(streamId);
				StreamPublisher publisher = (StreamPublisher) PublisherManager.getPublisher(publishName);
				publisher.close();
				PublisherManager.removePublisher(publishName);
			}
			
			break;
		case RtmpMessage.MESSAGE_AUDIO:
		case RtmpMessage.MESSAGE_VIDEO:
		case RtmpMessage.MESSAGE_AMF0_DATA:
			int streamId = header.getStreamId();
			String publishName = streams.get(streamId);
			if (publishName == null) break;
			StreamPublisher publisher = (StreamPublisher) PublisherManager.getPublisher(publishName);
			if (publisher != null) {
				publisher.publish(new MediaMessage(header.getTimestamp(), message));
			}
			break;
		}
	}
	
	public void run() {
		if (conn.isClosed()) {
			clear();
			return;
		}
		
		try {
			// waiting for data arriving
			if (conn.waitDataReceived(10)) {
				// read & process rtmp message
				readAndProcessRtmpMessage();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public IProtocolHandler newInstance(Connector conn) {
		IProtocolHandler handle = new SlaveHandler();
		handle.setConnection(conn);
		return handle;
	}

	public void clear() {
		conn.close();
		keepAlive = false;
	}

	public void setConnection(Connector conn) {
		this.conn = conn;
		this.rtmp = new RtmpConnection(conn);
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}

}