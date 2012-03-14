package com.ams.rtmp.net;

import java.io.IOException;
import java.util.Map;
import com.ams.amf.*;
import com.ams.flv.FlvDeserializer;
import com.ams.flv.FlvException;
import com.ams.flv.FlvSerializer;
import com.ams.message.IMediaDeserializer;
import com.ams.io.*;
import com.ams.mp4.Mp4Deserializer;
import com.ams.rtmp.RtmpConnection;
import com.ams.rtmp.message.*;

public class NetStream {
	private RtmpConnection rtmp;
	private int chunkStreamId = 3;
	private int streamId;
	private String streamName;
	private int transactionId = 0;
	private long timeStamp = 0;
	
	private StreamPublisher publisher = null;
	private StreamPlayer player = null;
	
	public NetStream(RtmpConnection rtmp, int streamId) {
		this.rtmp = rtmp;
		this.streamId = streamId;
	}
	
	public void writeMessage(RtmpMessage message) throws IOException {
		rtmp.writeRtmpMessage(chunkStreamId, streamId, timeStamp, message);
	}

	public void writeMessage(long time, RtmpMessage message) throws IOException {
		rtmp.writeRtmpMessage(chunkStreamId, streamId, time, message);
	}
	
	public void writeVideoMessage(RtmpMessage message) throws IOException {
		rtmp.writeRtmpMessage(5, streamId, timeStamp, message);
	}

	public void writeAudioMessage(RtmpMessage message) throws IOException {
		rtmp.writeRtmpMessage(6, streamId, timeStamp, message);
	}
	
	public void writeStatusMessage(String status, AmfValue info) throws IOException {
		AmfValue value = AmfValue.newObject();
		value.put("level", "status")
			 .put("code", status);
		Map<String, AmfValue> v = info.object();
		for(String key : v.keySet()) {
			value.put(key, v.get(key).toString());
		}
		writeMessage(new RtmpMessageCommand("onStatus", transactionId, AmfValue.array(null, value)));
	}

	public void writeErrorMessage(String msg) throws IOException {
		AmfValue value = AmfValue.newObject();
		value.put("level", "error")
			 .put("code", "NetStream.Error")
			 .put("details", msg);
		writeMessage(new RtmpMessageCommand("onStatus", transactionId, AmfValue.array(null, value)));
	}

