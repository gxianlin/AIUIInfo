package com.wonhigh.opus.opuslib;


import com.wonhigh.opus.opuslib.upload.FTPManager;
import com.wonhigh.opus.opuslib.upload.IResultListener;
import com.wonhigh.opus.opuslib.utils.Utils;

import java.io.File;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Dec-26
 */
public class OpusConverter {
    private static String TAG = OpusConverter.class.getName();

    private static final int STATE_NONE = 0;
    private static final int STATE_CONVERTING = 1;
    private static final boolean TYPE_ENC = true;
    private static final boolean TYPE_DEC = false;

    private volatile int state = STATE_NONE;
    private boolean convertType;

    private String inputFile;
    private String outputFile;
    private String option;

    private String upLoadFilePath;

    private String deleteFilePath;


    private OpusTool opusTool = new OpusTool();
    private Thread workerThread = new Thread();
    private Thread uploadThread = new Thread();
    private Thread deleteThread = new Thread();
    private OpusEvent eventSender = null;

    private static volatile OpusConverter converter;


    private OpusConverter() {
    }

    public static OpusConverter getInstance() {
        if (converter == null) {
            synchronized (OpusConverter.class) {
                if (converter == null) {
                    converter = new OpusConverter();
                }
            }
        }
        return converter;
    }

    public void setEventSender(OpusEvent es) {
        eventSender = es;
    }


    class ConvertThread implements Runnable {
        public void run() {
            if (eventSender != null) {
                eventSender.sendEvent(OpusEvent.CONVERT_STARTED);
            }

            if (convertType == TYPE_ENC) {
                opusTool.encode(inputFile, outputFile, option);
            } else if (convertType == TYPE_DEC) {
                opusTool.decode(inputFile, outputFile, option);
            }
            state = STATE_NONE;

//            OpusTrackInfo.getInstance().addOpusFile(outputFile);
            if (eventSender != null) {
                eventSender.sendEvent(OpusEvent.CONVERT_FINISHED, outputFile);
            }
        }
    }

    /**
     * 对文件进行opus编码
     *
     * @param wavFileNameIn   输入路径：wav格式音频文件
     * @param opusFileNameOut 输出路径：opus格式文件
     * @param opt
     */
    public void encode(String wavFileNameIn, String opusFileNameOut, String opt) {
        if (!Utils.isWAVFile(wavFileNameIn)) {
            if (eventSender != null)
                eventSender.sendEvent(OpusEvent.CONVERT_FAILED);
            return;
        }
        state = STATE_CONVERTING;
        convertType = TYPE_ENC;
        inputFile = wavFileNameIn;
        outputFile = opusFileNameOut;
        option = opt;
        workerThread = new Thread(new ConvertThread(), "Opus Enc Thread");
        workerThread.start();
    }


    /**
     * @param opusFileNameIn
     * @param wavFileNameOut
     * @param opt
     */
    public void decode(String opusFileNameIn, String wavFileNameOut, String opt) {
        if (!Utils.isFileExist(opusFileNameIn) || opusTool.isOpusFile(opusFileNameIn) == 0) {
            if (eventSender != null) {
                eventSender.sendEvent(OpusEvent.CONVERT_FAILED);
            }
            return;
        }
        state = STATE_CONVERTING;
        convertType = TYPE_DEC;
        inputFile = opusFileNameIn;
        outputFile = wavFileNameOut;
        option = opt;
        workerThread = new Thread(new ConvertThread(), "Opus Dec Thread");
        workerThread.start();
    }


    /**
     * 文件上传
     *
     * @param filePath 文件路径
     */
    public void upload(String filePath) {
        if (!Utils.isFileExist(filePath) || opusTool.isOpusFile(filePath) == 0) {
            if (eventSender != null) {
                eventSender.sendEvent(OpusEvent.UPLOAD_FAILED);
            }
            return;
        }
        upLoadFilePath = filePath;
        if (eventSender != null) {
            eventSender.sendEvent(OpusEvent.UPLOAD_START);
        }

        uploadThread = new Thread(new UploadRunnable(), "Opus File Upload Thread");
        uploadThread.start();
    }

    /**
     * 文件删除
     *
     * @param filePath 文件路径
     */
    public void delete(String filePath) {
        if (!Utils.isFileExist(filePath)) {
            if (eventSender != null) {
                eventSender.sendEvent(OpusEvent.DELETE_FILE_FAILED);
            }
            return;
        }

        deleteFilePath = filePath;
        if (eventSender != null) {
            eventSender.sendEvent(OpusEvent.DELETE_FILE_START);
        }
        deleteThread = new Thread(new DeleteRunnable(), "File Delete Thread");

        deleteThread.start();
    }

    /**
     * 文件删除
     */
    class DeleteRunnable implements Runnable {
        @Override
        public void run() {
            File file = new File(deleteFilePath);
            // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
            if (file.exists() && file.isFile()) {
                if (file.delete()) {
                    if (eventSender != null) {
                        eventSender.sendEvent(OpusEvent.DELETE_FILE_FINISHED,deleteFilePath);
                    }
                } else {
                    if (eventSender != null) {
                        eventSender.sendEvent(OpusEvent.DELETE_FILE_FAILED,deleteFilePath);
                    }
                }
            } else {
                if (eventSender != null) {
                    eventSender.sendEvent(OpusEvent.DELETE_FILE_FAILED,deleteFilePath);
                }
            }
        }
    }

    /**
     * 文件上传
     */
    class UploadRunnable implements Runnable {

        @Override
        public void run() {
            //TODO 执行文件上传操作

            FTPManager.getInstance().ftp4jUpload(upLoadFilePath, new IResultListener() {
                @Override
                public void onSuccess(String info) {
                    if (eventSender != null) {
                        eventSender.sendEvent(OpusEvent.UPLOAD_FINISHED);
                    }
                }

                @Override
                public void onFilure(String message) {
                    if (eventSender != null) {
                        eventSender.sendEvent(OpusEvent.UPLOAD_FAILED, upLoadFilePath);
                    }
                }
            });
        }
    }

    public boolean isWorking() {
        return state != STATE_NONE;
    }

    public void release() {
        try {
            if (state == STATE_CONVERTING) {
                if (workerThread.isAlive()) {
                    workerThread.interrupt();
                }

                if (uploadThread.isAlive()) {
                    uploadThread.interrupt();
                }
                if (deleteThread.isAlive()) {
                    deleteThread.interrupt();
                }

            }
        } catch (Exception e) {
            Utils.printE(TAG, e);
        } finally {
            state = STATE_NONE;
            if (eventSender != null) {
                eventSender.sendEvent(OpusEvent.CONVERT_FAILED);
            }
        }
    }
}