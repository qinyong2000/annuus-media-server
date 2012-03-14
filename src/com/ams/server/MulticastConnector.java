package com.ams.server;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class MulticastConnector extends Connector {
	class ReadFrameBuffer {
		private ByteBuffer[] buffers;
		private int lastFrameIndex = 0;
		
		public ReadFrameBuffer(int capacity) {
			buffers = new ByteBuffer[capacity];
		}
		
		public void set(int index, ByteBuffer buf) {
			int capacity = buffers.length;
			if (index >= capacity) {
				ByteBuffer[] tmp = new ByteBuffer[capacity * 3 / 2];
				System.arraycopy(buffers, 0, tmp, 0, index);
				buffers = tmp;
			}
			buffers[index] = buf;
		}
		
		public void lastFrame(int index) {
			lastFrameIndex = index;
		}
		
		public boolean hasLostFrame() {
			for(int i = 0; i <= lastFrameIndex; i++ ) {
				if (buffers[i] == null) return true;
			}
			return false;
		}
		
		public ByteBuffer[] getFrames() {
			ByteBuffer[] buf = new ByteBuffer[lastFrameIndex + 1];
			System.arraycopy(buffers, 0, buf, 0, lastFrameIndex + 1);
			return buf;
		}
		
		public void clear() {
			for(int i = 0; i < buffers.length; i++ ) {
				buffers[i] = null; 
			}
			lastFrameIndex = 0;
		}
	}
	private static final int MAX_DATA_SIZE = 8192;
	private HashMap<SocketAddress, MulticastConnector> connectorMap = new HashMap<SocketAddress, MulticastConnector>();
	private DatagramChannel channel;
	private SocketAddress groupAddr;
	private SocketAddress peer;
	private static ArrayList<InetAddress> localAddress = new ArrayList<InetAddress>();
	static {
		try {
			localAddress = getLocalAddress();
		} catch (IOException e) {
		}
	}

	private int currentSession = 0;
	private ReadFrameBuffer readFrameBuffer = new ReadFrameBuffer(256);
	
	private int session = (int) System.currentTimeMillis();
	private int writeDelayTime = 10;	// nano second
	
	public MulticastConnector() {
		super();
	}

	public MulticastConnector(DatagramChannel channel, SocketAddress groupAddr) {
		super();
		this.channel = channel;
		this.groupAddr = groupAddr;
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void invokeSocketMethod(String method, Class[] paramClass, Object[] param) {
		try {
            // http://www.mernst.org/blog/archives/12-01-2006_12-31-2006.html
            // UGLY UGLY HACK: multicast support for NIO
            // create a temporary instanceof PlainDatagramSocket, set its fd and configure it
            Constructor<? extends DatagramSocketImpl> c =
              (Constructor<? extends DatagramSocketImpl>)Class.forName("java.net.PlainDatagramSocketImpl").getDeclaredConstructor();
            c.setAccessible(true);
            DatagramSocketImpl socketImpl = c.newInstance();
            Field channelFd = Class.forName("sun.nio.ch.DatagramChannelImpl").getDeclaredField("fd");
            channelFd.setAccessible(true);
            Field socketFd = DatagramSocketImpl.class.getDeclaredField("fd");
            socketFd.setAccessible(true);
            socketFd.set(socketImpl, channelFd.get(channel));
            try {
                Method m = DatagramSocketImpl.class.getDeclaredMethod(method, paramClass);
                m.setAccessible(true);
                m.invoke(socketImpl, param);
            } catch (Exception e) {
                throw e;
            } finally {
                // important, otherwise the fake socket's finalizer will nuke the fd
                socketFd.set(socketImpl, null);
            }
        } catch (Exception e) {
        }
	}

	@SuppressWarnings("unchecked")
	public void joinGroup(SocketAddress groupAddr) {
        invokeSocketMethod("joinGroup", new Class[]{SocketAddress.class, NetworkInterface.class}, new Object[]{groupAddr, null});
	}
	
	public void configureSocket() {
        invokeSocketMethod("setTimeToLive", new Class[]{Integer.class}, new Object[]{1});
        invokeSocketMethod("setLoopbackMode", new Class[]{Boolean.class}, new Object[]{true});
	}
	
	public void connect(String host, int port) throws IOException {
		if (channel == null || !channel.isOpen()) {
			channel = DatagramChannel.open();
			channel.configureBlocking(false);
			channel.socket().bind(new InetSocketAddress(0));
			configureSocket();
			getDispatcher().addChannelToRegister(new ChannelInterestOps(channel, SelectionKey.OP_READ, this));
			groupAddr = new InetSocketAddress(host, port);
			open();
		}
	}

	public void finishConnect(SelectionKey key) throws IOException {
		// nothing to do
	}
	
	public void readChannel(SelectionKey key) throws IOException {
		ByteBuffer readBuffer = ByteBufferFactory.allocate(MAX_DATA_SIZE);
		SocketAddress remote = null;
		if ((remote = channel.receive(readBuffer)) != null) {
			// if remote is self, drop the packet
			if (localAddress.contains(((InetSocketAddress)remote).getAddress())) {
				return;
			}
		
			MulticastConnector conn = connectorMap.get(remote);
			if (conn == null) {
				conn = new MulticastConnector(channel, groupAddr);
				conn.setPeer(remote);
				for(ConnectionListner listener : listners) {
					conn.addListner(listener);
				}
				connectorMap.put(remote, conn);
			}
			readBuffer.flip();
			conn.handleIncoming(readBuffer);
		}
	}

	protected void writeToChannel(ByteBuffer[] buf) throws IOException {
		ArrayList<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
		ByteBuffer frame = null;
		short sequnce = 0;
		int i = 0;
		session++;
		while (i < buf.length) {
			if (frame == null) {
				frame = ByteBufferFactory.allocate(MAX_DATA_SIZE);
				frame.putInt(session);
				frame.put((byte) 0);
				frame.putShort(sequnce++);
			}
			ByteBuffer data = buf[i];
			int remaining = frame.remaining();
			if (remaining >= data.remaining()) {
				frame.put(data);
				i++;
			} else {
				if (remaining > 0) {
					ByteBuffer slice = data.slice();
					slice.limit(remaining);
					data.position(data.position() + remaining);

					frame.put(slice);
				}
				buffers.add(frame);
				frame = null;
			}
		}
		if (frame != null) {
			buffers.add(frame);
		}

		// session id: 4 byte
		// payload type: 1 byte, 0: data frame, 1: last frame, 0x10: join group, 0x11: leave group 
		// sequnce number: 2 byte
		ByteBuffer[] frames = buffers.toArray(new ByteBuffer[buffers.size()]);
		frames[frames.length - 1].put(4, (byte) 1);
		
		handleOutgoing(groupAddr, frames);
	}

	private void handleIncoming(ByteBuffer frame) {
		keepAlive();
		
		int sessionId = frame.getInt();
		byte payloadType = frame.get();
		short sequnceNum = frame.getShort();
		ByteBuffer data = frame.slice();
		
		if (currentSession == 0) {
			currentSession = sessionId;
		}
		if (sessionId < currentSession) {
			// drop this frame
			return;
		}
		if (sessionId > currentSession) {
			currentSession = sessionId;
			readFrameBuffer.clear();
		}
		
		readFrameBuffer.set(sequnceNum, data);
		
		if (payloadType == 1) {	// last frame?
			readFrameBuffer.lastFrame(sequnceNum);
			if (!readFrameBuffer.hasLostFrame()) {
				offerReadBuffer(readFrameBuffer.getFrames());
				if (isClosed()) {
					open();
				}
			}
			currentSession = 0;
			readFrameBuffer.clear(); 
		}
	}
	
	private void handleOutgoing(SocketAddress remote, ByteBuffer[] frames) throws IOException {
		Selector writeSelector = null;
		SelectionKey writeKey = null;
		int writeTimeout = 1000;
		try {
			for(ByteBuffer frame : frames) {
				int retry = 0;
				frame.flip();
				while(channel.send(frame, remote) == 0) {
					retry++;
					writeDelayTime += 10;
					// Check if there are more to be written.
					if (writeSelector == null) {
						writeSelector = SelectorFactory.getInstance().get();
						try {
							writeKey = channel.register(writeSelector, SelectionKey.OP_WRITE);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					if (writeSelector.select(writeTimeout) == 0) {
						if (retry > 2) {
							throw new IOException("Client disconnected");
						}
					}
				}
				try {
					TimeUnit.NANOSECONDS.sleep(writeDelayTime);
				} catch (InterruptedException e) {
				} 
			}
		} finally {
			if (writeKey != null) {
				writeKey.cancel();
				writeKey = null;
			}
			if (writeSelector != null) {
				SelectorFactory.getInstance().free(writeSelector);
			}
			keepAlive();
		}

	}
	
	public long getKeepAliveTime() {
		return System.currentTimeMillis();
	}
	
	public SocketAddress getLocalEndpoint() {
		return channel.socket().getLocalSocketAddress();
	}

	public SocketAddress getRemoteEndpoint() {
		return channel.socket().getRemoteSocketAddress();
	}

	public SelectableChannel getChannel() {
		return channel;
	}

	public SocketAddress getPeer() {
		return peer;
	}

	public void setPeer(SocketAddress addr) {
		this.peer = addr;
	}
	
}
