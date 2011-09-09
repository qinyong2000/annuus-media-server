package com.ams.server;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.ams.server.protocol.IProtocolHandler;

public class WorkerQueue {
	private int nThreads;
	private Worker[] threads;
	private ConcurrentLinkedQueue<IProtocolHandler> handlerQueue;
	private boolean running = true;

	public WorkerQueue(int nThreads) {
		this.handlerQueue = new ConcurrentLinkedQueue<IProtocolHandler>();
		this.nThreads = nThreads;
		this.threads = new Worker[nThreads];
	}

	public void execute(IProtocolHandler h) {
		synchronized (handlerQueue) {
			handlerQueue.add(h);
			handlerQueue.notify();
		}
	}

	public void start() {
		running = true;
		for (int i = 0; i < nThreads; i++) {
			threads[i] = new Worker(handlerQueue);
			threads[i].start();
		}
	}

	public void stop() {
		running = false;
		handlerQueue.clear();
	}

	private class Worker extends Thread {
		private ConcurrentLinkedQueue<IProtocolHandler> handlerQueue;

		public Worker(ConcurrentLinkedQueue<IProtocolHandler> handlerQueue) {
			this.handlerQueue = handlerQueue;
		}

		public void run() {
			while (running) {
				IProtocolHandler handler;
				synchronized (handlerQueue) {
					while (handlerQueue.isEmpty()) {
						try {
							handlerQueue.wait();
						} catch (InterruptedException ignored) {
						}
					}
					handler = handlerQueue.poll();
				}

				try {
					handler.run();
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
				if (handler.isKeepAlive()) {
					synchronized (handlerQueue) {
						handlerQueue.add(handler);
						handlerQueue.notify();
					}
				}
			}
		}
	}

}