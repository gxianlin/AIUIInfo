package com.wonhigh.opus.aiuiinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.iflytek.aiui.servicekit.AIUIAgent;
import com.iflytek.aiui.servicekit.AIUIConstant;
import com.iflytek.aiui.servicekit.AIUIEvent;
import com.iflytek.aiui.servicekit.AIUIListener;
import com.wonhigh.opus.aiuiinfo.receiver.RecordEven;
import com.wonhigh.opus.aiuiinfo.service.LongRunningService;
import com.wonhigh.opus.aiuiinfo.utils.RecordUtils;
import com.wonhigh.opus.aiuiinfo.utils.SPUtils;
import com.wonhigh.opus.opuslib.OpusEvent;
import com.wonhigh.opus.opuslib.OpusService;
import com.wonhigh.opus.opuslib.OpusTrackInfo;
import com.wonhigh.opus.opuslib.upload.FTPManager;
import com.wonhigh.opus.opuslib.upload.IResultListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.wonhigh.opus.aiuiinfo.receiver.RecordEven.ENCODE_WAV_FAILED;
import static com.wonhigh.opus.aiuiinfo.receiver.RecordEven.ENCODE_WAV_FINISHED;
import static com.wonhigh.opus.aiuiinfo.receiver.RecordEven.ENCODE_WAV_START;
import static com.wonhigh.opus.aiuiinfo.receiver.RecordEven.EVENT_MSG;
import static com.wonhigh.opus.aiuiinfo.receiver.RecordEven.EVENT_TYPE;
import static com.wonhigh.opus.aiuiinfo.receiver.RecordEven.RECORD_FAILED;
import static com.wonhigh.opus.aiuiinfo.receiver.RecordEven.RECORD_FINISHED;
import static com.wonhigh.opus.aiuiinfo.receiver.RecordEven.RECORD_START;
import static com.wonhigh.opus.opuslib.OpusEvent.CONVERT_FINISHED;
import static com.wonhigh.opus.opuslib.OpusEvent.CONVERT_STARTED;
import static com.wonhigh.opus.opuslib.OpusEvent.DELETE_FILE_FAILED;
import static com.wonhigh.opus.opuslib.OpusEvent.DELETE_FILE_FINISHED;
import static com.wonhigh.opus.opuslib.OpusEvent.UPLOAD_FAILED;
import static com.wonhigh.opus.opuslib.OpusEvent.UPLOAD_FINISHED;
import static com.wonhigh.opus.opuslib.OpusEvent.UPLOAD_START;
import static com.wonhigh.opus.opuslib.utils.Config.ACTION_OPUS_UI_RECEIVER;

public class MainActivity extends AppCompatActivity {
    private final String SP_FILE_KEY = "failed_file";
    private Toast mToast;

    // AIUI服务代理
    private AIUIAgent mAIUIAgent;
    private String pcmFilePath;
    private String wavFilePath;

    private String failedFilePath;                  //上传失败的文件路径
    private List<String> failedPaths = new ArrayList<>();       //上传失败的文件路径集


