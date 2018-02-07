package com.wonhigh.opus.opuslib;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.wonhigh.opus.opuslib.model.AudioPlayList;

import static com.wonhigh.opus.opuslib.utils.Config.ACTION_OPUS_UI_RECEIVER;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Dec-26
 */
public class OpusEvent {
    //below are values of EventType
    public static final int PLAYING_FINISHED = 1001;        //播放完毕
    public static final int PLAYING_STARTED = 1002;         //播放开始
    public static final int PLAYING_FAILED = 1003;          //播放失败
    public static final int PLAYING_PAUSED = 1004;          //播放暂停
    public static final int PLAY_PROGRESS_UPDATE = 1011;    //播放进度
    public static final int PLAY_GET_AUDIO_TRACK_INFO = 1012;

    public static final int RECORD_FINISHED = 2001;         //录音结束
    public static final int RECORD_STARTED = 2002;          //录音开始
    public static final int RECORD_FAILED = 2003;           //录音失败
    public static final int RECORD_PROGRESS_UPDATE = 2004;  //录音时间

    public static final int CONVERT_FINISHED = 3001;
    public static final int CONVERT_STARTED = 3002;
    public static final int CONVERT_FAILED = 3003;

    //文件上传相关
    public static final int UPLOAD_START = 4001;
    public static final int UPLOAD_FAILED = 4002;
    public static final int UPLOAD_FINISHED = 4003;

    //文件删除相关
    public static final int DELETE_FILE_START = 5001;
    public static final int DELETE_FILE_FAILED = 5002;
    public static final int DELETE_FILE_FINISHED = 5003;

    //below are types of EventType
    public static final String EVENT_TYPE = "EVENT_TYPE";
    public static final String EVENT_PLAY_PROGRESS_POSITION = "PLAY_PROGRESS_POSITION";
    public static final String EVENT_PLAY_DURATION = "PLAY_DURATION";
    public static final String EVENT_PLAY_TRACK_INFO = "PLAY_TRACK_INFO";
    public static final String EVENT_RECORD_PROGRESS = "RECORD_PROGRESS";
    public static final String EVENT_MSG = "EVENT_MSG";

    /**
     * Handler for sending status updates
     */
    private Context context = null;
    private String action = ACTION_OPUS_UI_RECEIVER;

    public OpusEvent(Context context) {
        this.context = context;
    }

    public void setActionReceiver(String action) {
        this.action = action;
    }

    public void sendEvent(Bundle bundle) {
        if (action == null) return;
        if (context == null) return;
        if (bundle == null) return;

        Intent i = new Intent().setAction(action).putExtras(bundle);
        context.sendBroadcast(i);
    }

    /**
     * Send Event to UI
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
     * Send Event to UI
     *
     * @param eventType int
     * @param msg       string
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

    /**
     * Send playback progress to UI
     *
     * @param currentPosition long
     * @param totalDuration   long
     */
    public void sendProgressEvent(long currentPosition, long totalDuration) {
        Bundle b = new Bundle();
        b.putInt(EVENT_TYPE, PLAY_PROGRESS_UPDATE);
        b.putLong(EVENT_PLAY_PROGRESS_POSITION, currentPosition);
        b.putLong(EVENT_PLAY_DURATION, totalDuration);
        Intent i = new Intent();
        i.setAction(action);
        i.putExtras(b);
        context.sendBroadcast(i);
    }

    /**
     * Send record progress(time, units: second) to UI
     * 录音时间
     */
    public void sendRecordProgressEvent(String time) {
        Bundle b = new Bundle();
        b.putInt(EVENT_TYPE, RECORD_PROGRESS_UPDATE);
        b.putString(EVENT_RECORD_PROGRESS, time);
        Intent i = new Intent();
        i.setAction(action);
        i.putExtras(b);
        context.sendBroadcast(i);
    }

    /**
     * Send infoList to UI
     *
     * @param infoList AudioPlayList
     */
    public void sendTrackInfoEvent(AudioPlayList infoList) {
        Bundle b = new Bundle();
        b.putInt(EVENT_TYPE, PLAY_GET_AUDIO_TRACK_INFO);
        // TODO; bug when sending the serializable infoList
        b.putSerializable(EVENT_PLAY_TRACK_INFO, infoList);
        Intent i = new Intent();
        i.setAction(action);
        i.putExtras(b);
        context.sendBroadcast(i);
    }
}
