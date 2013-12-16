package com.larry.threadpooldemo;

import android.app.Application;

public class MyApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		NdExecutors.init();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		NdExecutors.awaitQuit(0);
	}
}
