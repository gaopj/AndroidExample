/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.jobscheduler.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;


import static com.example.android.jobscheduler.MainActivity.MESSENGER_INTENT_KEY;
import static com.example.android.jobscheduler.MainActivity.MSG_COLOR_START;
import static com.example.android.jobscheduler.MainActivity.MSG_COLOR_STOP;
import static com.example.android.jobscheduler.MainActivity.WORK_DURATION_KEY;


/**
 * Service to handle callbacks from the JobScheduler. Requests scheduled with the JobScheduler
 * ultimately land on this service's "onStartJob" method. It runs jobs for a specific amount of time
 * and finishes them. It keeps the activity updated with changes via a Messenger.
 *
 * 服务来处理JobScheduler的回调。 使用JobScheduler安排的请求
 * 最终登陆此服务的“onStartJob”方法。 它运行特定时间的工作并完成它们。
 *  它通过Messenger更新活动更新。
 */

// JobService继承于Service，他是JobScheduler回调的入口点，它的回调方法运行在主线程上。
// onStartJob(JobParameters)与onStopJob(android.app.job.JobParameters)，这2个方法一定要实现。
public class MyJobService extends JobService {

    private static final String TAG = MyJobService.class.getSimpleName();

    private Messenger mActivityMessenger;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service destroyed");
    }

    /**
     * When the app's MainActivity is created, it starts this service. This is so that the
     * activity and this service can communicate back and forth. See "setUiCallback()"
     */


    // 通过onStartCommand将Activity的Messenger传入
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mActivityMessenger = intent.getParcelableExtra(MESSENGER_INTENT_KEY);
        return START_NOT_STICKY;
    }

    // 当作业开始之后，会先执行onStartJob方法。onStartJob的返回值有所区别：
    // (1) false：框架认为你作业已经执行完毕了，那么下一个作业就立刻展开了
    // (2) true：框架将作业结束状态交给你去处理。因为我们可能会异步的通过线程等方式去执行工作，
    //      这个时间肯定不能放在主线程里面去控制，
    //      这时候需要手动调用jobFinished(JobParameters params, boolean needsReschedule)方法去告诉框架作业结束了，
    //      其中needsReschedule表示是否重复执行

    @Override
    public boolean onStartJob(final JobParameters params) {
        // The work that this service "does" is simply wait for a certain duration and finish
        // the job (on another thread).

        // 该作业仅仅是 在另一个线程中空等一段时间，然后进行结束。
        sendMessage(MSG_COLOR_START, params.getJobId());

        // 获取额外参数
        long duration = params.getExtras().getLong(WORK_DURATION_KEY);

        // Uses a handler to delay the execution of jobFinished().
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendMessage(MSG_COLOR_STOP, params.getJobId());
                jobFinished(params, false);
            }
        }, duration);
        Log.i(TAG, "on start job: " + params.getJobId());

        // Return true as there's more work to be done with this job.
        return true;
    }

   // 当你使用cancel()或者cancelAll()的话会执行onStopJob方法。
   // 有个地方要注意，如果你onStartJob返回的是false的话，系统会因为认为工作已经结束而不再产生onStopJob回调

    @Override
    public boolean onStopJob(JobParameters params) {
        // Stop tracking these job parameters, as we've 'finished' executing.
        sendMessage(MSG_COLOR_STOP, params.getJobId());
        Log.i(TAG, "on stop job: " + params.getJobId());

        // Return false to drop the job.
        return false;
    }

    private void sendMessage(int messageID, @Nullable Object params) {
        // If this service is launched by the JobScheduler, there's no callback Messenger. It
        // only exists when the MainActivity calls startService() with the callback in the Intent.
        // 如果服务是被JobScheduler启动的，不会有Messenger回调 ，只有被MainActivity.startService() 启动才会
        if (mActivityMessenger == null) {
            Log.d(TAG, "Service is bound, not started. There's no callback to send a message to.");
            return;
        }
        Message m = Message.obtain();
        m.what = messageID;
        m.obj = params;
        try {
            mActivityMessenger.send(m);
        } catch (RemoteException e) {
            Log.e(TAG, "Error passing service object back to activity.");
        }
    }
}
