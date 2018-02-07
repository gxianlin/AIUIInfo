package com.wonhigh.opus.aiuiinfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 描述： TODO
 * 作者： gong.xl
 * 邮箱： gong.xl@belle.com.cn
 * 创建时间： 2018/2/2 18:08
 * 修改时间： 2018/2/2 18:08
 * 修改备注：
 */

public class Pcm2WavUtil {

    /**
     * 这里得到可播放的音频文件
     *
     * @param inFilename
     * @param outFilename
     * @param bufferSizeInBytes
     */
    public static void pcm2wav(String inFilename, String outFilename, int bufferSizeInBytes)
            throws IOException {

        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = 16000;
        int channels = 1;
        long byteRate = 16 * 16000 * channels / 8;
        byte[] data = new byte[bufferSizeInBytes];

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

    }

    /**
     * 这里提供一个头信息。插入这些信息就可以得到可以播放的文件。 为啥插入这44个字节，这个还真没深入研究，不过你随便打开一个wav
     * 音频的文件，可以发现前面的头文件可以说基本一样哦。每种格式的文件都有 自己特有的头文件。
     */
    private static void writeWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                            long totalDataLen, long longSampleRate,
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
