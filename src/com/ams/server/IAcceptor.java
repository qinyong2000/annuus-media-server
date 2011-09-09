package com.ams.server;

import java.net.SocketAddress;
import java.util.ArrayList;

public interface IAcceptor extends Runnable {
	public void setDispatchers(ArrayList<Dispatcher> dispatchers);
	public SocketAddress getListenEndpoint();
	public void start();
	public void stop();
}