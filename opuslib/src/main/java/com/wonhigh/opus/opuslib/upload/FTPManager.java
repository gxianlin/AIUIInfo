package com.wonhigh.opus.opuslib.upload;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;

/**
 * 描述： TODO
 * 作者： gong.xl
 * 邮箱： gong.xl@belle.com.cn
 * 创建时间： 2018/2/1 11:26
 * 修改时间： 2018/2/1 11:26
 * 修改备注：
 */

public class FTPManager {
    private static FTPManager instance;

    private FTPManager() {

    }

    /**
     * @return
     * @throws
     * @Title: getInstance
     * @Description: 单例方式提供对象
     */
    public static FTPManager getInstance() {
        if (instance == null) {
            synchronized (FTPManager.class) {
                if (instance == null) {
                    instance = new FTPManager();
                }
            }
        }
        return instance;
    }

    public void ftp4jUpload(final String path, final IResultListener listener) {
        try {
            String targetName = ftp4jUpload(path);
            if (listener != null) {
                listener.onSuccess(targetName);
            }
        } catch (IllegalStateException | IOException
                | FTPIllegalReplyException | FTPException
                | FTPDataTransferException | FTPAbortedException e) {
            e.printStackTrace();
            Log.d("lixm", "ftp4jUpload error : ", e);
            if (listener != null) {
                listener.onFilure(e.getMessage());
            }
        }
    }

    /**
     * FTP协议文件上传
     *
     * @param path
     * @throws FTPException
     * @throws FTPIllegalReplyException
     * @throws IOException
     * @throws IllegalStateException
     * @throws FTPAbortedException
     * @throws FTPDataTransferException
     */
    public String ftp4jUpload(String path) throws IllegalStateException, IOException,
            FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException {
        // 创建客户端
        final FTPClient client = new FTPClient();

        //主机地址
        String rightIP = "203.86.24.32";
        client.connect(rightIP, 21);

        // 用户登录
        client.login("rwftp", "42N2N565");

        //切换上传路径
        client.changeDirectory("/sr");
        File file = new File(path);
        client.upload(file);

        return "上传成功";
    }
}
