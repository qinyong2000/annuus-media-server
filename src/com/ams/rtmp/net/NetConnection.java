package com.ams.rtmp.net;

import java.io.IOException;
import java.util.HashMap;
import com.ams.amf.*;
import com.ams.flv.FlvException;
import com.ams.message.MediaMessage;
import com.ams.rtmp.message.*;
import com.ams.rtmp.*;
import com.ams.server.replicator.ReplicateCluster;
import com.ams.util.Log;

public class NetConnection {
	private RtmpHandShake handshake;
	private RtmpConnection rtmp;
	private NetContext context;
	private HashMap<Integer, NetStream> streams;
	
	public NetConnection(RtmpConnection rtmp, NetContext context) {
		this.rtmp = rtmp;
		this.handshake = new RtmpHandShake(rtmp);
		this.streams = new HashMap<Integer, NetStream>();
		this.context = context;
	}

	private void onMediaMessage(RtmpHeader header, RtmpMessage message) throws NetConnectionException, IOException {
		NetStream stream = streams.get(header.getStreamId());
		if( stream == null ) {
			throw new NetConnectionException("Unknown stream " + header.getStreamId());
		}
		StreamPublisher publisher = stream.getPublisher();
		if( publisher == null ) {
			throw new NetConnectionException("Publish not done on stream " + header.getStreamId());
		}
		
		publisher.publish(new MediaMessage(header.getTimestamp(), message));
		
		if (publisher.isPing()) {
			rtmp.writeProtocolControlMessage(new RtmpMessageAck(publisher.getBytes()));
		}
		
		// replicate to all slave server
		ReplicateCluster.publishMessage(publisher.getPublishName(), new MediaMessage(header.getTimestamp(), message));
	}

	private void onSharedMessage(RtmpHeader header, RtmpMessage message) {
		// TODO
	}	
	
	private void onCommandMessage(RtmpHeader header, RtmpMessage message) throws NetConnectionException, IOException, FlvException {
		RtmpMessageCommand command = (RtmpMessageCommand)message;
		String cmdName = command.getName();
		System.out.println("command name:" + cmdName);
		if ("connect".equals(cmdName)) {
			onConnect(header, command); 
		} else if ("createStream".equals(cmdName)) {
			onCreateStream(header, command);
		} else if ("deleteStream".equals(cmdName)) {
			onDeleteStream(header, command);
		} else if ("closeStream".equals(cmdName)) {
			onCloseStream(header, command);
		} else if ("play".equals(cmdName)) {
			onPlay(header, command);
		} else if ("play2".equals(cmdName)) {
			onPlay2(header, command);
		} else if ("publish".equals(cmdName)) {
			onPublish(header, command) ;
		} else if ("pause".equals(cmdName) || "pauseRaw".equals(cmdName)) {
			onPause(header, command);
		} else if ("receiveAudio".equals(cmdName)) {
			onReceiveAudio(header, command);
		} else if ("receiveVideo".equals(cmdName)) {
			onReceiveVideo(header, command);
		} else if ("seek".equals(cmdName)) {
			onSeek(header, command);
		} else {	//remote rpc call
			onCall(header, command);
		}
	}

	private void onConnect(RtmpHeader header, RtmpMessageCommand command) throws NetConnectionException, IOException {
		AmfValue amfObject = command.getCommandObject();
		HashMap<String, AmfValue> obj = amfObject.object();

		String app = obj.get("app").string();
		if (app == null) {
			netConnectionError(header.getChunkStreamId(), header.getStreamId(), command.getTransactionId(), "Invalid 'Connect' parameters");
			return;
		}
		context.setAttribute("app", app);

		rtmp.writeProtocolControlMessage(new RtmpMessageWindowAckSize(128*1024));
		rtmp.writeProtocolControlMessage(new RtmpMessagePeerBandwidth(128*1024, (byte)2));
		rtmp.writeProtocolControlMessage(new RtmpMessageUserControl(RtmpMessageUserControl.EVT_STREAM_BEGIN, header.getStreamId()));
		
		HashMap<String, AmfValue> value = new HashMap<String, AmfValue>();
		value.put("level", new AmfValue("status"));
		value.put("code", new AmfValue("NetConnection.Connect.Success"));
		value.put("description", new AmfValue("Connection succeeded."));
		AmfValue objectEncoding = obj.get("objectEncoding");
		if (objectEncoding != null) {
			value.put("objectEncoding", objectEncoding);
		}
		
		AmfValue[] argsMessageCommand = { new AmfNull(), new AmfValue(value)};
		rtmp.writeRtmpMessage(header.getChunkStreamId(), -1, -1,
				  new RtmpMessageCommand("_result", command.getTransactionId(), argsMessageCommand));

	}

