package com.ams.server;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.ArrayList;

public class MulticastAcceptor implements IAcceptor {
	private SocketAddress listenEndpoint;
	private DatagramChannel datagramChannel;
	private ConnectionListner listner;
	
	public MulticastAcceptor(SocketAddress host, SocketAddress multicastGroupAddr) throws IOException {
		datagramChannel = DatagramChannel.open();
		datagramChannel.socket().bind(host);
		datagramChannel.configureBlocking(false);
		
		listenEndpoint = multicastGroupAddr;
	}
	

	public void setDispatchers(ArrayList<Dispatcher> dispatchers) {
		MulticastConnector connector = new MulticastConnector(datagramChannel, listenEndpoint);
		connector.joinGroup(listenEndpoint);
		connector.configureSocket();
		connector.addListner(listner);
		dispatchers.get(0).addChannelToRegister(new ChannelInterestOps(datagramChannel, SelectionKey.OP_READ, connector));
	}
	

	public SocketAddress getListenEndpoint() {
		return listenEndpoint;
	}
	
	public synchronized void stop() {
		try {
			datagramChannel.close();
		} catch (IOException e) {
		}
		notifyAll();
	}

	public void run() {
		try {
			synchronized (this) {
				this.wait();
			}
		} catch (InterruptedException e) {
		}
	}

	public void start() {
	}

	public void setConnectionListner(ConnectionListner listner) {
		this.listner = listner;
	}
	
}
