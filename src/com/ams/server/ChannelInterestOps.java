package com.ams.server;

import java.nio.channels.SelectableChannel;

public class ChannelInterestOps {
	private SelectableChannel channel;
	private int interestOps;
	private Connector connector;

	public ChannelInterestOps(SelectableChannel channel, int interestOps, Connector connector) {
		this.channel = channel;
		this.interestOps = interestOps;
		this.connector = connector; 
	}

	public SelectableChannel getChannel() {
		return channel;
	}

	public int getInterestOps() {
		return interestOps;
	}

	public Connector getConnector() {
		return connector;
	}
}
