package com.wonhigh.opus.aiuiinfo.utils;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.iflytek.aiui.servicekit.AIUIAgent;
import com.iflytek.aiui.servicekit.AIUIConstant;
import com.iflytek.aiui.servicekit.AIUIMessage;
import com.wonhigh.opus.aiuiinfo.Pcm2WavUtil;
import com.wonhigh.opus.aiuiinfo.receiver.RecordEven;
import com.wonhigh.opus.opuslib.OpusEvent;
import com.wonhigh.opus.opuslib.OpusRecorder;
import com.wonhigh.opus.opuslib.OpusTool;
import com.wonhigh.opus.opuslib.OpusTrackInfo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 描述： TODO
 * 作者： gong.xl
 * 邮箱： gong.xl@belle.com.cn
 * 创建时间： 2018/2/2 18:41
 * 修改时间： 2018/2/2 18:41
 * 修改备注：
 */

public class RecordUtils {

    private final int byteSize = 1536;
    private RecordEven recordEven;
    private static volatile RecordUtils instance;

    private FileOutputStream outputStream;
    private BufferedOutputStream bufferedOutputStream;

    private String pcmFilePath;            //当前录音文件保存的绝对路径
    private String wavFilePath;            //当前录音文件保存的绝对路径
    private String opusFilePath;            //当前录音文件保存的绝对路径

    //录音状态
    private static final int STATE_NONE = 0;        //空闲状态
    private static final int STATE_STARTED = 1;     //录音状态
    private static final int STATE_ENCODE = 2;      //wav编码状态

    private volatile int state = STATE_NONE;


    private final String filePath;


    private RecordUtils() {
        String sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        filePath = sdcardPath + "/BelleRecord/";
    }

    /**
     * 获取单例
     *
     * @return RecordUtils
     */
    public static RecordUtils getInstance() {
        if (instance == null) {
            synchronized (OpusRecorder.class) {
                if (instance == null) {
                    instance = new RecordUtils();
                }
            }
        }
        return instance;
    }


    /**
     * 录音开关
     *
     * @param aiuiAgent
     */
    public void recordToggle(AIUIAgent aiuiAgent) {

        switch (state) {
            case STATE_NONE:
                startRecord(aiuiAgent);
                break;
            case STATE_STARTED:
                stopRecord(aiuiAgent);
                break;
            case STATE_ENCODE:

                break;
            default:
                break;
        }
    }

    //判断是否正在录音
    public boolean isWorking() {
        return state != STATE_NONE;
    }

    /**
     * @param even
     */
    public void setEvent(RecordEven even) {
        recordEven = even;
    }

    /**
     * 开始录音
     */
    private void startRecord(AIUIAgent aiuiAgent) {
        if (null == aiuiAgent) {
            return;
        }

        // 发送start消息，开始抛出降噪音频
        AIUIMessage msg = new AIUIMessage(AIUIConstant.CMD_START_THROW_AUDIO,
                0, 0, null, null);
        aiuiAgent.sendMessage(msg);

        Log.e("tag", "发出消息，开始抛出音频");

        //创建当前保存的文件名
        this.pcmFilePath = OpusTrackInfo.getInstance().getPcmFileName();
        this.wavFilePath = OpusTrackInfo.getInstance().getWavFileName();
        this.opusFilePath = OpusTrackInfo.getInstance().getOpusFileName();

        //创建文件对象，准备开始保存录音
        File file = new File(pcmFilePath);
        // 如果文件存在则删除
        if (file.exists()) {
            file.delete();
        }
        // 在文件系统中根据路径创建一个新的空文件
        try {
            file.createNewFile();
            // 获取FileOutputStream对象
            outputStream = new FileOutputStream(file);
            // 获取BufferedOutputStream对象
            bufferedOutputStream = new BufferedOutputStream(outputStream);
        } catch (IOException e) {
            e.printStackTrace();

            return;
        }
        //改变录音状态：录音
        state = STATE_STARTED;

        //发送消息
        if (recordEven != null) {
            recordEven.sendEvent(RecordEven.RECORD_START);
        }
    }

    /**
     * 保存数据,在录音状态会不停的执行
     *
     * @param bytes 音频数据
     */
    public void saveRecord(byte[] bytes) {
        if (bufferedOutputStream == null || state == STATE_NONE) {
            return;
        }

        try {
            // 往文件所在的缓冲输出流中写byte数据
            bufferedOutputStream.write(bytes);
            // 刷出缓冲输出流，该步很关键，要是不执行flush()方法，那么文件的内容是空的。
            bufferedOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 停止录音
     */
    private void stopRecord(AIUIAgent aiuiAgent) {
        if (null == aiuiAgent) {
            return;
        }
        // 发送stop消息，停止抛出音频
        AIUIMessage msg = new AIUIMessage(AIUIConstant.CMD_STOP_THROW_AUDIO,
                0, 0, null, null);
        aiuiAgent.sendMessage(msg);


        Log.e("tag", "发出消息，停止录音");
        // 关闭创建的流对象
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (bufferedOutputStream != null) {
            try {
                bufferedOutputStream.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }

        //改变工作状态：编码
        state = STATE_ENCODE;
        //发送消息:录音完成
        if (recordEven != null) {
            recordEven.sendEvent(RecordEven.RECORD_FINISHED, pcmFilePath);
        }
        Log.e("time", "录音完毕，时间 = " + System.currentTimeMillis());

        //文件保存完毕后编码处理
        pcm2wav();
    }


    /**
     * 对录音文件进行编码
     *
     * @return 编码后输出文件路径
     */
    private void pcm2wav() {

        //1.先将pcm转成wav
        try {
            //发送消息:录音失败
            if (recordEven != null) {
                recordEven.sendEvent(RecordEven.ENCODE_WAV_START);
            }
            Pcm2WavUtil.pcm2wav(pcmFilePath, wavFilePath, byteSize);
        } catch (IOException e) {
            //转wav出错
            e.printStackTrace();
            //发送消息:录音失败
            if (recordEven != null) {
                recordEven.sendEvent(RecordEven.ENCODE_WAV_FAILED);
            }

            state = STATE_NONE;
            Log.e("tag", "pcm 2 wav 出错,message = " + e.getMessage());
            return;
        }

        //编码完毕后改变状态：空闲
        state = STATE_NONE;

        //发送消息:wav编码完成
        if (recordEven != null) {
            recordEven.sendEvent(RecordEven.ENCODE_WAV_FINISHED, wavFilePath);
        }
        Log.e("time", "转wav完毕，时间 = " + System.currentTimeMillis());
    }

    public String getOpusFilePath() {
        if (TextUtils.isEmpty(opusFilePath)) {
            return "";
        }
        return opusFilePath;
    }


}
