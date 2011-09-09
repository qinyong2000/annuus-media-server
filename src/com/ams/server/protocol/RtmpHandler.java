package com.ams.server.protocol;

import com.ams.rtmp.net.NetConnection;
import com.ams.rtmp.net.NetContext;
import com.ams.rtmp.RtmpConnection;
import com.ams.server.Connector;
import com.ams.util.Log;

public class RtmpHandler implements IProtocolHandler {
	private Connector conn;
	private RtmpConnection rtmp;
	private NetConnection netConn;
	private NetContext context;
	private boolean keepAlive = true;
	
	public RtmpHandler(String contextRoot) {
		this.context = new NetContext(contextRoot);
	}

	public RtmpHandler(NetContext context) {
		this.context = context;
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
				netConn.readAndProcessRtmpMessage();
			}

			// write client video/audio streams
			netConn.playStreams();

			// write to socket channel
			conn.flush();
		} catch (Exception e) {
			e.printStackTrace();
			Log.logger.info("rtmp handler exception");
			clear();
		}
	}
	
	public IProtocolHandler newInstance(Connector conn) {
		IProtocolHandler handle = new RtmpHandler(context);
		handle.setConnection(conn);
		return handle;
	}

	public void clear() {
		conn.close();
		netConn.close();
		keepAlive = false;
	}

	public void setConnection(Connector conn) {
		this.conn = conn;
		this.rtmp = new RtmpConnection(conn);
		this.netConn = new NetConnection(rtmp, context);
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}

}