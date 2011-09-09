package com.ams.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.ams.config.Configuration;
import com.ams.server.protocol.HttpHandler;
import com.ams.server.protocol.RtmpHandler;
import com.ams.server.protocol.SlaveHandler;
import com.ams.util.Log;

public class DaemonThread extends Thread {
	private ServerSocket commandSocket;
	private Server server;

	public DaemonThread() throws IOException {
		commandSocket = new ServerSocket(5555);
		createServerInstance();
	}

	private void createServerInstance() {
		Configuration config = new Configuration();
		try {
			if (!config.read()) {
				Log.logger.info("read config error!");
				return;
			}
		} catch (FileNotFoundException e) {
			Log.logger
					.info("Not found server.conf file, using default setting!");
		}

		try {
			server = new Server(config);
			// http service
			InetSocketAddress httpEndpoint = new InetSocketAddress(
					config.getHttpHost(), config.getHttpPort());
			server.addTcpListenEndpoint(httpEndpoint, new HttpHandler(config.getHttpContextRoot()));
			// rtmp service
			InetSocketAddress rtmpEndpoint = new InetSocketAddress(
					config.getRtmpHost(), config.getRtmpPort());
			server.addTcpListenEndpoint(rtmpEndpoint, new RtmpHandler(config.getRtmpContextRoot()));
			
			// tcp replication service
			if (config.getReplicationHost() != null) {
				InetSocketAddress replicationEndpoint = new InetSocketAddress(config.getReplicationHost(),	config.getReplicationPort());
				server.addTcpListenEndpoint(replicationEndpoint, new SlaveHandler());
			}
			// multicast replication service
			if (config.getMulticastHost() != null) {
				InetSocketAddress multicastEndpoint = new InetSocketAddress(
						config.getMulticastHost(), config.getMulticastPort());
				InetSocketAddress multicastGroup = new InetSocketAddress(
						config.getMulticastGroup(), config.getMulticastPort());
				server.addMulticastListenEndpoint(multicastEndpoint,
						multicastGroup, new SlaveHandler());
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.logger.info(e.getMessage());
		}
	}

	public void run() {
		Log.logger.info("Daemon thread is started.");

		while (true) {
			try {
				Socket socket = commandSocket.accept();
				BufferedReader in = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				String cmd = in.readLine();
				socket.close();
				if ("stop".equalsIgnoreCase(cmd)) {
					server.stop();
				} else if ("start".equalsIgnoreCase(cmd)) {
					server.start();
				} else if ("restart".equalsIgnoreCase(cmd)) {
					server.stop();
					createServerInstance();
					server.start();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
