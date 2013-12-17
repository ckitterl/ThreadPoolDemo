package com.larry.threadpooldemo;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 解决任务在暂存态和运行态之间转换的过程中，无法同步的问题
 * 
 * @author caimk
 * 
 */
public class NdPriorityBlockingQueue<E> extends
		PriorityBlockingQueue<E> {

	private static final long serialVersionUID = 1L;

	public NdPriorityBlockingQueue(int i) {
		super(i);
	}

	@Override
	public boolean offer(E e, long timeout, TimeUnit unit) {
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
