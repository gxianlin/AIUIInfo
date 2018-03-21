package com.wonhigh.opus.opuslib;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.wonhigh.opus.opuslib.utils.LogHelper;


/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Dec-26
 */
public class
OpusService extends Service {
    private static final String TAG = LogHelper.makeLogTag(OpusService.class);

    //This server
    private static final String ACTION_OPUSSERVICE = "com.wonhigh.opus.action.OPUSSERVICE";

    private static final String EXTRA_FILE_NAME = "FILE_NAME";
    private static final String EXTRA_FILE_NAME_OUT = "FILE_NAME_OUT";
    private static final String EXTRA_OPUS_CODING_OPTION = "OPUS_CODING_OPTION";
    private static final String EXTRA_CMD = "CMD";
    private static final String IS_SAVE_PCM = "IS_SAVE_PCM";
    private static final String EXTRA_SEEKFILE_SCALE = "SEEKFILE_SCALE";
    private static final String EXTRA_FILE_UPLOAD_PATH = "FILE_UPLOAD_PATH";
    private static final String EXTRA_FILE_DELETE_PATH = "FILE_DELETE_PATH";


    private static final int CMD_PLAY = 10001;
    private static final int CMD_PAUSE = 10002;
    private static final int CMD_STOP_PLAYING = 10003;
    private static final int CMD_TOGGLE = 10004;
    private static final int CMD_SEEK_FILE = 10005;
    private static final int CMD_GET_TRACK_INFO = 10006;
    private static final int CMD_ENCODE = 20001;                //编码
    private static final int CMD_DECODE = 20002;                //解码
    private static final int CMD_RECORD = 30001;
    private static final int CMD_STOP_RECORDING = 30002;
    private static final int CMD_RECORD_TOGGLE = 30003;         //录音开关

    private static final int FILE_UPLOAD = 4001;                //上传文件
    private static final int FILE_DELETE = 4002;                //删除文件

    //Looper
    private volatile Looper serviceLooper;
    private volatile ServiceHandler serviceHandler;

    private OpusRecorder recorder;
    private OpusConverter converter;
    private OpusTrackInfo trackInfo;
    private OpusEvent event = null;


    @Override
    public void onCreate() {
        super.onCreate();
        event = new OpusEvent(getApplicationContext());


        recorder = OpusRecorder.getInstance();
        converter = OpusConverter.getInstance();
        trackInfo = OpusTrackInfo.getInstance();

        trackInfo.setEvenSender(event);
        recorder.setEventSender(event);
        converter.setEventSender(event);


        // start looper in onCreate() instead of onStartCommand()
        HandlerThread thread = new HandlerThread("OpusServiceHandler");
        thread.start();
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }

    @Override
    public void onDestroy() {
        // quit looper
        serviceLooper.quit();
        recorder.release();
        converter.release();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        serviceHandler.sendMessage(msg);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }


    public static void record(Context context, String fileName) {
        Intent intent = new Intent(context, OpusService.class);
        intent.setAction(ACTION_OPUSSERVICE);
        intent.putExtra(EXTRA_CMD, CMD_RECORD);
        intent.putExtra(EXTRA_FILE_NAME, fileName);
        context.startService(intent);
    }

    public static void toggle(Context context, String fileName) {
        Intent intent = new Intent(context, OpusService.class);
        intent.setAction(ACTION_OPUSSERVICE);
        intent.putExtra(EXTRA_CMD, CMD_TOGGLE);
        intent.putExtra(EXTRA_FILE_NAME, fileName);
        context.startService(intent);
    }

    public static void seekFile(Context context, float scale) {
        Intent intent = new Intent(context, OpusService.class);
        intent.setAction(ACTION_OPUSSERVICE);
        intent.putExtra(EXTRA_CMD, CMD_SEEK_FILE);
        intent.putExtra(EXTRA_SEEKFILE_SCALE, scale);
        context.startService(intent);
    }


    /**
     * 调用系统录音的开关
     *
     * @param context
     * @param isSavaPcm 是否需要保存pcm格式文件
     */
    public static void recordToggle(Context context, boolean isSavaPcm) {
        Intent intent = new Intent(context, OpusService.class);
        intent.setAction(ACTION_OPUSSERVICE);
        intent.putExtra(EXTRA_CMD, CMD_RECORD_TOGGLE);
        intent.putExtra(IS_SAVE_PCM, isSavaPcm);
        context.startService(intent);
    }

    /**
     * 编码
     *
     * @param context
     * @param fileName
     * @param fileNameOut
     * @param option
     */
    public static void encode(Context context, String fileName, String fileNameOut, String option) {
        Intent intent = new Intent(context, OpusService.class);
        intent.setAction(ACTION_OPUSSERVICE);
        intent.putExtra(EXTRA_CMD, CMD_ENCODE);
        intent.putExtra(EXTRA_FILE_NAME, fileName);
        intent.putExtra(EXTRA_FILE_NAME_OUT, fileNameOut);
        intent.putExtra(EXTRA_OPUS_CODING_OPTION, option);
        context.startService(intent);
    }

    /**
     * 解码
     *
     * @param context
     * @param fileName
     * @param fileNameOut
     * @param option
     */
    public static void decode(Context context, String fileName, String fileNameOut, String option) {
        Intent intent = new Intent(context, OpusService.class);
        intent.setAction(ACTION_OPUSSERVICE);
        intent.putExtra(EXTRA_CMD, CMD_DECODE);
        intent.putExtra(EXTRA_FILE_NAME, fileName);
        intent.putExtra(EXTRA_FILE_NAME_OUT, fileNameOut);
        intent.putExtra(EXTRA_OPUS_CODING_OPTION, option);
        context.startService(intent);
    }



    /**
     * 文件上传
     * @param context
     * @param filePath
     */
    public static void opusUpload(Context context, String filePath) {
        Intent intent = new Intent(context, OpusService.class);
        intent.setAction(ACTION_OPUSSERVICE);
        intent.putExtra(EXTRA_CMD, FILE_UPLOAD);
        intent.putExtra(EXTRA_FILE_UPLOAD_PATH, filePath);
        context.startService(intent);
    }

    /**
     * 删除文件
     *
     * @param context
     * @param filePath 文件路径
     */
    public static void deleteFile(Context context, String filePath) {
        Intent intent = new Intent(context, OpusService.class);
        intent.setAction(ACTION_OPUSSERVICE);
        intent.putExtra(EXTRA_CMD, FILE_DELETE);
        intent.putExtra(EXTRA_FILE_DELETE_PATH, filePath);
        context.startService(intent);
    }

    private void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            if (ACTION_OPUSSERVICE.equals(action)) {
                int request = intent.getIntExtra(EXTRA_CMD, 0);
                String fileName;
                String fileNameOut;
                String option;
                switch (request) {
                    case CMD_STOP_RECORDING:
                        handleActionStopRecording();
                        break;
                    case CMD_ENCODE:
                        fileName = intent.getStringExtra(EXTRA_FILE_NAME);
                        fileNameOut = intent.getStringExtra(EXTRA_FILE_NAME_OUT);
                        option = intent.getStringExtra(EXTRA_OPUS_CODING_OPTION);
                        handleActionEncode(fileName, fileNameOut, option);
                        break;
                    case CMD_DECODE:
                        fileName = intent.getStringExtra(EXTRA_FILE_NAME);
                        fileNameOut = intent.getStringExtra(EXTRA_FILE_NAME_OUT);
                        option = intent.getStringExtra(EXTRA_OPUS_CODING_OPTION);
                        handleActionDecode(fileName, fileNameOut, option);
                        break;
                    case CMD_RECORD_TOGGLE:
                        //判断是否在录音
                        if (recorder.isWorking()) {
                            handleActionStopRecording();
                        } else {
                            boolean isSavePcm = intent.getBooleanExtra(IS_SAVE_PCM, true);
                            handleActionRecord(isSavePcm);
                        }
                        break;

                    case CMD_GET_TRACK_INFO:
                        trackInfo.sendTrackInfo();
                        break;

                    case FILE_UPLOAD:

                        handleActionUpload(intent.getStringExtra(EXTRA_FILE_UPLOAD_PATH));
                        break;
                    case FILE_DELETE:
                        handleActionDelete(intent.getStringExtra(EXTRA_FILE_DELETE_PATH));
                        break;


                    default:
                        LogHelper.e(TAG, "Unknown intent CMD,discarded!");
                }
            } else {
                LogHelper.e(TAG, "Unknown intent action,discarded!");
            }
        }
    }


    private void handleActionRecord(boolean isSavePcm) {
        recorder.startRecording(isSavePcm);
    }

    /**
     * 停止录音
     */
    private void handleActionStopRecording() {
        recorder.stopRecording();
    }

    private void handleActionEncode(String fileNameIn, String fileNameOut, String option) {
        converter.encode(fileNameIn, fileNameOut, option);
    }

    private void handleActionDecode(String fileNameIn, String fileNameOut, String option) {
        converter.decode(fileNameIn, fileNameOut, option);
    }

    private void handleActionUpload(String filePath) {
        converter.upload(filePath);
    }

    private void handleActionDelete(String filePath) {
        converter.delete(filePath);
    }



    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent) msg.obj);
        }
    }
}
