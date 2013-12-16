package com.larry.threadpooldemo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.util.Log;

import com.larry.threadpooldemo.NdAbstractTask.State;

public class NdThreadPoolExecutor extends ThreadPoolExecutor {
	private boolean mIsDebug = true;
	private long mTimeout = 0;
	private static final String TAG = "NdThreadPoolExecutor";

	private ConcurrentLinkedQueue<NdAbstractTask> mQueueRunningTask = new ConcurrentLinkedQueue<NdAbstractTask>();

	public NdThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	public NdThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
				threadFactory);
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		if (r instanceof NdAbstractTask) {
			final NdAbstractTask nr = (NdAbstractTask) r;
			if (mIsDebug) {
				Log.d(TAG, "runnable: " + nr.getName()
						+ " have been finished: " + System.currentTimeMillis());
			}

			Timer timer = nr.getTimer();
			if (timer != null) {
				timer.cancel();
			}

			synchronized (NdThreadPoolExecutor.this) {
				mQueueRunningTask.remove(nr);
				nr.setState(State.FINISHED);
				nr.setCurrentThread(null);
			}
		}
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		if (r instanceof NdAbstractTask) {
			final NdAbstractTask nr = (NdAbstractTask) r;
			if (mIsDebug) {
				Log.d(TAG,
						"runnable: " + nr.getName() + " in thread: "
								+ t.getName() + " will start: "
								+ System.currentTimeMillis());
			}

			// 超时机制
			if (mTimeout != 0) {

				Timer timeOutTimer = new Timer();
				final Thread timeT = t;
				timeOutTimer.schedule(new TimerTask() {

					@Override
					public void run() {
						Log.d(TAG,
								"thread："
										+ timeT.getName()
										+ " timeout and should be interrupt, task id: "
										+ nr.getId());
						// timeT.interrupt();
					}
				}, mTimeout);
				nr.setTimer(timeOutTimer);
			}

			synchronized (NdThreadPoolExecutor.this) {
				mQueueRunningTask.add(nr);
				nr.setState(State.COMMITTED);
				nr.setCurrentThread(t);
			}
		}

		super.beforeExecute(t, r);
	}

	/**
	 * 设置线程超时时间，如果超过timeout, 线程仍然没有返回 将设置中断信号，把该线程中断
	 * 
	 * @param timeout
	 *            超时时间戳，单位millionSecond
	 */
	public void setThreadTimeout(long timeout) {
		mTimeout = timeout;
	}

	public ConcurrentLinkedQueue<NdAbstractTask> getRunningTask() {
		return mQueueRunningTask;
	}

	public boolean checkTerminatedAndLog() {
		if (super.isTerminated()) {
			return true;
		}

		for (NdAbstractTask task : mQueueRunningTask) {
			Log.d(TAG, "task: " + task.getName() + " have not quited already!");
		}
		return false;
	}

	/**
	 * 通过id查询任务的执行状态,如果没有注册id，将无法进行查询
	 * 
	 * @param id
	 *            指定要被查询的任务，id必须是预定义好的
	 * @return
	 */
	public ArrayList<NdAbstractTask> getTaskListState(int id) {
		ArrayList<NdAbstractTask> result = new ArrayList<NdAbstractTask>();
		if (id == 0) {
			return null;
		}
		// 先去待执行队列中查找
		BlockingQueue<Runnable> stagedQueue = getQueue();
		for (Runnable r : stagedQueue) {
			if (r instanceof NdAbstractTask
					&& ((NdAbstractTask) r).getId() == id) {
				result.add((NdAbstractTask) r);
			}
		}

		// 再去正在执行的队列中查找
		synchronized (this) {
			for (NdAbstractTask r : mQueueRunningTask) {
				if (r.getId() == id) {
					result.add(r);
				}
			}

		}
		return result;
	}

	/**
	 * 从队列中删除用id指定的任务，
	 * 
	 * @param id
	 *            指定要被删除的任务，id必须是预定义好的
	 */
	public void removeById(int id) {
		// 删除待等待执行队列
		BlockingQueue<Runnable> stagedQueue = getQueue();
		Iterator<Runnable> i = stagedQueue.iterator();
		while (i.hasNext()) {
			Runnable r = i.next();
			if (r instanceof NdAbstractTask
					&& ((NdAbstractTask) r).getId() == id) {
				Log.d(TAG, ((NdAbstractTask) r).getName() + " cancel by id: "
						+ id + ", state: STAGE");
				i.remove();
			}
		}

		// 删除正在执行中的任务
		synchronized (this) {
			for (NdAbstractTask task : mQueueRunningTask) {
				if (task.getId() == id && task.getCurrentThread() != null) {
					Log.d(TAG, task.getName() + " cancel by id: "
							+ id + ", state: " + task.getState());
					task.getCurrentThread().interrupt();
				}
			}
		}
	}

	/**
	 * 删除指定的任务,保留，功能还没有完成
	 * 
	 * @param r
	 */
	protected void removeByObject(NdAbstractTask r) {
		if (r == null) {
			return;
		}
		State state = r.getState();
		if (state == State.COMMITTED) {
			// remove the running thread

			return;
		} else {
			getQueue().remove(r);
		}
	}

}
