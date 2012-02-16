package com.ams.util;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import com.ams.amf.AmfException;
import com.ams.amf.AmfValue;
import com.ams.rtmp.message.RtmpMessage;
import com.ams.rtmp.message.RtmpMessageCommand;
import com.ams.rtmp.net.IPlayer;
import com.ams.rtmp.net.NetStream;
import com.ams.rtmp.RtmpConnection;
import com.ams.rtmp.RtmpException;
import com.ams.rtmp.RtmpHandShake;
import com.ams.server.SocketConnector;

public class RtmpClient implements Runnable {
	private String fileName;
	private SocketConnector conn;
	private RtmpConnection rtmp;
	private RtmpHandShake handshake;
	private IPlayer player;

	private final static int CMD_CONNECT = 1;
	private final static int CMD_CREATE_STREAM = 2;
	private final static int CMD_PUBLISH = 3;

	private final static int TANSACTION_ID_CONNECT = 1;
	private final static int TANSACTION_ID_CREATE_STREAM = 2;
	private final static int TANSACTION_ID_PUBLISH = 3;
	
	private final static int CHANNEL_RTMP_COMMAND = 3;
	private final static int CHANNEL_RTMP_PUBLISH = 7;
	
	private LinkedList<Integer> commands = new LinkedList<Integer>();
	int streamId = 0;
	String publishName;
	String errorMsg;
	
	public RtmpClient(String fileName, String publishName, String host, int port) throws IOException {
		this.fileName = fileName;
		this.publishName = publishName;
		conn = new SocketConnector();
		conn.connect(host, port);
		rtmp = new RtmpConnection(conn);
		handshake = new RtmpHandShake(rtmp);
	}
	
	private void readResponse() throws IOException, AmfException, RtmpException {
		// waiting for data arriving
		conn.waitDataReceived(100);
		rtmp.readRtmpMessage();
	
		if (!rtmp.isRtmpMessageReady()) return;

		RtmpMessage message = rtmp.getCurrentMessage();
		if (!(message instanceof RtmpMessageCommand)) return;
		
		RtmpMessageCommand msg = (RtmpMessageCommand)message;
		switch (msg.getTransactionId()) {
		case TANSACTION_ID_CONNECT:
			boolean isConnected = connectResult(msg);
			if (isConnected) {
				commands.add(CMD_CREATE_STREAM);
				Log.logger.info("rtmp connected.");
			} else {
				Log.logger.info(errorMsg);
			}
			break;
		case TANSACTION_ID_CREATE_STREAM:
			streamId = createStreamResult(msg);
			if (streamId > 0) {
				commands.add(CMD_PUBLISH);
				Log.logger.info("rtmp stream created.");
			}
			break;
		case TANSACTION_ID_PUBLISH:
			String publishName = publishResult(msg);
			if (publishName != null) {
				NetStream stream = new NetStream(rtmp, streamId);
				player = stream.createPlayer(null, fileName);
				player.seek(0);
				Log.logger.info("rtmp stream published.");
			} else {
				Log.logger.info(errorMsg);
			}
			break;
		}
	}
	
	private void connect() throws IOException {
		AmfValue[] args = {AmfValue.newObject().put("app", "")};
		RtmpMessage message = new RtmpMessageCommand("connect", TANSACTION_ID_CONNECT, args);
		rtmp.writeRtmpMessage(CHANNEL_RTMP_COMMAND, 0, System.currentTimeMillis(), message);
	}

	private boolean connectResult(RtmpMessageCommand msg) {
		if ("_result".equals(msg.getName())) {
			AmfValue[] args = msg.getArgs();
			HashMap<String, AmfValue> result = args[1].object();
			if ("NetConnection.Connect.Success".equals(result.get("code").string())) {
				return true;
			}
		}
		if ("onStatus".equals(msg.getName())) {
			errorMsg = "";
			AmfValue[] args = msg.getArgs();
			HashMap<String, AmfValue> result = args[1].object();
			if ("NetConnection.Error".equals(result.get("code").string())) {
				errorMsg = result.get("details").string();
			}
		}
		
		return false;
	}
	
	private void createStream() throws IOException {
		AmfValue[] args = {new AmfValue(null)};
		RtmpMessage message = new RtmpMessageCommand("createStream", TANSACTION_ID_CREATE_STREAM, args);
		rtmp.writeRtmpMessage(CHANNEL_RTMP_COMMAND, 0, System.currentTimeMillis(), message);
	}

	private int createStreamResult(RtmpMessageCommand msg) {
		int streamId = -1;
		if ("_result".equals(msg.getName())) {
			AmfValue[] args = msg.getArgs();
			streamId = args[1].integer();
		}
		return streamId;
	}

	private void publish(String publishName, int streamId) throws IOException {
		AmfValue[] args = AmfValue.array(null, publishName, "live");
		RtmpMessage message = new RtmpMessageCommand("publish", TANSACTION_ID_PUBLISH, args);
		rtmp.writeRtmpMessage(CHANNEL_RTMP_PUBLISH, streamId, System.currentTimeMillis(), message);
	}

	private String publishResult(RtmpMessageCommand msg) {
		errorMsg = "";
		if ("onStatus".equals(msg.getName())) {
			AmfValue[] args = msg.getArgs();
			HashMap<String, AmfValue> result = args[1].object();
			String level = result.get("level").string();
			if ("status".equals(level)) {
				String publishName = result.get("details").string();
				return publishName;
			} else {
				errorMsg = result.get("details").string();
			}
		}
		return null;
	}
	
	private void closeStream(int streamId) throws IOException {
		AmfValue[] args = {new AmfValue(null)};
		RtmpMessage message = new RtmpMessageCommand("closeStream", 0, args);
		rtmp.writeRtmpMessage(CHANNEL_RTMP_COMMAND, streamId, System.currentTimeMillis(), message);
	}
	
	private boolean handShake() {
		boolean success = true;
		while (!handshake.isHandshakeDone()) {
			try {
				handshake.doClientHandshake();
				// write to socket channel
				conn.flush();
			} catch (Exception e) {
				Log.logger.warning(e.toString());
				success = false;
				break;
			}
		}
		return success;
	}
	
	public void run() {
		if (!handShake()) return;
		Log.logger.info("handshake done.");
		
		commands.add(CMD_CONNECT);
		while (true) {
			try {
				Integer cmd = commands.poll();
				if (cmd != null) {
					switch(cmd) {
					case CMD_CONNECT:			connect();break;
					case CMD_CREATE_STREAM:		createStream();break;
					case CMD_PUBLISH:			publish(publishName, streamId);break;
					}
				}
				if (player != null) {
					player.play();
				}
				readResponse();
				// write to socket channel
				conn.flush();
			} catch (EOFException e) {
				Log.logger.warning("publish end");
				break;
			} catch (Exception e) {
				Log.logger.warning(e.toString());
				break;
			}
			
		}
		
		try {
			closeStream(streamId);
			conn.flush();
			Log.logger.info("stream closed.");
		} catch (IOException e) {
			Log.logger.warning(e.toString());
		}
		
	}
	

	public static void main(String[] args) {
		if (args.length < 3) {
			System.out.println("RtmpClient.main fileName publishName host [port]");
			return;
		}
		String fileName = args[0];
		String publishName = args[1];
		String host = args[2];
		int port;
		if (args.length == 4)
			port = Integer.parseInt(args[3]); 
		else
			port = 1935;
		RtmpClient client;
		try {
			client = new RtmpClient(fileName, publishName, host, port);
			client.run();
		} catch (IOException e) {
			Log.logger.warning(e.toString());
		}	
	}
}
