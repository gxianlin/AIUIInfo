package com.wonhigh.opus.opuslib;

import android.os.Environment;
import android.text.TextUtils;

/**
 * 描述： TODO
 * 作者： gong.xl
 * 邮箱： gong.xl@belle.com.cn
 * 创建时间： 2018/1/25 20:43
 * 修改时间： 2018/1/25 20:43
 * 修改备注：
 */

public class OpusInit {

    public static String appExtDir;


    /**
     * 设置音频文件保存路径
     *
     * @param path 文件路径
     */
    public static void setFilePath(String path) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return;
        }
        if (TextUtils.isEmpty(path)) {
            throw new NullPointerException("File path cannot be empty.");
        }
        String sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        appExtDir = sdcardPath + "/" + path + "/";
    }
}
