package com.larry.threadpooldemo;

import java.util.Timer;

import android.os.Process;

/**
 * 自定义的Runnable，提供两个方法进行对这个Runnable的标示，
 * 并自定义一个mTimer，用来对这个task进行超时监控
 * @author caimk
 *
 */
public abstract class NdAbstractTask implements Runnable, Comparable<NdAbstractTask> {
	// 用来处理任务超时的一个定时器
	private Timer mTimer;
	private int mPriority = DEFAULT_PRIORITY;
	private State mState = State.STAGED;
	
	private ETaskTypeId mId;
	private String mName;
	private Thread mCurrentThread;
	
	private static final int DEFAULT_PRIORITY = Process.THREAD_PRIORITY_DEFAULT;
	
	public enum State {
		NONE,
		STAGED,
		COMMITTED,
		FINISHED
	}
	
	@Override
	public abstract void run(); 
	
	public State getState() {
		return mState;
	}
	
	public void setState(State state) {
		mState = state;
	}

	/**
	 * 
	 * @return name 所要监控的线程名字,在log中会体现
	 */
	public String getName() {
		return mName;
	}
	
	/**
	 * 
	 * 如果想要对自己的线程进行监控，必须要设置名字
	 * 
	 * @param name
	 */
	public void setName(String name) {
		mName = name;
	}
	
	/**
	 * 如果返回0，将无法对这个Runner执行查询、停止操作
	 * @return
	 */
	public ETaskTypeId getId() {
		return mId;
	}
	
	/**
	 * 
	 * 如果需要查询这个Runner，必须设置id。
	 * 
	 * @param id
	 */
	public void setId(ETaskTypeId id) {
		mId = id;
	}
	
	public void setCurrentThread(Thread currentThread) {
		mCurrentThread = currentThread;
	}
	
	public Thread getCurrentThread() {
		return mCurrentThread;
	}
	
	public void setTimer(Timer timer) {
		this.mTimer = timer;
	}
	
	public Timer getTimer() {
		return mTimer;
	}
	
	public void setPriority(int priority) {
		mPriority = priority;
	}
	
	public int getPriority() {
		return mPriority; 
	}

	@Override
	public int compareTo(NdAbstractTask another) {
		return this.mPriority - another.getPriority();
	}

}
