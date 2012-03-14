package com.ams.server;

import java.io.*;
import java.net.SocketAddress;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;

import com.ams.util.Log;

public class SocketAcceptor implements IAcceptor {
	private SocketAddress listenEndpoint;
	private SocketProperties socketProperties = null;
	private ServerSocketChannel serverChannel;
	private Selector selector;
	private ArrayList<Dispatcher> dispatchers;
	private int nextDispatcher = 0;
	private boolean running = true;
	private ConnectionListner listner;
	
	public SocketAcceptor(SocketAddress host) throws IOException {
		serverChannel = ServerSocketChannel.open();
		listenEndpoint = host;
		serverChannel.socket().bind(host);
		serverChannel.configureBlocking(false);
		selector = SelectorFactory.getInstance().get();
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);
	}

	public void run() {
		int selectedKeys = 0;
		while (running) {
			try {
				selectedKeys = selector.select();
			} catch (Exception e) {
				if (selector.isOpen()) {
					continue;
				} else {
					Log.logger.warning(e.getMessage());
					try {
						SelectorFactory.getInstance().free(selector);
						selector = SelectorFactory.getInstance().get();
						serverChannel
								.register(selector, SelectionKey.OP_ACCEPT);
					} catch (Exception e1) {
					}
				}
			}
			if (selectedKeys == 0) {
				continue;
			}

			Iterator<SelectionKey> it = selector.selectedKeys().iterator();
			while (it.hasNext()) {
				SelectionKey key = it.next();
				it.remove();

				if (!key.isValid()) {
					continue;
				}

				try {
					ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
					SocketChannel channel = serverChannel.accept();

					if (socketProperties != null) {
						socketProperties.setSocketProperties(channel.socket());
					}

					if (dispatchers != null) {
						Dispatcher dispatcher = dispatchers.get(nextDispatcher++);
						SocketConnector connector = new SocketConnector(channel);
						connector.addListner(listner);
						dispatcher.addChannelToRegister(new ChannelInterestOps(channel, SelectionKey.OP_READ, connector));
						if (nextDispatcher >= dispatchers.size()) {
							nextDispatcher = 0;
						}
					}
				} catch (Exception e) {
					key.cancel();
					Log.logger.warning(e.getMessage());
				}
			}
		}
		
		try {
			serverChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setSocketProperties(SocketProperties socketProperties) {
		this.socketProperties = socketProperties;
	}

	public void setDispatchers(ArrayList<Dispatcher> dispatchers) {
		this.dispatchers = dispatchers;
	}

	public SocketAddress getListenEndpoint() {
		return this.listenEndpoint;
	}
	
	public void stop() {
		running = false;
	}

	public void start() {
		running = true;
	}

	public void setConnectionListner(ConnectionListner listner) {
		this.listner = listner;
	}

}
