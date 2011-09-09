package com.ams.server;

public interface ConnectionListner {
	public void connectionEstablished(Connector conn);
	public void connectionClosed(Connector conn);
}