	public void writeDataMessage(AmfValue[] values) throws IOException {
		writeMessage(new RtmpMessageData(AmfValue.toBinary(values)));
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

	public StreamPlayer getPlayer() {
		return player;
	}


	public void setPlayer(StreamPlayer player) {
		this.player = player;
	}

	public StreamPublisher getPublisher() {
		return publisher;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public void seek(int time) throws NetConnectionException, IOException, FlvException {
		if (player == null) {
			writeErrorMessage("Invalid 'Seek' stream id " + streamId);
			return;
		}
		// clear
		rtmp.writeProtocolControlMessage(new RtmpMessageUserControl(RtmpMessageUserControl.EVT_STREAM_EOF, streamId));
		rtmp.writeProtocolControlMessage(new RtmpMessageUserControl(RtmpMessageUserControl.EVT_STREAM_IS_RECORDED, streamId));
		rtmp.writeProtocolControlMessage(new RtmpMessageUserControl(RtmpMessageUserControl.EVT_STREAM_BEGIN, streamId));

		player.seek(time);
		
		writeStatusMessage("NetStream.Seek.Notify", AmfValue.newObject()
				.put("description", "Seeking " + time + ".")
				.put("details", streamName)
				.put("clientId", streamId));

		
		writeStatusMessage("NetStream.Play.Start", AmfValue.newObject()
				.put("description", "Start playing " + streamName + ".")
				.put("clientId", streamId));

	}
	
	
	public void play(NetContext context, String streamName, int start, int len) throws NetConnectionException, IOException, FlvException {
		if (player != null) {
			writeErrorMessage("This channel is already playing");
			return;
		}
		this.streamName = streamName;

		// set chunk size
		rtmp.writeProtocolControlMessage(new RtmpMessageChunkSize(1024));

		//NetStream.Play.Reset
		writeStatusMessage("NetStream.Play.Reset",		
							AmfValue.newObject()
								.put("description", "Resetting " + streamName + ".")
								.put("details", streamName)
								.put("clientId", streamId));
		
		// clear
		rtmp.writeProtocolControlMessage(new RtmpMessageUserControl(RtmpMessageUserControl.EVT_STREAM_IS_RECORDED, streamId));
		rtmp.writeProtocolControlMessage(new RtmpMessageUserControl(RtmpMessageUserControl.EVT_STREAM_BEGIN, streamId));

		//NetStream.Play.Start
		writeStatusMessage("NetStream.Play.Start", 
							AmfValue.newObject()
								.put("description", "Start playing " + streamName + ".")
								.put("clientId", streamId));								
		
		String app = context.getAttribute("app");
		switch(start) {
		case -1:		// live only
				{
					StreamPublisher publisher = (StreamPublisher) PublisherManager.getPublisher(streamName);
					if (publisher == null) {
						writeErrorMessage("Unknown shared stream '" + streamName + "'");
						return;
					}
					StreamSubscriber subscriber = new StreamSubscriber(publisher);
					player = new StreamPlayer(subscriber, this);
					publisher.addSubscriber(subscriber);
					player.seek(0);
				}
				break;
		case -2:		// first find live
				{
					StreamPublisher publisher = (StreamPublisher) PublisherManager.getPublisher(streamName);
					if (publisher != null) {
						StreamSubscriber subscriber = new StreamSubscriber(publisher);
						player = new StreamPlayer(subscriber, this);
						publisher.addSubscriber(subscriber);
						player.seek(0);
					} else {
						String tokens[] = streamName.split(":");
						String type = "";
						String file = streamName;
						if (tokens.length >= 2) {
							type = tokens[0];
							file = tokens[1];
						}
						String path = context.getRealPath(app, file, type);
						player = createPlayer(type, path);
						player.seek(0);
					}
				}		
				break;
		default:		// >= 0
				String tokens[] = streamName.split(":");
				String type = "";
				String file = streamName;
				if (tokens.length >= 2) {
					type = tokens[0];
					file = tokens[1];
				}
				String path = context.getRealPath(app, file, type);
				player = createPlayer(type, path);
				player.seek(start);
		}
		
	}
	
	public StreamPlayer createPlayer(String type, String file) throws IOException {
		RandomAccessFileReader reader = new RandomAccessFileReader(file, 0);
		IMediaDeserializer sampleDeserializer = null;
		if ("mp4".equalsIgnoreCase(type)) {
			sampleDeserializer = new Mp4Deserializer(reader);
		} else {
			String ext = file.substring(file.lastIndexOf('.') + 1);
			if ("f4v".equalsIgnoreCase(ext) || "mp4".equalsIgnoreCase(ext)) {
				sampleDeserializer = new Mp4Deserializer(reader);
			} else {
				sampleDeserializer = new FlvDeserializer(reader);
			}
		}
		return new StreamPlayer(sampleDeserializer, this);
	}
	
	public void stop() throws IOException {
		//clear
		rtmp.writeProtocolControlMessage(new RtmpMessageUserControl(RtmpMessageUserControl.EVT_STREAM_EOF, streamId));
		
		//NetStream.Play.Complete
		writeDataMessage(AmfValue.array("onPlayStatus", 
						AmfValue.newObject()
						.put("level", "status")
						.put("code", "NetStream.Play.Complete")));

		//clear
		rtmp.writeProtocolControlMessage(new RtmpMessageUserControl(RtmpMessageUserControl.EVT_STREAM_EOF, streamId));
		
		//NetStream.Play.Stop
		writeStatusMessage("NetStream.Play.Stop", 
							AmfValue.newObject()
								.put("description", "Stoped playing " + streamName + ".")
								.put("clientId", streamId));
		setPlayer(null);
	}
	
	public void pause(boolean pause, long time) throws IOException, NetConnectionException, FlvException {
		if( player == null ) {
			writeErrorMessage("This channel is already closed");
			return;
		}
		player.pause(pause);

		writeStatusMessage((pause ? "NetStream.Pause.Notify" : "NetStream.Unpause.Notify"), AmfValue.newObject());
	}
	
	public void publish(NetContext context, String name, String type) throws NetConnectionException, IOException {
		String app = context.getAttribute("app");
		//save to share or file
		publisher = new StreamPublisher(name);
		if ("record".equals(type)) {
			String file = context.getRealPath(app, name, "");
			RandomAccessFileWriter writer = new RandomAccessFileWriter(file, false); 
			publisher.setRecorder(new FlvSerializer(writer));
		} else if ("append".equals(type)) {
			String file = context.getRealPath(app, name, "");
			RandomAccessFileWriter writer = new RandomAccessFileWriter(file, true); 
			publisher.setRecorder(new FlvSerializer(writer));
		} else if ("live".equals(type)) {
			// nothing to do	
		}
		PublisherManager.addPublisher(publisher);
		
		writeStatusMessage("NetStream.Publish.Start", AmfValue.newObject().put("details", name));
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