package com.ams.rtmp.net;

import java.io.IOException;
import java.util.HashMap;

import com.ams.amf.*;
import com.ams.flv.FlvException;
import com.ams.flv.FlvSerializer;
import com.ams.io.*;
import com.ams.rtmp.RtmpConnection;
import com.ams.rtmp.message.*;
import com.ams.event.*;

public class NetStream {
	private RtmpConnection rtmp;
	private int chunkStreamId = -1;
	private int streamId;
	private int transactionId = 0;
	
	private StreamPublisher publisher = null;
	private IPlayer player = null;
	
	public NetStream(RtmpConnection rtmp, int streamId) {
		this.rtmp = rtmp;
		this.streamId = streamId;
	}
	
	public void writeMessage(long timestamp, RtmpMessage message) throws IOException {
		rtmp.writeRtmpMessage(chunkStreamId, streamId, timestamp, message);
	}

	public void writeStatusMessage(String status, HashMap<String, AmfValue> info) throws IOException {
		info.put("level", new AmfValue("status"));
		info.put("code", new AmfValue(status));
		AmfValue[] argsMessageCommand = { new AmfNull(), new AmfValue(info) };
		rtmp.writeRtmpMessage(chunkStreamId, streamId, -1, 
						new RtmpMessageCommand("onStatus", transactionId, argsMessageCommand));
	}

	public void writeErrorMessage(String msg) throws IOException {
		HashMap<String, AmfValue> value = new HashMap<String, AmfValue>();
		value.put("level", new AmfValue("error"));
		value.put("code", new AmfValue("NetStream.Error"));
		value.put("details", new AmfValue(msg));
		AmfValue[] argsMessageCommand = { new AmfNull(), new AmfValue(value) };
		rtmp.writeRtmpMessage(chunkStreamId, streamId, -1, 
				  		new RtmpMessageCommand("onStatus", transactionId, argsMessageCommand));
	}
	
	public synchronized void close() throws IOException {
		if(player != null) {
			player.close();
		}
		if (publisher != null) {
			publisher.close();
			PublisherManager.removePublisher(publisher.getPublishName());
		}
		
	}

	public boolean isWriteBlocking() {
		return rtmp.getConnector().isWriteBlocking();
	}
	
	public void setChunkStreamId(int chunkStreamId) {
		this.chunkStreamId = chunkStreamId;
	}

	public void setTransactionId(int transactionId) {
		this.transactionId = transactionId;
	}
	
	public int getStreamId() {
		return streamId;
	}

	public IPlayer getPlayer() {
		return player;
	}


	public void setPlayer(IPlayer player) {
		this.player = player;
	}

	public StreamPublisher getPublisher() {
		return publisher;
	}

	public void play(NetContext context, String streamName, int start, int len) throws NetConnectionException, IOException, FlvException {
		if (player != null) {
			writeErrorMessage("This channel is already playing");
			return;
		}
		// set chunk size
		rtmp.writeProtocolControlMessage(new RtmpMessageChunkSize(1024));
		
		// clear
		rtmp.writeProtocolControlMessage(new RtmpMessageUserControl(RtmpMessageUserControl.EVT_STREAM_IS_RECORDED, streamId));
		rtmp.writeProtocolControlMessage(new RtmpMessageUserControl(RtmpMessageUserControl.EVT_STREAM_BEGIN, streamId));
		
		String app = context.getAttribute("app");
		
		switch(start) {
		case -1:		// live only
				{
					StreamPublisher publisher = (StreamPublisher) PublisherManager.getPublisher(streamName);
					if (publisher == null) {
						writeErrorMessage("Unknown shared stream '" + streamName + "'");
						return;
					}
					player = new StreamPlayer(this, publisher);
					publisher.addSubscriber((IEventSubscriber)player);
				}
				break;
		case -2:		// first find live
				{
					StreamPublisher publisher = (StreamPublisher) PublisherManager.getPublisher(streamName);
					if (publisher != null) {
						player = new StreamPlayer(this, publisher);
						publisher.addSubscriber((IEventSubscriber)player);
					} else {
						String file = context.getRealPath(app, streamName);
						player = new FlvPlayer(file, this);
						player.seek(0);
					}
				}		
				break;
		default:		// >= 0
				String file = context.getRealPath(app, streamName);
				player = new FlvPlayer(file, this);
				player.seek(start);
		}

		HashMap<String, AmfValue> status = new HashMap<String, AmfValue>();
		status.put("description", new AmfValue("Resetting " + streamName + "."));
		status.put("details", new AmfValue(streamName));
		status.put("clientId", new AmfValue(streamId));
		writeStatusMessage("NetStream.Play.Reset", status);
		
		status = new HashMap<String, AmfValue>();
		status.put("description", new AmfValue("Start playing " + streamName + "."));
		status.put("clientId", new AmfValue(streamId));
		writeStatusMessage("NetStream.Play.Start", status);
	}

