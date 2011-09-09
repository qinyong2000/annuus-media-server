package com.ams.server.protocol;

import com.ams.server.Connector;

public interface IProtocolHandler extends Runnable {
	public IProtocolHandler newInstance(Connector conn);
	public void setConnection(Connector conn);
	public void clear();
	public boolean isKeepAlive();
}