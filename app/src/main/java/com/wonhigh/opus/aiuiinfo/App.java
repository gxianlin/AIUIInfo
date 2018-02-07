package com.wonhigh.opus.aiuiinfo;

import android.app.Application;

import com.wonhigh.opus.opuslib.OpusInit;

/**
 * 描述： TODO
 * 作者： gong.xl
 * 邮箱： gong.xl@belle.com.cn
 * 创建时间： 2018/2/3 10:13
 * 修改时间： 2018/2/3 10:13
 * 修改备注：
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        OpusInit.setFilePath("BelleRecord");
    }
}
