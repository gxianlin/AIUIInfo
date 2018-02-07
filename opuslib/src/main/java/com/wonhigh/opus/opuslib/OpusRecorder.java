package com.wonhigh.opus.opuslib;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.wonhigh.opus.opuslib.model.AudioTime;
import com.wonhigh.opus.opuslib.utils.LogHelper;
import com.wonhigh.opus.opuslib.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import static android.media.AudioRecord.ERROR_INVALID_OPERATION;
import static com.wonhigh.opus.opuslib.OpusEvent.RECORD_FAILED;
import static com.wonhigh.opus.opuslib.OpusEvent.RECORD_FINISHED;

/**
 * 录音处理类
 */
public class OpusRecorder {
    private static final String TAG = LogHelper.makeLogTag(OpusRecorder.class);

    //录音文件保存名称前缀
//    public static String fileHeaderName = "OpusRecord";
//    public static String pcmHeaderName = "PcmRecord";

    private final String option = " --framesize 2.5 --comp 10 --vbr --bitrate 32";

    //录音机状态
    private static final int STATE_NONE = 0;
    private static final int STATE_STARTED = 1;
    private volatile int state = STATE_NONE;

    //音频采样率
    private static final int RECORDER_SAMPLERATE = 16000;
    //设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    //音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    //音频大小
    private int bufferSize = 0;
    private AudioRecord recorder = null;
    private Thread recordingThread = new Thread();
    private OpusTool opusTool = new OpusTool();
    private static volatile OpusRecorder opusRecorder;


    // Should be 1920, to meet with function writeFrame()
    private ByteBuffer opusBuffer = ByteBuffer.allocateDirect(1920);
    private ByteBuffer pcmBuffer = ByteBuffer.allocateDirect(1920);

    private String opusFilePath = null;
    private String pcmFilePath = null;
    private String wavFilePath = null;

    private OpusEvent eventSender = null;
    private Timer progressTimer = null;
    private AudioTime recordTime = new AudioTime();
    private boolean isSavePcm = true;
    private FileOutputStream pcmFos;

    private OpusRecorder() {
    }

    /**
     * 获取单例
     *
     * @return
     */
    public static OpusRecorder getInstance() {
        if (opusRecorder == null) {
            synchronized (OpusRecorder.class) {
                if (opusRecorder == null) {
                    opusRecorder = new OpusRecorder();
                }
            }
        }
        return opusRecorder;
    }

    public void setEventSender(OpusEvent es) {
        eventSender = es;
    }



    /**
     * 开始录音
     *
     * @param isSavePcm 是否保存pcm格式文件
     */
    public void startRecording(boolean isSavePcm) {
        this.isSavePcm = isSavePcm;
        //如果录音机正在录音
        if (state == STATE_STARTED) {
            return;
        }

        //获取数据大小
        int minBufferSize = AudioRecord.getMinBufferSize(
                RECORDER_SAMPLERATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING
        );

        bufferSize = (minBufferSize / 1920 + 1) * 1920;

        //初始化录音机
        recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING,
                bufferSize
        );

        //开始录音
        recorder.startRecording();


        //创建文件路径
        opusFilePath = OpusTrackInfo.getInstance().getOpusFileName();
        pcmFilePath = OpusTrackInfo.getInstance().getPcmFileName();
        wavFilePath = OpusTrackInfo.getInstance().getWavFileName();

        int rst = opusTool.startRecording(opusFilePath);
        LogHelper.e(TAG, "opusFilePath = " + opusFilePath);
        if (rst != 1) {
            if (eventSender != null) {
                eventSender.sendEvent(RECORD_FAILED);
            }
            LogHelper.e(TAG, "recorder initially error");
            return;
        }

        //改变录音机状态
        state = STATE_STARTED;

