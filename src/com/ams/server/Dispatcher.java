package com.ams.server;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.ams.util.Log;

public class Dispatcher implements Runnable {
	private static final long SELECT_TIMEOUT = 2 * 1000;
	private long timeExpire = 2 * 60 * 1000;
	private long lastExpirationTime = 0;
	private Selector selector = null;
	private ConcurrentLinkedQueue<ChannelInterestOps> registerChannelQueue = null;
	private boolean running = true;
	
	public Dispatcher() throws IOException {
		selector = SelectorFactory.getInstance().get();
		registerChannelQueue = new ConcurrentLinkedQueue<ChannelInterestOps>();
	}

	public void addChannelToRegister(ChannelInterestOps channelInterestOps) {
		registerChannelQueue.offer(channelInterestOps);
		selector.wakeup();
	}

	public void run() {
		while (running) {
			// register a new channel
			registerNewChannel();

			// do select
			doSelect();

			// collect idle keys that will not be used
			expireIdleKeys();
		}

		closeAllKeys();
	}

	private void registerNewChannel() {
		try {
			ChannelInterestOps channelInterestOps = null;
			while ((channelInterestOps = registerChannelQueue.poll()) != null) {
				SelectableChannel channel = channelInterestOps.getChannel();
				int interestOps = channelInterestOps.getInterestOps();
				Connector connector = channelInterestOps.getConnector();
				channel.configureBlocking(false);
				channel.register(selector, interestOps, connector);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void doSelect() {
		int selectedKeys = 0;
		try {
			selectedKeys = selector.select(SELECT_TIMEOUT);
		} catch (Exception e) {
			e.printStackTrace();
			if (selector.isOpen()) {
				return;
			}
		}

		if (selectedKeys == 0) {
			return;
		}

		Iterator<SelectionKey> it = selector.selectedKeys().iterator();
		while (it.hasNext()) {
			SelectionKey key = it.next();
			it.remove();

			if (!key.isValid()) {
				continue;
			}
			Connector connector = (Connector)key.attachment();
			try {
				if (key.isConnectable()) {
					connector.finishConnect(key);
				}
				
				if (key.isReadable()) {
					connector.readChannel(key);
				}

			} catch (Exception e) {
				e.printStackTrace();
				key.cancel();
				key.attach(null);
				connector.close();
			}
		}
	}
	private void expireIdleKeys() {
		// check every timeExpire
		long now = System.currentTimeMillis();
		long elapsedTime = now - lastExpirationTime;
		if (elapsedTime < timeExpire) {
			return;
		}
		lastExpirationTime = now;
		
		for (SelectionKey key : selector.keys()) {
			// Keep-alive expired
			Connector connector = (Connector)key.attachment();
			if (connector != null) {
				long keepAliveTime = connector.getKeepAliveTime();
				if (now - keepAliveTime > timeExpire) {
					Log.logger.warning("close expired idle connector!");
					key.cancel();
					key.attach(null);
					connector.close();
				}	
			}
		}
	}

	private void closeAllKeys() {
		// close all keys	
		for (SelectionKey key : selector.keys()) {
			// Keep-alive expired
			Connector connector = (Connector)key.attachment();
			if (connector != null) {
				key.cancel();
				key.attach(null);
				connector.close();
			}	
		}

	}

	public void setTimeExpire(long timeExpire) {
		this.timeExpire = timeExpire;
	}
	
	public void start() {
		running = true;
	}

	public void stop() {
		running = false;
	}

}
