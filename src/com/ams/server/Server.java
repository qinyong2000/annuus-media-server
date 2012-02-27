package com.ams.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import com.ams.config.Configuration;
import com.ams.server.protocol.IProtocolHandler;
import com.ams.server.replicator.ReplicateCluster;
import com.ams.util.Log;

public final class Server implements ConnectionListner {
	private Configuration config;
	private HashMap<SocketAddress, IAcceptor> acceptorMap;
	private ArrayList<Dispatcher> dispatchers;
	private HashMap<SocketAddress, IProtocolHandler> handlerMap;
	private WorkerQueue workerQueue;
	
	public Server(Configuration config) throws IOException {
		initByteBufferFactory(config);
		this.config = config;
		this.acceptorMap = new HashMap<SocketAddress, IAcceptor>();
		this.dispatchers = new ArrayList<Dispatcher>();
		this.handlerMap = new HashMap<SocketAddress, IProtocolHandler>();
		
		int dispatcherSize = config.getDispatcherThreadPoolSize();
		for (int i = 0; i < dispatcherSize; i++) {
			Dispatcher dispatcher = new Dispatcher();
			dispatchers.add(dispatcher);
		}
		int poolSize = config.getWokerThreadPoolSize();
		workerQueue = new WorkerQueue(poolSize);
	}
	
	private void initByteBufferFactory(Configuration config) {
		ByteBufferFactory.setPageSize(config.getSlabPageSize());
		ByteBufferFactory.init();
	}
	
	public void addTcpListenEndpoint(SocketAddress endpoint, IProtocolHandler handler) throws IOException {
		SocketAcceptor acceptor = new SocketAcceptor(endpoint);
		acceptor.setConnectionListner(this);
		acceptor.setSocketProperties(config.getSocketProperties());
		acceptor.setDispatchers(dispatchers);
		acceptorMap.put(endpoint, acceptor);
		handlerMap.put(endpoint, handler);
	}

	public void addMulticastListenEndpoint(SocketAddress endpoint, SocketAddress group, IProtocolHandler handler) throws IOException {
		MulticastAcceptor acceptor = new MulticastAcceptor(endpoint, group);
		acceptor.setConnectionListner(this);
		acceptor.setDispatchers(dispatchers);
		acceptorMap.put(endpoint, acceptor);
		handlerMap.put(endpoint, handler);
	}

	public void removeListenEndpoint(SocketAddress endpoint) {
		IAcceptor acceptor = acceptorMap.get(endpoint);
		if (acceptor != null) {
			acceptor.stop();
			acceptorMap.remove(acceptor);
		}
		
		IProtocolHandler handler = handlerMap.get(endpoint);
		if (handler != null) {
			handler.clear();
			handlerMap.remove(handler);
		}
	}
	
	public void start() {
		workerQueue.start();
		
		for (int i = 0; i < dispatchers.size(); i++) {
			Dispatcher dispatcher = dispatchers.get(i);
			new Thread(dispatcher).start();
		}

		for (SocketAddress endpoint : acceptorMap.keySet()) {
			IAcceptor acceptor = acceptorMap.get(endpoint);
			acceptor.start();
			new Thread(acceptor).start();
			Log.logger.info("Start service on port: " + acceptor.getListenEndpoint());
		}
		
		// establish replicate cluster
		if (config.getReplicationSlaves() != null) {
			try {
				ReplicateCluster.establishTcpReplicator(config.getReplicationSlaves(), config.getReplicationPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (config.getMulticastHost() != null && config.getMulticastGroup() != null) {
			try {
				ReplicateCluster.establishMulticastReplicator(config.getMulticastGroup(), config.getMulticastPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void stop() {
		for (SocketAddress endpoint : acceptorMap.keySet()) {
			IAcceptor acceptor = acceptorMap.get(endpoint);
			acceptor.stop();
		}

		for (int i = 0; i < dispatchers.size(); i++) {
			dispatchers.get(i).stop();
		}

		workerQueue.stop();
		
		ReplicateCluster.close();
		
		Log.logger.info("Server is Stoped.");
	}
	
	public IProtocolHandler getHandler(SocketAddress endpoint) {
		if (handlerMap.containsKey(endpoint)) {
			return (IProtocolHandler)handlerMap.get(endpoint);
		} else {
			if (endpoint instanceof InetSocketAddress) {
				SocketAddress endpointAny = new InetSocketAddress("0.0.0.0", 
						((InetSocketAddress)endpoint).getPort());
				if (handlerMap.containsKey(endpointAny)) {
					return (IProtocolHandler)handlerMap.get(endpointAny);
				}
			}
		}
		return null;
	}
	
	public void connectionEstablished(Connector conn) {
		SocketAddress endpoint = conn.getLocalEndpoint();
		IProtocolHandler handler = getHandler(endpoint);
		if (handler != null) {
			workerQueue.execute(handler.newInstance(conn));
		} else {
			Log.logger.warning("unkown connection port:" + endpoint);
		}
	}

	public void connectionClosed(Connector conn) {
	}

}