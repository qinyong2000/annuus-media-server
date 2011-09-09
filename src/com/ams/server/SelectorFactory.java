package com.ams.server;

import java.io.IOException;
import java.nio.channels.Selector;
import com.ams.util.ObjectPool;

public final class SelectorFactory extends ObjectPool<Selector> {
	private static SelectorFactory instance = null;
	public static int poolSize = 16;

	public SelectorFactory() {
		super();
		grow(poolSize);
	}

	public static synchronized SelectorFactory getInstance() {
		if (instance == null) {
			instance = new SelectorFactory();
		}
		return instance;
	}

	public void free(Selector selector) {
		recycle(selector);
	}

	protected void assemble(Selector obj) {
	}

	protected void dispose(Selector obj) {
		try {
			obj.selectNow();
		} catch (IOException e) {
		}
	}

	protected Selector newInstance() {
		Selector selector = null;
		try {
			selector = Selector.open();
		} catch (IOException e) {
		}
		return selector;
	}

}
