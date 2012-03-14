package com.ams.server;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import com.ams.util.Log;

public final class SlabByteBufferAllocator implements IByteBufferAllocator{
	private int minChunkSize = 16;
	private int maxChunkSize = 128 * 1024;
	private int pageSize = 8 * 1024 * 1024;
	private float factor = 2.0f;
	private double logFactor;

	private ArrayList<Slab> slabs;
	private class Slab {
		private ArrayList<Page> pages;
		private int chunkSize;
		
		public Slab(int chunkSize) {
			this.chunkSize = chunkSize;
			pages = new ArrayList<Page>();
			pages.add(new Page(chunkSize, pageSize / chunkSize ));
		}
		
		public synchronized ByteBuffer allocate() {
			ByteBuffer buf = null;
			for(Page page : pages) {
				if (page.isEmpty()) {
					buf = page.allocate();
					if (buf != null) {
						return buf;
					}
				}
			}
			return null;
		}
		
		public synchronized Page grow() {
			Page page = new Page(chunkSize, pageSize / chunkSize );
			pages.add(page);
			Log.logger.info("grow a page: " + chunkSize);
			return page;
		}
		
		public boolean isEmpty() {
			for(Page page : pages) {
				if (page.isEmpty()) {
					return true;
				}
			}	
			return false;
		}
		
	}
	
	private class Page {
		private boolean empty = true;
		private int chunkSize;
		private ByteBuffer chunks;
		private BitSet chunkBitSet;
		private int bitSize;
		private int currentIndex = 0;
		
		public Page(int chunkSize, int n) {
			this.chunks = ByteBuffer.allocateDirect(chunkSize * n);
			this.chunkSize = chunkSize;
			this.chunkBitSet = new BitSet(n);
			this.bitSize = n;
		}
		
		public synchronized ByteBuffer allocate() {
			int index = chunkBitSet.nextClearBit(currentIndex);
			if (index >= bitSize) {
				index = chunkBitSet.nextClearBit(0);
				if (index >= bitSize) {
					empty = false;
					return null;		// should allocate a new page
				}
			}
			// slice a buffer
			chunkBitSet.set(index, true);
			chunks.position(index * chunkSize);
			chunks.limit(chunks.position() + chunkSize);
			ByteBuffer slice = chunks.slice();
			referenceList.add(new ChunkReference(slice, this, index));
			currentIndex = index + 1;
			chunks.clear();
			return slice;
		}
		
		public synchronized void free(int index) {
			chunkBitSet.set(index, false);
			empty = true;
		}

		public boolean isEmpty() {
			return empty;
		}
	}

	private class ChunkReference extends WeakReference<ByteBuffer> {
		private Page page;
		private int chunkIndex;

		public ChunkReference(ByteBuffer buf, Page page , int index) {
			super(buf, chunkReferenceQueue);
			this.page = page;
			this.chunkIndex = index;
		}

		public Page getPage() {
			return page;
		}

		public int getChunkIndex() {
			return chunkIndex;
		}
	}
	
	private static ReferenceQueue<ByteBuffer> chunkReferenceQueue = new ReferenceQueue<ByteBuffer>();
	private static List<ChunkReference> referenceList = Collections.synchronizedList(new LinkedList<ChunkReference>());
	private void collect() {
		System.gc();
		ChunkReference r;
	    while((r = (ChunkReference)chunkReferenceQueue.poll()) != null) {
	    	Page page = r.getPage();
	    	int index = r.getChunkIndex();
	    	page.free(index);
	    	referenceList.remove(r);
	    }
	}
	
	private class ByteBufferCollector extends Thread {
		private static final int COLLECT_INTERVAL_MS = 1000;

		public ByteBufferCollector() {
			super();
			try {
				setDaemon(true);
			} catch (Exception e) {
			}
		}

		public void run() {
			try {
				while (! Thread.interrupted()) {
					sleep(COLLECT_INTERVAL_MS);
					collect();
				}
			} catch (InterruptedException e) {
				interrupt();
			}
		}
	}

	public SlabByteBufferAllocator (){
		ByteBufferCollector collector = new ByteBufferCollector();
		collector.start();
		
		logFactor = Math.log(factor);
	}
	
	public void init() {
		// Initialize Slabs
		slabs = new ArrayList<Slab>();
		int size = minChunkSize;
		while(size <= maxChunkSize) {
			slabs.add(new Slab(size));
			size *= factor;
		}
		logFactor = Math.log(factor);
	}
	
	private int getSlabIndex(int size) {
		if (size <= 0) return 0;
		int index = (int) Math.ceil(Math.log((float)size / minChunkSize) / logFactor);
		if (index < 0) return 0;
		return index;
	}
	
	public void setMinChunkSize(int minChunkSize) {
		this.minChunkSize = minChunkSize;
	}

	public void setMaxChunkSize(int maxChunkSize) {
		this.maxChunkSize = maxChunkSize;
	}
	
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	
	public void setFactor(float factor) {
		this.factor = factor;
	}
	
	public synchronized ByteBuffer allocate(int size) {
		if (slabs == null) {
			init();
		}
		// find a slab to allocate
		ByteBuffer buf = null;
		Slab slab = null;
		int index = getSlabIndex(size);
		for (int retry = 0; retry < 2; retry ++) {
			for (int i = 0, slabSize = slabs.size(); i < 3 && index + i < slabSize; i++) {
				slab = slabs.get(index + i);
				if (slab.isEmpty()) {
					buf = slab.allocate();
					if (buf != null) {
						return buf;
					} 
				}
			}
			// collect free buffers
			Log.logger.info("collect free buffers");
			collect();
		}
		//grow the selected slab and allocate a chunk
		slab = slabs.get(index);
		Page page = slab.grow();
		return page.allocate();
	}
}
