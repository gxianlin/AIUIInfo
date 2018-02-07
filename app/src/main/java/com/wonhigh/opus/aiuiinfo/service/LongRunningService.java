package com.wonhigh.opus.aiuiinfo.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class LongRunningService extends Service {

    private Timer timer;
    private TimerTask task;


    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("tag", "服务启动");

        startAutoBanner();

//        //创建Intent对象，action为ELITOR_CLOCK
//        Intent test = new Intent("ELITOR_CLOCK");
//
//        //定义一个PendingIntent对象，PendingIntent.getBroadcast包含了sendBroadcast的动作。
//        //也就是发送了action 为"ELITOR_CLOCK"的intent
//        pendingIntent = PendingIntent.getBroadcast(this, 0, test, 0);
//
//        //AlarmManager对象,注意这里并不是new一个对象，Alarmmanager为系统级服务
//        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
//
//        //设置闹钟从当前时间开始，每隔5s执行一次PendingIntent对象pi，注意第一个参数与第二个参数的关系
//        // 10分钟后通过PendingIntent pi对象发送广播
////        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 5 * 60 * 1000, pendingIntent);
//        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
//                60 * 1000, pendingIntent);
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        Log.e("tag", "服务停止，取消定时任务");
        //取消定时任务
//        alarmManager.cancel(pendingIntent);
        stopAutoBanner();
        super.onDestroy();
    }

    /**
     * 开始计时，并发出定时任务
     */
    public void startAutoBanner() {
        if (timer == null) {
            timer = new Timer();
        }
        if (task == null) {
            task = new TimerTask() {
                @Override
                public void run() {
                    Intent intent = new Intent("ELITOR_CLOCK");
                    sendBroadcast(intent);
                }
            };

            timer.schedule(task, 0, 10 * 60 * 1000);
        }
    }

    /**
     * 结束计时
     */
    public void stopAutoBanner() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

}