	private void onPlay(RtmpHeader header, RtmpMessageCommand command) throws IOException, NetConnectionException, FlvException {
		AmfValue[] args = command.getArgs();
		String streamName = args[1].string();
		int start = (args.length > 2)?args[2].integer() : -2;
		int duration  = (args.length > 3)?args[3].integer() : -1;
		NetStream stream = streams.get(header.getStreamId());
		if( stream == null ) {
			streamError(header.getChunkStreamId(), header.getStreamId(), command.getTransactionId(), "Invalid 'Play' stream id "+ header.getStreamId());
			return;
		}
		stream.setChunkStreamId(header.getChunkStreamId());
		stream.setTransactionId(command.getTransactionId());
		stream.play(context, streamName, start, duration);
	}

	private void onPlay2(RtmpHeader header, RtmpMessageCommand command) throws IOException, NetConnectionException, FlvException {
		AmfValue[] args = command.getArgs();
		int startTime = args[0].integer();
		String oldStreamName = args[1].string();
		String streamName = args[2].string();
		int duration  = (args.length > 3)?args[3].integer() : -1;
		String transition = (args.length > 4)?args[4].string() : "switch"; //switch or swap	
		
		//TODO
	}
	
	private void onSeek(RtmpHeader header, RtmpMessageCommand command) throws IOException, NetConnectionException, FlvException {
		AmfValue[] args = command.getArgs();
		int offset = args[1].integer();
		NetStream stream = streams.get(header.getStreamId());
		if( stream == null ) {
			streamError(header.getChunkStreamId(), header.getStreamId(), command.getTransactionId(), "Invalid 'Seek' stream id "+ header.getStreamId());
			return;
		}
		stream.setTransactionId(command.getTransactionId());
		stream.seek(offset);
	}

	private void onPause(RtmpHeader header, RtmpMessageCommand command) throws IOException, NetConnectionException, FlvException {
		AmfValue[] args = command.getArgs();
		boolean pause = args[1].bool();
		int time = args[2].integer();
		NetStream stream = streams.get(header.getStreamId());
		if( stream == null ) {
			streamError(header.getChunkStreamId(), header.getStreamId(), command.getTransactionId(), "Invalid 'Pause' stream id "+ header.getStreamId());
			return;
		}
		stream.setTransactionId(command.getTransactionId());
		stream.pause(pause, time);
	}

	private void onPublish(RtmpHeader header, RtmpMessageCommand command) throws IOException, NetConnectionException {
		AmfValue[] args = command.getArgs();
		String publishName = args[1].string();
		if(PublisherManager.getPublisher(publishName) != null) {
			streamError(header.getChunkStreamId(), header.getStreamId(), command.getTransactionId(), "The publish '" + publishName + "' is already used");
			return;
		}

		String type = (args.length > 2)? args[2].string() : null;
		NetStream stream = streams.get(header.getStreamId());
		if( stream == null) {
			streamError(header.getChunkStreamId(), header.getStreamId(), command.getTransactionId(), "Invalid 'Publish' stream id "+ header.getStreamId());
			return;
		}

		stream.setChunkStreamId(header.getChunkStreamId());
		stream.setTransactionId(command.getTransactionId());
		stream.publish(context, publishName, type);
	}
	
	private void onReceiveAudio(RtmpHeader header, RtmpMessageCommand command) throws IOException {
		AmfValue[] args = command.getArgs();
		boolean flag  = args[1].bool();
		NetStream stream = streams.get(header.getStreamId());
		if( stream == null) {
			streamError(header.getChunkStreamId(), header.getStreamId(), command.getTransactionId(), "Invalid 'ReceiveAudio' stream id "+ header.getStreamId());
			return;
		}
		stream.receiveAudio(flag);
	}

	private void onReceiveVideo(RtmpHeader header, RtmpMessageCommand command) throws IOException {
		AmfValue[] args = command.getArgs();
		boolean flag  = args[1].bool();
		NetStream stream = streams.get(header.getStreamId());
		if( stream == null) {
			streamError(header.getChunkStreamId(), header.getStreamId(), command.getTransactionId(), "Invalid 'ReceiveVideo' stream id "+ header.getStreamId());
			return;
		}
		stream.receiveVideo(flag);
	}
	
	private void onCreateStream(RtmpHeader header, RtmpMessageCommand command) throws IOException {
		NetStream stream = createStream();
		AmfValue[] argsMessageCommand = { new AmfNull(), new AmfValue(stream.getStreamId()) };
		rtmp.writeRtmpMessage(header.getChunkStreamId(), -1, -1, 
						new RtmpMessageCommand("_result", command.getTransactionId(), argsMessageCommand));
		
	}

	private void onDeleteStream(RtmpHeader header, RtmpMessageCommand command) throws IOException, NetConnectionException {
		AmfValue[] args = command.getArgs();
		int streamId = args[1].integer();
		NetStream stream = streams.get(streamId);
		if( stream == null ) {
			streamError(header.getChunkStreamId(), header.getStreamId(), command.getTransactionId(), "Invalid 'deleteStream' stream id");
			return;
		}
		closeStream(stream);
	}