        if (eventSender != null) {
            eventSender.sendEvent(OpusEvent.RECORD_STARTED);
        }
        recordingThread = new Thread(new RecordThread(), "OpusRecord Thread");
        recordingThread.start();
    }


    /**
     * 将音频流保存PCM文件
     */
    private void writeAudioToPCM() {
        // new一个byte数组用来存一些字节数据，大小为缓冲区大小
        byte[] audiodata = new byte[bufferSize];
        FileOutputStream fos = null;
        int readsize = 0;

        try {
            File file = new File(pcmFilePath);
            if (file.exists()) {
                file.delete();
            }
            fos = new FileOutputStream(file);// 建立一个可存取字节的文件
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (state == STATE_STARTED) {
            readsize = recorder.read(audiodata, 0, bufferSize);
            if (AudioRecord.ERROR_INVALID_OPERATION != readsize && fos != null) {
                try {
                    fos.write(audiodata);
                } catch (IOException e) {
                    e.printStackTrace();
                    if (eventSender != null) {
                        eventSender.sendEvent(RECORD_FAILED);
                    }
                    LogHelper.e(TAG, "recorder initially error");
                }
            }
        }
        try {
            if (fos != null)
                fos.close();// 关闭写入流
        } catch (IOException e) {
            e.printStackTrace();
            if (eventSender != null) {
                eventSender.sendEvent(RECORD_FAILED);
            }
            LogHelper.e(TAG, "recorder initially error");
        }
    }


    /**
     * 将数据写入文件
     *
     * @param buffer
     * @param size
     */
    private void writeAudioDataToFile(ByteBuffer buffer, int size) {
        ByteBuffer finalBuffer = ByteBuffer.allocateDirect(size);
        finalBuffer.put(buffer);
        finalBuffer.rewind();

        //调用jni opus编码得到opus文件
        boolean flush = false;
        while (state == STATE_STARTED && finalBuffer.hasRemaining()) {
            int oldLimit = -1;
            if (finalBuffer.remaining() > opusBuffer.remaining()) {
                oldLimit = finalBuffer.limit();
                finalBuffer.limit(opusBuffer.remaining() + finalBuffer.position());
            }
            opusBuffer.put(finalBuffer);

            if (opusBuffer.position() == opusBuffer.limit() || flush) {
                int length = !flush ? opusBuffer.limit() : finalBuffer.position();
                int rst = opusTool.writeFrame(opusBuffer, length);

                if (rst != 0) {
                    opusBuffer.rewind();
                }
            }
            if (oldLimit != -1) {
                finalBuffer.limit(oldLimit);
            }
        }
    }

    /**
     * 将录音输输出流数据写入文件 opus
     */

    private void writeAudioDataToFile() {
        if (state != STATE_STARTED)
            return;

        //opus编码
        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);

        if (isSavePcm) {
            try {
                pcmFos = new FileOutputStream(pcmFilePath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        while (state == STATE_STARTED) {
            buffer.rewind();
            int len = recorder.read(buffer, bufferSize);
            LogHelper.d(TAG, "\n bufferSize's length is " + len);

            //判断是否需要保存pcm文件
            if (isSavePcm && pcmFos != null) {
                //复制数据用于保存pcm格式文件
                byte[] bytes = new byte[bufferSize];
                System.arraycopy(buffer.array(), 0, bytes, 0, bufferSize);
                //保存pcm格式文件
                try {
                    pcmFos.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            if (len != ERROR_INVALID_OPERATION) {
                try {
                    //opus编码处理
                    writeAudioDataToFile(buffer, len);
                } catch (Exception e) {
                    if (eventSender != null)
                        eventSender.sendEvent(RECORD_FAILED);
                    Utils.printE(TAG, e);
                }
            }


        }

        try {
            if (isSavePcm && pcmFos != null) {
                pcmFos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("tag", "pcmFos close()出错");
        }
    }


    //录音完成后发出通知
    private void updateTrackInfo() {
        OpusTrackInfo info = OpusTrackInfo.getInstance();
        info.addOpusFile(opusFilePath);

        if (eventSender != null) {
            File f = new File(opusFilePath);
            eventSender.sendEvent(RECORD_FINISHED, f.getName());
        }
    }

    /**
     * 停止录音
     */
    public void stopRecording() {
        if (state != STATE_STARTED) {
            return;
        }

        state = STATE_NONE;

        progressTimer.cancel();

        try {
            Thread.sleep(200);
        } catch (Exception e) {
            Utils.printE(TAG, e);
        }

        if (null != recorder) {
            opusTool.stopRecording();

            recordingThread = null;
            recorder.stop();
            recorder.release();
            recorder = null;
        }

        //发通知
        updateTrackInfo();
    }

    //判断录音机是否在录音
    public boolean isWorking() {
        return state != STATE_NONE;
    }

    //释放录音机
    public void release() {
        if (state != STATE_NONE) {
            stopRecording();
        }
    }

    /**
     * 录音时长计时器
     */
    private class ProgressTask extends TimerTask {
        public void run() {
            if (state != STATE_STARTED) {
                progressTimer.cancel();
            } else {
                recordTime.add(1);
                String progress = recordTime.getTime();
                if (eventSender != null) {
                    eventSender.sendRecordProgressEvent(progress);
                }
            }
        }
    }


    /**
     * 录音数据保存线程
     */
    private class RecordThread implements Runnable {
        public void run() {
            progressTimer = new Timer();
            recordTime.setTimeInSecond(0);
            progressTimer.schedule(new ProgressTask(), 1000, 1000);

            writeAudioDataToFile();
        }
    }


    /**
     * 这里得到可播放的音频文件
     *
     * @param inFilename
     * @param outFilename
     * @param bufferSizeInBytes
     */
    public static void pcm2Wav(String inFilename, String outFilename, int bufferSizeInBytes) {
        Log.i("tag", "pcm2wav 开始");
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = 16000;
        int channels = 1;
        long byteRate = 16 * 16000 * channels / 8;
        byte[] data = new byte[bufferSizeInBytes];
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            writeWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();
            Log.i("tag", "pcm2wav 结束");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 这里提供一个头信息。插入这些信息就可以得到可以播放的文件。 为啥插入这44个字节，这个还真没深入研究，不过你随便打开一个wav
     * 音频的文件，可以发现前面的头文件可以说基本一样哦。每种格式的文件都有 自己特有的头文件。
     */
    private static void writeWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate,
                                            int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }

}
