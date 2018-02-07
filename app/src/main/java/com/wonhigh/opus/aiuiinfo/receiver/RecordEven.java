package com.wonhigh.opus.aiuiinfo.receiver;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import static com.wonhigh.opus.opuslib.utils.Config.ACTION_OPUS_UI_RECEIVER;

/**
 * 描述： TODO
 * 作者： gong.xl
 * 邮箱： gong.xl@belle.com.cn
 * 创建时间： 2018/2/3 10:37
 * 修改时间： 2018/2/3 10:37
 * 修改备注：
 */

public class RecordEven {
    public static final int RECORD_START = 1001;        //录音开始
    public static final int RECORD_FINISHED = 1002;     //录音完成
    public static final int RECORD_FAILED = 1003;       //录音失败


    public static final int ENCODE_WAV_START = 2001;            //WAV编码开始
    public static final int ENCODE_WAV_FINISHED = 2002;         //WAV编码结束
    public static final int ENCODE_WAV_FAILED = 2003;           //WAV编码失败

    public static final int ENCODE_OPUS_START = 2004;            //opus编码开始
    public static final int ENCODE_OPUS_FINISHED = 2005;         //opus编码结束
    public static final int ENCODE_OPUS_FAILED = 2006;           //opus编码失败

    public static final String EVENT_TYPE = "EVENT_TYPE";
    public static final String EVENT_MSG = "EVENT_MSG";

    private Context context = null;
    private String action = ACTION_OPUS_UI_RECEIVER;


    public RecordEven(Context context) {
        this.context = context;
    }

    /**
     * 发送消息
     *
     * @param eventType int
     */
    public void sendEvent(int eventType) {
        Bundle b = new Bundle();
        b.putInt(EVENT_TYPE, eventType);
        Intent i = new Intent();
        i.setAction(action);
        i.putExtras(b);
        context.sendBroadcast(i);
    }


    /**
     * 发送消息
     *
     * @param eventType int 消息类型
     * @param msg       消息内容
     */
    public void sendEvent(int eventType, String msg) {
        Bundle b = new Bundle();
        b.putInt(EVENT_TYPE, eventType);
        b.putString(EVENT_MSG, msg);
        Intent i = new Intent();
        i.setAction(action);
        i.putExtras(b);
        context.sendBroadcast(i);
    }
}