    private boolean isStart = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mToast = Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT);

        if (null == mAIUIAgent) {
            // 创建AIUIAgent对象，绑定到AIUIServcie，绑定成功之后服务即为开启状态
            mAIUIAgent = AIUIAgent.createAgent(MainActivity.this, mAIUIListener);
        } else {
            showTip("不要重复绑定");
        }
        initBroadcast();

        initTimeBroadcast();

        initUpload();

        RecordUtils.getInstance().setEvent(new RecordEven(getApplicationContext()));

    }


    /**
     * 未上传文件上传处理
     */
    private void initUpload() {
        //读取未上传文件路径集
        String sp = (String) SPUtils.get(MainActivity.this, SP_FILE_KEY, "");
        String[] split = sp.split(",");
        //遍历添加到集合,并开始上传
        if (split.length != 0) {
            for (int i = 0; i < split.length; i++) {
                if (!TextUtils.isEmpty(split[i])) {
                    failedPaths.add(split[i]);
                }
            }
        }
        //如果有文件未上传，则开始上传
        if (failedPaths.size() > 0) {
            new Thread(new FilesUpload(), "Init File Upload Thread").start();
        }
    }

    /**
     * Toast提示信息
     *
     * @param tip 提示内容
     */
    private void showTip(final String tip) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (!TextUtils.isEmpty(tip)) {
                    mToast.setText(tip);
                    mToast.show();
                }
            }
        });
    }

    /**
     * 注册广播监听
     */
    private void initBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_OPUS_UI_RECEIVER);
        registerReceiver(receiver, filter);
    }

    private void initTimeBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("ELITOR_CLOCK");
        registerReceiver(timeReceicer, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        unregisterReceiver(timeReceicer);
        super.onDestroy();
    }

    // AIUI结果监听器
    private AIUIListener mAIUIListener = new AIUIListener() {

        @Override
        public void onEvent(AIUIEvent event) {
            switch (event.eventType) {
                // 绑定成功事件
                case AIUIConstant.EVENT_BIND_SUCCESS: {
                    // 绑定成功之后服务即为开启状态，无需发送CMD_START命令开启服务
                    showTip("Service绑定成功");
                    Log.e("tag", "Service绑定成功 ");

                    //绑定成功上传一个文件通知服务器
                    uploadTestFile();
                }
                break;

                // 唤醒事件
                case AIUIConstant.EVENT_WAKEUP: {
                    // 显示唤醒结果
                    showTip("已唤醒");
                    Log.e("tag", "已唤醒 ");

                    //定时任务服务没有启动时启动服务，反之停止服务
                    if (!isStart) {
                        Intent intent = new Intent(MainActivity.this, LongRunningService.class);
                        startService(intent);
                        isStart = true;
                    } else {
                        Intent intent = new Intent(MainActivity.this, LongRunningService.class);
                        stopService(intent);
                        isStart = false;
                        //发出停止录音操作
                        RecordUtils.getInstance().recordToggle(mAIUIAgent);
                    }

                    uploadTestFile();
                }

                break;

                // 交互超时触发的休眠事件
                case AIUIConstant.EVENT_SLEEP: {
                    showTip("交互超时触发的休眠事件");
                    Log.e("tag", "交互超时触发的休眠事件 ");
                }
                break;

                //即将进入休眠
                case AIUIConstant.EVENT_PRE_SLEEP: {
                    Log.e("tag", "即将进入休眠 ");
                }

                break;

                // 出错事件
                case AIUIConstant.EVENT_ERROR: {
                    int errorCode = event.arg1;
                    showTip("errorCode=" + errorCode);
                    Log.e("tag", "errorCode=" + errorCode);
                }
                break;

                //抛出降噪音频
                case AIUIConstant.EVENT_AUDIO:
                    byte[] audio = event.data.getByteArray("audio");

                    //保存录音
                    RecordUtils.getInstance().saveRecord(audio);
                    break;

                default:
                    break;
            }
        }
    };

    /**
     * 上传链接成功测试文件
     */
    private void uploadTestFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String path = OpusTrackInfo.getInstance().getTestFileName();
                File testFile = new File(path);

                try {
                    FileOutputStream outStream = new FileOutputStream(testFile);

                    SimpleDateFormat timesdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String content = "Device:" + android.os.Build.MODEL + "\n" + "Time:" + timesdf.format(new Date()) + "\nConnected:Success!\nIsStartRecord:" + isStart;

                    outStream.write(content.getBytes());
                    outStream.flush();
                    outStream.close();

                    FTPManager.getInstance().ftp4jUpload(path, new IResultListener() {
                        @Override
                        public void onSuccess(String info) {

                        }

                        @Override
                        public void onFilure(String message) {

                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    /**
     * 定时任务广播
     */
    private BroadcastReceiver timeReceicer = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("MyReceiver", "定时任务来了......................");
            //第一次接收到消息为开始录音，后续为停止录音指令
            RecordUtils.getInstance().recordToggle(mAIUIAgent);
        }
    };


    /**
     * 录音相关监听广播
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            int type = bundle.getInt(EVENT_TYPE, 0);
            switch (type) {
                case RECORD_START:
                    Log.e("tag", "录音开始");

                    break;

                case RECORD_FINISHED:
                    Log.e("tag", "录音完成");
                    pcmFilePath = bundle.getString(EVENT_MSG);

                    break;

                case RECORD_FAILED:
                    Log.e("tag", "录音失败");
                    break;

                case ENCODE_WAV_START:
                    Log.e("tag", "wav编码开始");
                    break;

                case ENCODE_WAV_FINISHED:
                    Log.e("tag", "wav编码完成");
                    //获取文件路径
                    wavFilePath = bundle.getString(EVENT_MSG);

                    //如果计时任务在进行，则继续发起录音
                    if (isStart) {
                        RecordUtils.getInstance().recordToggle(mAIUIAgent);
                    }


                    //删除pcm文件
                    OpusService.deleteFile(getApplicationContext(), pcmFilePath);
                    //opus编码
                    OpusService.encode(getApplicationContext(), wavFilePath, RecordUtils.getInstance().getOpusFilePath(), "");
                    break;

                case ENCODE_WAV_FAILED:
                    Log.e("tag", "wav编码失败");
                    break;

                case CONVERT_STARTED:
                    Log.e("tag", "opus编码开始");
                    break;


                case CONVERT_FINISHED:
                    Log.e("tag", "opus编码结束");

                    //获取文件路径
                    String opusFilePath = bundle.getString(EVENT_MSG);

                    //删除wav文件
                    OpusService.deleteFile(getApplicationContext(), wavFilePath);

                    //执行文件上传操作
                    OpusService.opusUpload(getApplicationContext(), opusFilePath);
                    break;


                case UPLOAD_START:
                    Log.e("tag", "文件上传开始");
                    break;
                case UPLOAD_FAILED:
                    //获取上传失败的文件路径
                    String filePath = bundle.getString(EVENT_MSG);
                    Log.e("tag", "文件上传失败，path = " + filePath);
                    //上传失败，获取失败的文件路径
                    failedFilePath = bundle.getString(EVENT_MSG);
                    //再次尝试上传
                    new Thread(new UploadRunable(), "Again File Upload Thread").start();
                    break;
                case UPLOAD_FINISHED:
                    Log.e("tag", "文件上传成功");
                    break;

                case DELETE_FILE_FAILED:
                    Log.e("tag", "文件删除失败，path = " + bundle.getString(EVENT_MSG));
                    break;

                case DELETE_FILE_FINISHED:
                    Log.e("tag", "文件删除完毕，path = " + bundle.getString(EVENT_MSG));
                    break;
            }
        }
    };


    //再次尝试文件上传
    private class UploadRunable implements Runnable {

        @Override
        public void run() {
            FTPManager.getInstance().ftp4jUpload(failedFilePath, new IResultListener() {
                @Override
                public void onSuccess(String info) {

                }

                @Override
                public void onFilure(String message) {
                    //再次失败时保存，下次进入应用后统一上传
                    failedPaths.add(failedFilePath);

                    StringBuffer buffer = new StringBuffer();
                    for (String s : failedPaths) {
                        buffer.append(",").append(s);
                    }

                    SPUtils.put(MainActivity.this, SP_FILE_KEY, buffer.toString());
                }
            });


        }
    }

    //进入应用开始上传未上传的文件
    private class FilesUpload implements Runnable {
        @Override
        public void run() {
            for (int i = (failedPaths.size() - 1); i >= 0; i--) {
                final int index = i;
                Log.e("tag", "index = " + index);
                FTPManager.getInstance().ftp4jUpload(failedPaths.get(i), new IResultListener() {
                    @Override
                    public void onSuccess(String info) {
                        failedPaths.remove(index);
                    }

                    @Override
                    public void onFilure(String message) {

                    }
                });
            }
            //如果还有文件没上传成功，记录
            if (failedPaths.size() > 0) {
                StringBuffer buffer = new StringBuffer();
                for (String s : failedPaths) {
                    buffer.append(",").append(s);
                }
                SPUtils.put(MainActivity.this, SP_FILE_KEY, buffer.toString());
            } else {
                SPUtils.put(MainActivity.this, SP_FILE_KEY, "");
            }

        }
    }

}
