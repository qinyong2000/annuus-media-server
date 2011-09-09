package com.ams.util;

import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class ObjectPool<T> {
	protected ConcurrentLinkedQueue<T> pool = new ConcurrentLinkedQueue<T>();

	protected abstract void assemble(T obj);

	protected abstract void dispose(T obj);

	protected abstract T newInstance();

	public void grow(int size) {
		for (int i = 0; i < size; i++) {
			T obj = newInstance();
			if (obj != null) {
				pool.offer(obj);
			}
		}
	}

	public boolean recycle(T obj) {
		if (obj != null) {
			dispose(obj);
			return pool.offer(obj);
		} else {
			return false;
		}
	}

	public T get() {
		T obj = pool.poll();
		if (obj == null) {
			obj = newInstance();
		}
		assemble(obj);
		return obj;
	}

}
