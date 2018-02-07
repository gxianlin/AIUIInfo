package com.wonhigh.opus.opuslib;

import android.text.TextUtils;

import com.wonhigh.opus.opuslib.model.AudioPlayList;
import com.wonhigh.opus.opuslib.model.AudioTime;
import com.wonhigh.opus.opuslib.utils.LogHelper;
import com.wonhigh.opus.opuslib.utils.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Dec-26
 */
public class OpusTrackInfo {
    private String TAG = LogHelper.makeLogTag(OpusTrackInfo.class);

    private static volatile OpusTrackInfo opusTrackInfo;

    private OpusEvent eventSender;
    private OpusTool opusTool = new OpusTool();

    private String appExtDir;
    private File requestDirFile;

    private Thread workThread = new Thread();
    private AudioPlayList audioPlayList = new AudioPlayList();
    private AudioTime audioTime = new AudioTime();


    public static final String TITLE_TITLE = "TITLE";
    public static final String TITLE_ABS_PATH = "ABS_PATH";
    public static final String TITLE_DURATION = "DURATION";
    public static final String TITLE_IMG = "TITLE_IMG";
    public static final String TITLE_IS_CHECKED = "TITLE_IS_CHECKED";

    private OpusTrackInfo() {
        // create OPlayer directory if it does not exist.

//        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
//            return;
//        String sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        appExtDir = OpusInit.appExtDir;
        if (TextUtils.isEmpty(appExtDir)) {
            throw new NullPointerException("File path cannot be empty, please set the file path first.");
        }
        File fp = new File(appExtDir);
        if (!fp.exists()) {
            fp.mkdir();
        }

//        getTrackInfo(appExtDir + "OpusFile/");
    }

    public static OpusTrackInfo getInstance() {
        if (opusTrackInfo == null)
            synchronized (OpusTrackInfo.class) {
                if (opusTrackInfo == null)
                    opusTrackInfo = new OpusTrackInfo();
            }
        return opusTrackInfo;
    }

    public void setEvenSender(OpusEvent opusEven) {
        eventSender = opusEven;
    }


    public AudioPlayList getAudioPlayList() {
        return audioPlayList;
    }

    //将刚编码后的opus文件添加到列表中
    public void addOpusFile(String file) {
        try {
            Thread.sleep(10);
        } catch (Exception e) {
            Utils.printE(TAG, e);
        }

        File f = new File(file);
        if ("opus".equalsIgnoreCase(Utils.getExtension(file))) {
            Map<String, Object> map = new HashMap<>();
            map.put(TITLE_TITLE, f.getName());
            map.put(TITLE_ABS_PATH, file);
            audioTime.setTimeInSecond(opusTool.getTotalDuration());
            map.put(TITLE_DURATION, audioTime.getTime());
            map.put(TITLE_IS_CHECKED, false);
            //TODO: get image from opus files
            map.put(TITLE_IMG, 0);
            audioPlayList.add(map);

            if (eventSender != null) {
                eventSender.sendTrackInfoEvent(audioPlayList);
            }
        }
    }

    /**
     * 获取录音文件保存根路径
     *
     * @return
     */
    public String getAppExtDir() {
        return appExtDir;
    }

    public void sendTrackInfo() {
        if (eventSender != null) {
            eventSender.sendTrackInfoEvent(audioPlayList);
        }
    }

    /**
     * 获取opus文件列表
     *
     * @return
     */
    public AudioPlayList getTrackInfo() {
        return audioPlayList;
    }

    private void getTrackInfo(String Dir) {
        if (Dir.length() == 0)
            Dir = appExtDir;
        File file = new File(Dir);
        if (file.exists() && file.isDirectory())
            requestDirFile = file;

        workThread = new Thread(new WorkingThread(), "Opus Trc Trd");
        workThread.start();
    }

    /**
     * 获取测试文件名
     * 规则：以时间格式命名 如:2018-01-01_08-00-00
     *
     * @return
     */
    public String getTestFileName() {
        String fileName = "ConnectTest.txt";
        String path = appExtDir + "TestFile/";
        File fp = new File(path);
        if (!fp.exists()) {
            fp.mkdir();
        }
        return path + fileName;
    }


    /**
     * 获取文件名
     * 规则：以时间格式命名 如:2018-01-01_08-00-00
     *
     * @return
     */
    public String getOpusFileName() {
        SimpleDateFormat timesdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String fileTime = android.os.Build.MODEL + "_" + timesdf.format(new Date());
        String path = appExtDir + "OpusFile/";
        File fp = new File(path);
        if (!fp.exists()) {
            fp.mkdir();
        }
        return path + fileTime.replace(" ", "_").replace(":", "-") + ".opus";
    }

    public String getPcmFileName() {
        SimpleDateFormat timesdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String fileTime = android.os.Build.MODEL + "_" + timesdf.format(new Date());
        String path = appExtDir + "PcmFile/";
        File fp = new File(path);
        if (!fp.exists()) {
            fp.mkdir();
        }
        return path + fileTime.replace(" ", "_").replace(":", "-") + ".pcm";
    }

    public String getWavFileName() {
        SimpleDateFormat timesdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String fileTime = android.os.Build.MODEL + "_" + timesdf.format(new Date());
        String path = appExtDir + "WavFile/";
        File fp = new File(path);
        if (!fp.exists()) {
            fp.mkdir();
        }
        return path + fileTime.replace(" ", "_").replace(":", "-") + ".wav";
    }

    private void prepareTrackInfo(File file) {
        try {
            File[] files = file.listFiles();
            for (File f : files) {
                if (f.isFile()) {
                    String name = f.getName();
                    String absPath = f.getAbsolutePath();

                    if ("opus".equalsIgnoreCase(Utils.getExtension(name))) {
                        Map<String, Object> map = new HashMap<>();
                        map.put(TITLE_TITLE, f.getName());
                        map.put(TITLE_ABS_PATH, absPath);
                        audioTime.setTimeInSecond(opusTool.getTotalDuration());
                        map.put(TITLE_DURATION, audioTime.getTime());
                        map.put(TITLE_IS_CHECKED, false);
                        // TODO: get image from opus files
                        map.put(TITLE_IMG, 0);
                        audioPlayList.add(map);
//                        opusTool.closeOpusFile();
                    }

                } else if (f.isDirectory()) {
                    prepareTrackInfo(f);
                }
            }
        } catch (Exception e) {
            Utils.printE(TAG, e);
        }
    }

    class WorkingThread implements Runnable {
        public void run() {
            prepareTrackInfo(requestDirFile);
            sendTrackInfo();
        }
    }

    public void release() {
        try {
            if (workThread.isAlive())
                workThread.interrupt();
        } catch (Exception e) {
            Utils.printE(TAG, e);
        }
    }
}
