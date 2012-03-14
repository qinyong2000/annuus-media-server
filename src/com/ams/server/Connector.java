package com.ams.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import com.ams.io.ByteBufferInputStream;
import com.ams.io.ByteBufferOutputStream;
import com.ams.io.IByteBufferReader;
import com.ams.io.IByteBufferWriter;

public abstract class Connector implements IByteBufferReader, IByteBufferWriter {
	protected static final int DEFAULT_TIMEOUT_MS = 30000;
	protected static final int MAX_QUEUE_SIZE = 1024;
	protected ConcurrentLinkedQueue<ByteBuffer> readQueue = new ConcurrentLinkedQueue<ByteBuffer>();
	protected ConcurrentLinkedQueue<ByteBuffer> writeQueue = new ConcurrentLinkedQueue<ByteBuffer>();
	protected ByteBuffer writeBuffer = null;
	protected AtomicLong available = new AtomicLong(0);
	protected int timeout = DEFAULT_TIMEOUT_MS;
	protected long keepAliveTime;
	protected boolean keepAlive = false;
	protected boolean closed = true;

	protected ArrayList<ConnectionListner> listners = new ArrayList<ConnectionListner>();

	protected ByteBufferInputStream in;
	protected ByteBufferOutputStream out;

	private static Dispatcher dispatcher = null;
	private static Object dispatcherLock = new Object();
	
	public Connector() {
		this.in = new ByteBufferInputStream(this);
		this.out = new ByteBufferOutputStream(this);
		keepAlive();
	}

	public abstract void connect(String host, int port) throws IOException;

	public abstract void finishConnect(SelectionKey key) throws IOException;

	public abstract void readChannel(SelectionKey key) throws IOException;

	protected abstract void writeToChannel(ByteBuffer[] data) throws IOException;

	public abstract SocketAddress getLocalEndpoint();

	public abstract SocketAddress getRemoteEndpoint();

	public abstract SelectableChannel getChannel();

	protected Dispatcher getDispatcher() throws IOException {
		if (dispatcher == null) {
			synchronized(dispatcherLock) {
				dispatcher = new Dispatcher();
				Thread t = new Thread(dispatcher, "dispatcher");
				t.setDaemon(true);
				t.start();
			}
		}
		return dispatcher;
	}
	
	protected void keepAlive() {
		keepAliveTime = System.currentTimeMillis();
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	public long getKeepAliveTime() {
		return keepAliveTime;
	}

	public synchronized void open() {
		keepAlive();
		if (!isClosed()) return;
		closed = false;
		for (ConnectionListner listner : listners) {
			listner.connectionEstablished(this);
		}
	}

	public synchronized void close() {
		if (isClosed()) return;
		closed = true;
		if (!isKeepAlive()) {
			Channel channel = getChannel();
			try {
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		for (ConnectionListner listner : listners) {
			listner.connectionClosed(this);
		}

	}

	public boolean isClosed() {
		return closed;
	}

	public boolean isWriteBlocking() {
		return writeQueue.size() > MAX_QUEUE_SIZE;
	}

	public long available() {
		return available.get();
	}

	public ByteBuffer peek() {
		return readQueue.peek();
	}

	protected void offerReadBuffer(ByteBuffer buffers[]) {
		for(ByteBuffer buffer : buffers) {
			readQueue.offer(buffer);
			available.addAndGet(buffer.remaining());
		}
		keepAlive();
		synchronized (readQueue) {
			readQueue.notifyAll();
		}
	}

	public ByteBuffer[] read(int size) throws IOException {
		ArrayList<ByteBuffer> list = new ArrayList<ByteBuffer>();
		int length = size;
		while (length > 0) {
			// read a buffer with blocking
			ByteBuffer buffer = readQueue.peek();
			if (buffer != null) {
				int remain = buffer.remaining();

				if (length >= remain) {
					list.add(readQueue.poll());
					length -= remain;
					available.addAndGet(-remain);
				} else {
					ByteBuffer slice = buffer.slice();
					slice.limit(length);
					buffer.position(buffer.position() + length);
					list.add(slice);
					available.addAndGet(-length);
					length = 0;
				}
			} else {
				// wait new buffer append to queue
				// sleep for timeout ms
				long start = System.currentTimeMillis();
				try {
					synchronized (readQueue) {
						readQueue.wait(timeout);
					}
				} catch (InterruptedException e) {
					throw new IOException("read interrupted");
				}
				long now = System.currentTimeMillis();
				if (now - start >= timeout) {
					throw new IOException("read time out");
				}
			}
		} // end while
		return list.toArray(new ByteBuffer[list.size()]);
	}

	public void write(ByteBuffer[] data) throws IOException {
		for (ByteBuffer buf : data) {
			writeQueue.offer(buf);
		}
	}

	public void flush() throws IOException {
		if (out != null) out.flush();
		ArrayList<ByteBuffer> writeBuffers = new ArrayList<ByteBuffer>();
		ByteBuffer data;
		boolean hasData = false;
		while ((data = writeQueue.poll()) != null) {
			writeBuffers.add(data);
			hasData = true;
		}
		if (hasData) {
			writeToChannel(writeBuffers.toArray(new ByteBuffer[writeBuffers.size()]));
		}
	}
	
	public boolean waitDataReceived(int time) {
		if (available.get() == 0) {
			long start = System.currentTimeMillis();
			try {
				synchronized (readQueue) {
					readQueue.wait(time);
				}
			} catch (InterruptedException e) {
			}
			
			long now = System.currentTimeMillis();
			if (now - start >= timeout) {
				return false;
			}
		}
		return true;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public ByteBufferInputStream getInputStream() {
		return in;
	}

	public ByteBufferOutputStream getOutputStream() {
		return out;
	}

	public synchronized void addListner(ConnectionListner listener) {
		this.listners.add(listener);
	}

	public synchronized boolean removeListner(ConnectionListner listener) {
		return this.listners.remove(listener);
	}
	
	public static ArrayList<InetAddress> getLocalAddress() throws IOException {
		ArrayList<InetAddress> address = new ArrayList<InetAddress>();
		for (Enumeration<NetworkInterface> ifaces = NetworkInterface
				.getNetworkInterfaces(); ifaces.hasMoreElements();) {
			NetworkInterface iface = ifaces.nextElement();
			for (Enumeration<InetAddress> ips = iface.getInetAddresses(); ips
					.hasMoreElements();) {
				InetAddress ia = ips.nextElement();
				address.add(ia);
			}
		}
		return address;
	}
	
}
