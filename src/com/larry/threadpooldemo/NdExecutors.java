package com.larry.threadpooldemo;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.Process;

/**
 * @author caimk
 * 
 *         这个类用来实现对线程池的统一管理,除非特殊情况，不然不要直接用使用NdThreadPoolExecutor类去生成线程池
 *         单独生成的线程池必须要在注销或者切换帐号的时候去销毁，并在再次启动
 * 
 */
public class NdExecutors {
	private static NdThreadPoolExecutor sTinyHttpThreadPool; // 网络小数据请求线程池
	private static NdThreadPoolExecutor sCachedThreadPool; // 本地缓存数据请求线程池
	private static NdThreadPoolExecutor sFileUploadDownloadThreadPool; // 文件上传下载操作线程池
	private static NdThreadPoolExecutor sBackgroundThreadPool; // 后台数据同步线程池
	private static PriorityBlockingQueue<Runnable> sNormalWorkQueue = new PriorityBlockingQueue<Runnable>();

	/*
	 * 停止接受新的请求，等待任务执行完成的超时时间，单位ms. 具体查询 {@link #shutdown shutdown}.
	 */
	public static final int AWAIT_TERMINATION_SELF = 3000;

	/*
	 * 强制停止，等待正在执行的任务完成的超时时间，单位ms. 具体查询 {@link #shutdown shutdown}.
	 */
	public static final int AWAIT_TERMINATION_FORCE = 3000; // 强制停止，等待正在执行的任务完成的超时时间，单位ms

	// 网络小数据请求操作的超时时间，如果是超过预定时间，将停止其操作
	private static final long TINY_HTTP_THREAD_POOL_TIMEOUT = 6000; // unit:ms
	// 本地cache操作的超时时间，如果是超过预定时间，将停止其操作
	private static final long CACHE_THREAD_POOL_TIMEOUT = 1000; // unit:ms
	
	public synchronized static void init() {
		if (sTinyHttpThreadPool != null || sCachedThreadPool != null 
				|| sFileUploadDownloadThreadPool != null || sBackgroundThreadPool != null) {
			throw new IllegalStateException("Thread pool have already been initialized");
		}
		getCachedThreadPool(); 
		getTinyHttpThreadPool();
		getFileUploadDownloadThreadPool();
		getBackgroundThreadPool();
	}

	/**
	 * 
	 * @return 本地缓存数据请求线程池
	 */
	public synchronized static ExecutorService getCachedThreadPool() {
		if (sCachedThreadPool == null || sCachedThreadPool.isShutdown()) {
			sCachedThreadPool = new NdThreadPoolExecutor(0, Integer.MAX_VALUE,
					60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
					NdThreadFactory.newCachedThreadFactory());
			sCachedThreadPool.setThreadTimeout(CACHE_THREAD_POOL_TIMEOUT);
		}
		return sCachedThreadPool;
	}

	/**
	 * UI交互密切相关的线程，比如获取历史聊天记录，刷新微博
	 * 
	 * @return
	 */
	public synchronized static ExecutorService getTinyHttpThreadPool() {
		if (sTinyHttpThreadPool == null || sTinyHttpThreadPool.isShutdown()) {
			sTinyHttpThreadPool = new NdThreadPoolExecutor(2, 12, 0,
					TimeUnit.SECONDS, sNormalWorkQueue,
					NdThreadFactory.newTinyHttpThreadFactory());
			sTinyHttpThreadPool.setThreadTimeout(TINY_HTTP_THREAD_POOL_TIMEOUT);
		}
		return sTinyHttpThreadPool;
	}

	/**
	 * 
	 * @return : 文件上传下载操作线程池
	 */
	public synchronized static ExecutorService getFileUploadDownloadThreadPool() {

		if (sFileUploadDownloadThreadPool == null
				|| sFileUploadDownloadThreadPool.isShutdown()) {
			// TODO 是否需要对上传下载队列大小做限制？
			sFileUploadDownloadThreadPool = new NdThreadPoolExecutor(1, 1, 0,
					TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(128),
					NdThreadFactory.newUploadDownloadThreadFactory());
		}
		return sFileUploadDownloadThreadPool;
	}

	/**
	 * @return 后台数据同步线程池
	 */
	public synchronized static ExecutorService getBackgroundThreadPool() {
		if (sBackgroundThreadPool == null || sBackgroundThreadPool.isShutdown()) {
			// TODO 是否需要对上传下载队列大小做限制？
			sBackgroundThreadPool = new NdThreadPoolExecutor(1, 1, 0,
					TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(128),
					NdThreadFactory.newBackgroundThreadFactory());
		}
		return sBackgroundThreadPool;
	}

