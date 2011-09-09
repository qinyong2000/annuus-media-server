package com.ams.server;

import java.net.Socket;
import java.net.SocketException;

public final class SocketProperties {
	private int rxBufSize = 25188;
	private int txBufSize = 43800;
	private boolean tcpNoDelay = true;
	private boolean soKeepAlive = false;
	private boolean ooBInline = true;
	private boolean soReuseAddress = true;
	private boolean soLingerOn = true;
	private int soLingerTime = 25;
	private int soTimeout = 5000;
	private int soTrafficClass = 0x04 | 0x08 | 0x010;
	private int performanceConnectionTime = 1;
	private int performanceLatency = 0;
	private int performanceBandwidth = 1;

	public void setSocketProperties(Socket socket) throws SocketException {
		socket.setReceiveBufferSize(rxBufSize);
		socket.setSendBufferSize(txBufSize);
		socket.setOOBInline(ooBInline);
		socket.setKeepAlive(soKeepAlive);
		socket.setPerformancePreferences(performanceConnectionTime,
				performanceLatency, performanceBandwidth);
		socket.setReuseAddress(soReuseAddress);
		socket.setSoLinger(soLingerOn, soLingerTime);
		socket.setSoTimeout(soTimeout);
		socket.setTcpNoDelay(tcpNoDelay);
		socket.setTrafficClass(soTrafficClass);
	}

}
