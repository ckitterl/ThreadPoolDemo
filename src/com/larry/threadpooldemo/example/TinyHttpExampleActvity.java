package com.larry.threadpooldemo.example;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.larry.threadpooldemo.NdExecutors;
import com.larry.threadpooldemo.NdThreadPoolExecutor;
import com.larry.threadpooldemo.NdTinyHttpAsyncTask;
import com.larry.threadpooldemo.R;
import com.larry.threadpooldemo.TaskClassId;

public class TinyHttpExampleActvity extends Activity implements OnClickListener {
	private static final String TAG = "TinyHttpExampleActvity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tiny_http);
		findViewById(R.id.add).setOnClickListener(this);
		findViewById(R.id.add_id).setOnClickListener(this);
		findViewById(R.id.stop_id).setOnClickListener(this);
		findViewById(R.id.stop_all).setOnClickListener(this);
	}

	public class DoSomethingTask extends NdTinyHttpAsyncTask<Long, Void, Void> {

		@Override
		protected Void doInBackground(Long... params) {
			try {
				while (true) {
					Log.d(TAG, "do work....");
					Thread.sleep(params[0]);
				}
			} catch (InterruptedException e) {
				Log.d(TAG, getTaskName() + " stop by user");
			}
			return null;
		}
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (v.getId()) {
		case R.id.add:
			getTask(false).execute(1000L);
			break;
		case R.id.add_id:
			getTask(true).execute(1000L);
			break;
		case R.id.stop_id:
			NdTinyHttpAsyncTask.stopTaskById(TaskClassId.TEST_ID);
			break;
		case R.id.stop_all:
			NdExecutors.awaitQuit(0);
			break;
		}
	}

	private DoSomethingTask getTask(boolean withId) {
		DoSomethingTask asyncTask = new DoSomethingTask();
		if (withId) {
			asyncTask.setId(TaskClassId.TEST_ID);
		}
		return asyncTask;
	}

}