	public void seek(int offset) throws NetConnectionException, IOException, FlvException {
		if (player == null) {
			writeErrorMessage("Invalid 'Seek' stream id " + streamId);
			return;
		}
		
		player.seek(offset);
		
		HashMap<String, AmfValue> value = new HashMap<String, AmfValue>();
		value.put("level", new AmfValue("status"));
		value.put("code", new AmfValue("NetStream.Seek.Notify"));

		AmfValue[] argsMessageCommand = { new AmfNull(), new AmfValue(value) };
		writeMessage(-1, new RtmpMessageCommand("_result", transactionId, argsMessageCommand));
		
		HashMap<String, AmfValue> status = new HashMap<String, AmfValue>();
		status.put("time", new AmfValue(offset));
		writeStatusMessage("NetStream.Play.Start", status);
	}
	
	public void pause(boolean pause, long time) throws IOException, NetConnectionException, FlvException {
		if( player == null ) {
			writeErrorMessage("This channel is already closed");
			return;
		}
		
		player.pause(pause);
		player.seek(time);

		if (pause) {
			rtmp.writeProtocolControlMessage(new RtmpMessageUserControl(RtmpMessageUserControl.EVT_STREAM_EOF, streamId));
		}	

		HashMap<String, AmfValue> value = new HashMap<String, AmfValue>();
		value.put("level", new AmfValue("status"));
		value.put("code", new AmfValue(pause ? "NetStream.Pause.Notify" : "NetStream.Unpause.Notify"));
		
		AmfValue[] argsMessageCommand = { new AmfNull(), new AmfValue(value) };
		rtmp.writeRtmpMessage(chunkStreamId, -1, -1,  
				  new RtmpMessageCommand("_result", transactionId, argsMessageCommand));
		
	}
	
	public void publish(NetContext context, String name, String type) throws NetConnectionException, IOException {
		String app = context.getAttribute("app");
		
		//save to share or file
		publisher = new StreamPublisher(name);
		if ("record".equals(type)) {
			String file = context.getRealPath(app, name);
			RandomAccessFileWriter writer = new RandomAccessFileWriter(file, false); 
			publisher.setRecorder(new FlvSerializer(writer));
		} else if ("append".equals(type)) {
			String file = context.getRealPath(app, name);
			RandomAccessFileWriter writer = new RandomAccessFileWriter(file, true); 
			publisher.setRecorder(new FlvSerializer(writer));
		} else if ("live".equals(type)) {
			// nothing to do	
		}
		PublisherManager.addPublisher(publisher);
		
		HashMap<String, AmfValue> status = new HashMap<String, AmfValue>();
		status.put("details", new AmfValue(name));
		writeStatusMessage("NetStream.Publish.Start", status);
	}

	public void receiveAudio(boolean flag) throws IOException {
		if (player != null) {
			player.audioPlaying(flag);
		}
	}

	public void receiveVideo(boolean flag) throws IOException {
		if (player != null) {
			player.videoPlaying(flag);
		}	
	}

}