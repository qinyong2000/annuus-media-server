package com.ams.server.replicator;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import com.ams.message.MediaMessage;
import com.ams.rtmp.message.RtmpMessage;
import com.ams.server.Connector;
import com.ams.server.MulticastConnector;
import com.ams.server.SocketConnector;

public class ReplicateCluster {
	private static ArrayList<Replicator> replicators = new ArrayList<Replicator>();

	public static void establishTcpReplicator(String[] slaves, int port)
			throws IOException {
		if (slaves == null)
			return;
		for (int i = 0; i < slaves.length; i++) {
			String host = slaves[i];
			if (!isLocalHost(host)) {
				Replicator replicator = new Replicator(new SocketConnector(), host, port);
				replicators.add(replicator);
				new Thread(replicator).start();
			}
		}
	}

	public static boolean isLocalHost(String host) throws IOException {
		InetAddress hostAddr = InetAddress.getByName(host);
		ArrayList<InetAddress> address = Connector.getLocalAddress();
		return address.contains(hostAddr);
	}

	public static void establishMulticastReplicator(String group, int port)
			throws IOException {
		Replicator replicator = new Replicator(new MulticastConnector(), group,
				port);
		replicators.add(replicator);
		new Thread(replicator).start();
	}

	public static void publishMessage(String publishName, MediaMessage msg) {
		for (Replicator replicator : replicators) {
			replicator.publishMessage(publishName, msg);
		}
	}
	
	public static void close() {
		for (Replicator replicator : replicators) {
			replicator.close();
		}
		replicators.clear();
	}
}
