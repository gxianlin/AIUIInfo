package com.wonhigh.opus.aiuiinfo.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.wonhigh.opus.aiuiinfo.MainActivity;

/**
 * 描述： 程序自启动广播
 * 作者： gong.xl
 * 邮箱： gong.xl@belle.com.cn
 * 创建时间： 2018/2/2 18:36
 * 修改时间： 2018/2/2 18:36
 * 修改备注：
 */

public class AutoStartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.e("tag","接收到开机广播");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(context, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
        },10000);

    }

}
