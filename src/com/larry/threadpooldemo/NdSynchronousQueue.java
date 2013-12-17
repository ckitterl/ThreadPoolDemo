package com.larry.threadpooldemo;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class NdSynchronousQueue<E> extends SynchronousQueue<E> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NdSynchronousQueue() {
		super();
	}

	@Override
	public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
		synchronized (NdThreadPoolExecutor.sLock) {
			return super.offer(e, timeout, unit);
		}
	}

	@Override
		public E poll() {
			synchronized (NdThreadPoolExecutor.sLock) {
				return super.poll();
			}
		}

	@Override
	public boolean remove(Object o) {
		synchronized (NdThreadPoolExecutor.sLock) {
			return super.remove(o);
		}
	}

	@Override
	public E take() throws InterruptedException {
		synchronized (NdThreadPoolExecutor.sLock) {
			return super.take();
		}
	}
}