	private void onCloseStream(RtmpHeader header, RtmpMessageCommand command) throws NetConnectionException, IOException {
		NetStream stream = streams.get(header.getStreamId());
		if( stream == null ) {
			streamError(header.getChunkStreamId(), header.getStreamId(), command.getTransactionId(), "Invalid 'CloseStream' stream id " + header.getStreamId());
			return;
		}
		closeStream(stream);
	}

	private void onCall(RtmpHeader header, RtmpMessageCommand command) {
		//String procedureName = command.getName();
	}	
	
	
	public void writeError(int chunkStreamId, int streamId, int transactionId, String code, String msg) throws IOException {
		HashMap<String, AmfValue> value = new HashMap<String, AmfValue>();
		value.put("level", new AmfValue("error"));
		value.put("code", new AmfValue());
		value.put("details", new AmfValue(msg));
		AmfValue[] argsMessageCommand = { new AmfNull(), new AmfValue(value) };
		rtmp.writeRtmpMessage(chunkStreamId, streamId, -1, 
				  new RtmpMessageCommand("onStatus", transactionId, argsMessageCommand));
	}

	public void netConnectionError(int chunkStreamId, int streamId, int transactionId, String msg) throws IOException {
		writeError(chunkStreamId, streamId, transactionId, "NetConnection", msg);
	}
	
	public void streamError(int chunkStreamId, int streamId, int transactionId, String msg) throws IOException {
		writeError(chunkStreamId, streamId, transactionId, "NetStream.Error", msg);
	}
	
	public void readAndProcessRtmpMessage() throws IOException, RtmpException, AmfException {
		if (!handshake.isHandshakeDone()) {
			handshake.doServerHandshake();
			return;
		}
		
		if (!rtmp.readRtmpMessage()) return;
		RtmpHeader header = rtmp.getCurrentHeader();
		RtmpMessage message = rtmp.getCurrentMessage();
		try {
			switch( message.getType() ) {
			case RtmpMessage.MESSAGE_AMF0_COMMAND:
			case RtmpMessage.MESSAGE_AMF3_COMMAND:
				onCommandMessage(header, message);
				break;
			case RtmpMessage.MESSAGE_AUDIO:
			case RtmpMessage.MESSAGE_VIDEO:
			case RtmpMessage.MESSAGE_AMF0_DATA:
			case RtmpMessage.MESSAGE_AMF3_DATA:
				onMediaMessage(header, message);
				break;
			case RtmpMessage.MESSAGE_USER_CONTROL:
				RtmpMessageUserControl userControl = (RtmpMessageUserControl)message;
				System.out.println("read message USER_CONTROL:" + userControl.getEvent() + ":" + userControl.getStreamId() + ":" + userControl.getTimestamp());
				break;
			case RtmpMessage.MESSAGE_SHARED_OBJECT:
				System.out.println("read message SHARED OBJECT:");
				onSharedMessage(header, message);
				break;
			case RtmpMessage.MESSAGE_CHUNK_SIZE:
				System.out.println("read message chunk size:");
				break;
			case RtmpMessage.MESSAGE_ABORT:
				System.out.println("read message abort:");
				break;
			case RtmpMessage.MESSAGE_ACK:
				RtmpMessageAck ack = (RtmpMessageAck)message;
				System.out.println("read message ACK:" + ack.getBytes());
				break;
			case RtmpMessage.MESSAGE_WINDOW_ACK_SIZE:
				RtmpMessageWindowAckSize ackSize = (RtmpMessageWindowAckSize)message;
				System.out.println("read message window ack size:" + ackSize.getSize());
				break;
			case RtmpMessage.MESSAGE_PEER_BANDWIDTH:
				System.out.println("read message peer bandwidth:");
				break;
			case RtmpMessage.MESSAGE_AGGREGATE:
				System.out.println("read message aggregate:");
				break;
			case RtmpMessage.MESSAGE_UNKNOWN:
				System.out.println("read message UNKNOWN:");
				break;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void playStreams() {
		for(NetStream stream : streams.values()) {
			if(stream != null) {
				IPlayer player = stream.getPlayer();
				if (player != null) {
					try {
						player.play();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	

	public void close() {
		for(NetStream stream : streams.values()) {
			if( stream != null )
				try {
					closeStream(stream);
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		streams.clear();
	}

	private NetStream createStream() {
		int id;
		for(id = 1; id <= streams.size(); id++ ) {
			if( streams.get(id) == null )
				break;
		}
		NetStream stream = new NetStream(rtmp, id);
		streams.put(id, stream);
		return stream;
	}
	
	private void closeStream(NetStream stream) throws IOException {
		stream.close();
		streams.remove(stream.getStreamId());
	}
}
