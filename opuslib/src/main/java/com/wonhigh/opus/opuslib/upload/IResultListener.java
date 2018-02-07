package com.wonhigh.opus.opuslib.upload;

/**
 * 描述： TODO
 * 作者： gong.xl
 * 邮箱： gong.xl@belle.com.cn
 * 创建时间： 2018/2/1 11:30
 * 修改时间： 2018/2/1 11:30
 * 修改备注：
 */

public interface IResultListener {
    void onSuccess(String info);

    void onFilure(String message);
}