	/**
	 * 线程池用于创建线程的工厂类,每个类型的线程池内的线程优先级不一样
	 */
	static class NdThreadFactory implements ThreadFactory {
		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String namePrefix;
		private int priority;

		private NdThreadFactory(int priority, String poolName) {
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() : Thread.currentThread()
					.getThreadGroup();
			namePrefix = "pool-" + poolName + "-thread-";
			this.priority = priority;
		}

		public static NdThreadFactory newBackgroundThreadFactory() {
			return new NdThreadFactory(Process.THREAD_PRIORITY_LOWEST,
					"backround");
		}

		public static NdThreadFactory newUploadDownloadThreadFactory() {
			return new NdThreadFactory(Process.THREAD_PRIORITY_LESS_FAVORABLE,
					"file_upload_download");
		}

		public static NdThreadFactory newTinyHttpThreadFactory() {
			return new NdThreadFactory(Thread.NORM_PRIORITY,
					"tiny_http");
		}

		public static NdThreadFactory newCachedThreadFactory() {
			return new NdThreadFactory(Process.THREAD_PRIORITY_MORE_FAVORABLE,
					"cached");
		}

		public Thread newThread(Runnable r) {
			Thread t = new Thread(group, r, namePrefix
					+ threadNumber.getAndIncrement(), 0);
			if (t.isDaemon()) {
				t.setDaemon(false);
			}

			t.setPriority(priority);
			return t;
		}
	}

	/**
	 * 退出所有的工作线程，这个函数是阻塞的
	 * 
	 * @return tiemout参数暂时没用
	 */
	public static boolean awaitQuit(long timeout) {

		// 两种方式-轮询or阻塞。这里采用轮询的方式，减少线程数量以及线程之间的通信，
		// 减少代码复杂度
		// 停止接收新的请求，等待执行完退出
		shutdown();
		if (checkShutdown()) {
			return true;
		}

		int queryCount = 0;
		while (queryCount < (AWAIT_TERMINATION_SELF / 1000)) {
			try {
				Thread.sleep(1000); // 轮询的间隔是300ms
			} catch (InterruptedException e) {
				shutdownNow();
				Thread.currentThread().interrupt();
			}

			if (checkShutdown()) {
				return true;
			}
			queryCount++;
		}

		// 超时，立即关闭
		shutdownNow();
		checkShutdown();
		queryCount = 0;
		while (queryCount < AWAIT_TERMINATION_FORCE / 1000) {
			try {
				Thread.sleep(1000); // 轮询的间隔是300ms
			} catch (InterruptedException e) {
				shutdownNow();
				Thread.currentThread().interrupt();
			}

			if (checkShutdown()) {
				return true;
			}
		}
		

		return checkAndLog();
	}

	/**
	 * 参见ThreadPoolExecutor.shutdown()
	 * 
	 */
	private static void shutdown() {
		if (!sTinyHttpThreadPool.isShutdown()) {
			sTinyHttpThreadPool.shutdown();
		}

		if (!sCachedThreadPool.isShutdown()) {
			sCachedThreadPool.shutdown();
		}

		if (!sFileUploadDownloadThreadPool.isShutdown()) {
			sFileUploadDownloadThreadPool.shutdown();
		}

		if (!sBackgroundThreadPool.isShutdown()) {
			sBackgroundThreadPool.shutdown();
		}
	}

	/**
	 * 参见ThreadPoolExecutor.shutdownNow()
	 * 
	 */
	private static void shutdownNow() {
		sTinyHttpThreadPool.shutdownNow();
		sCachedThreadPool.shutdownNow();
		sFileUploadDownloadThreadPool.shutdownNow();
		sBackgroundThreadPool.shutdownNow();
	}

	private static boolean checkShutdown() {
		return sTinyHttpThreadPool.isTerminated()
				&& sCachedThreadPool.isTerminated()
				&& sFileUploadDownloadThreadPool.isTerminated()
				&& sBackgroundThreadPool.isTerminated();
	}
	
	private static boolean checkAndLog() {
		return sTinyHttpThreadPool.checkTerminatedAndLog()
				&& sCachedThreadPool.checkTerminatedAndLog()
				&& sFileUploadDownloadThreadPool.checkTerminatedAndLog()
				&& sBackgroundThreadPool.checkTerminatedAndLog();
		
	}

	/**
	 * 退出线程
	 * 
	 * @return
	 */
	private static boolean quitInterval(ThreadPoolExecutor pool) {
		pool.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!pool.awaitTermination(3, TimeUnit.SECONDS)) {
				pool.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(20, TimeUnit.SECONDS)) {
					System.err.println("Pool did not terminate");
					return false;
				}

			}

		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
		return true;
	}

}
