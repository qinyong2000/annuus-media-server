package com.ams.config;

import com.ams.server.SocketProperties;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public final class Configuration {
	private SocketProperties socketProperties = new SocketProperties();
	private int slabPageSize = 10 * 1024 * 1024;
	private int dispatcherThreadPoolSize = 8;
	private int workerThreadPoolSize = 16;
	private String httpHost = "0.0.0.0";
	private int httpPort = 80;
	private String httpContextRoot = "www";
	private String rtmpHost = "0.0.0.0";
	private int rtmpPort = 1935;
	private String rtmpContextRoot = "video";
	private String replicationHost = null;
	private int replicationPort = 1936;
	private String[] replicationSlaves = null;
	private String multicastHost = null;
	private int multicastPort = 5000;
	private String multicastGroup = "239.0.0.0";

	public boolean read() throws FileNotFoundException {
		boolean result = true;
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("server.conf"));
			String dispatchersProp = prop.getProperty("dispatchers");
			if (dispatchersProp != null) {
				dispatcherThreadPoolSize = Integer.parseInt(dispatchersProp);
			}
			String workersProp = prop.getProperty("workers");
			if (workersProp != null) {
				workerThreadPoolSize = Integer.parseInt(workersProp);
			}
			String host = prop.getProperty("http.host");
			if (host != null) {
				httpHost = host;
			}
			String portProp = prop.getProperty("http.port");
			if (portProp != null) {
				httpPort = Integer.parseInt(portProp);
			}
			String root = prop.getProperty("http.root");
			if (root != null) {
				httpContextRoot = root;
			}

			host = prop.getProperty("rtmp.host");
			if (host != null) {
				rtmpHost = host;
			}
			portProp = prop.getProperty("rtmp.port");
			if (portProp != null) {
				rtmpPort = Integer.parseInt(portProp);
			}

			root = prop.getProperty("rtmp.root");
			if (root != null) {
				rtmpContextRoot = root;
			}

			host = prop.getProperty("replication.host");
			if (host != null) {
				replicationHost = host;
			}

			portProp = prop.getProperty("replication.port");
			if (portProp != null) {
				replicationPort = Integer.parseInt(portProp);
			}

			String slavesProp = prop.getProperty("replication.slaves");
			if (slavesProp != null) {
				replicationSlaves = slavesProp.split(",");
			}

			host = prop.getProperty("replication.multicast.host");
			if (host != null) {
				multicastHost = host;
			}

			portProp = prop.getProperty("replication..multicast.port");
			if (portProp != null) {
				multicastPort = Integer.parseInt(portProp);
			}

			host = prop.getProperty("replication.multicast.group");
			if (host != null) {
				multicastGroup = host;
			}
		} catch (FileNotFoundException e) {
			throw e;
		} catch (IOException e) {
			result = false;
		}

		return result;
	}

	public int getSlabPageSize() {
		return slabPageSize;
	}

	public int getDispatcherThreadPoolSize() {
		return dispatcherThreadPoolSize;
	}

	public int getWokerThreadPoolSize() {
		return workerThreadPoolSize;
	}

	public String getHttpHost() {
		return httpHost;
	}

	public int getHttpPort() {
		return httpPort;
	}

	public String getHttpContextRoot() {
		return httpContextRoot;
	}

	public String getRtmpHost() {
		return rtmpHost;
	}

	public int getRtmpPort() {
		return rtmpPort;
	}

	public String getRtmpContextRoot() {
		return rtmpContextRoot;
	}

	public SocketProperties getSocketProperties() {
		return socketProperties;
	}

	public String getReplicationHost() {
		return replicationHost;
	}

	public int getReplicationPort() {
		return replicationPort;
	}

	public String[] getReplicationSlaves() {
		return replicationSlaves;
	}
	
	public String getMulticastHost() {
		return multicastHost;
	}

	public int getMulticastPort() {
		return multicastPort;
	}
	
	public String getMulticastGroup() {
		return multicastGroup;
	}

}
