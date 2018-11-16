package com.sometimestwo.jumble.Utils;

import android.os.AsyncTask;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import okhttp3.OkHttpClient;



public class DownloadManager extends AsyncTask<String, Integer, Integer> {
    public static final int DOWNLOAD_SUCCESS = 1;
    public static final int DOWNLOAD_FAILED = 2;
    public static final int DOWNLOAD_PAUSED = 3;
    public static final int DOWNLOAD_CANCELED = 4;
    private DownloadListener downloadListener = null;
    String newFilename = Constants.DEFAULT_DOWNLOAD_FILENAME;
    private boolean downloadCanceled = false;
    private boolean downloadPaused = false;
    private int lastDownloadProgress = 0;
    private String currDownloadUrl = "";
    private String mNewFilenameAbsolutePath = "";

    private static OkHttpClient okHttpClient = new OkHttpClient();
    public boolean isDownloadCanceled() {
        return downloadCanceled;
    }

    public void setDownloadCanceled(boolean downloadCanceled) {
        this.downloadCanceled = downloadCanceled;
    }

    public boolean isDownloadPaused() {
        return downloadPaused;
    }

    public void setDownloadPaused(boolean downloadPaused) {
        this.downloadPaused = downloadPaused;
    }

    public int getLastDownloadProgress() {
        return lastDownloadProgress;
    }

    public void setLastDownloadProgress(int lastDownloadProgress) {
        this.lastDownloadProgress = lastDownloadProgress;
    }

    public DownloadManager(DownloadListener downloadListener,
                           String newFilename) {
        this.downloadListener = downloadListener;
        this.newFilename = newFilename;
        this.setDownloadPaused(false);
        this.setDownloadCanceled(false);
    }

    /* This method is invoked after doInBackground() method. */
    @Override
    protected void onPostExecute(Integer downloadStatue) {
        if(downloadStatue == DOWNLOAD_SUCCESS)
        {
            this.setDownloadCanceled(false);
            this.setDownloadPaused(false);
            downloadListener.onSuccess();
            //scanFile();
        }else if(downloadStatue == DOWNLOAD_FAILED)
        {
            this.setDownloadCanceled(false);
            this.setDownloadPaused(false);
            downloadListener.onFailed();
        }else if(downloadStatue == DOWNLOAD_PAUSED)
        {
            downloadListener.onPaused();
        }else if(downloadStatue == DOWNLOAD_CANCELED)
        {
            downloadListener.onCanceled();
        }
    }


    /* Invoked when this async task execute.When this method return, onPostExecute() method will be called.*/
    @Override
    protected Integer doInBackground(String... params) {

        // Set current thread priority lower than main thread priority, so main thread Pause, Continue and Cancel action will not be blocked.
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 2);

        String downloadFileUrl = "";
        if(params!=null && params.length > 0)
        {
            downloadFileUrl = params[0];
        }

        currDownloadUrl = downloadFileUrl;
        File downloadLocalFile = createDownloadLocalFile(downloadFileUrl);

        int ret = DownloadUtil.downloadFileFromUrl(downloadFileUrl, downloadLocalFile);

        return ret;
    }

    /*
     * Parse the download file name from the download file url,
     * check whether the file exist in sdcard download directory or not.
     * If the file do not exist then create it.
     *
     * Return the file object.
     * */
    private File createDownloadLocalFile(String downloadFileUrl)
    {
        File dir = null;
        File ret = null;

        try {
            if (downloadFileUrl != null && !TextUtils.isEmpty(downloadFileUrl)) {
                int lastIndex = downloadFileUrl.lastIndexOf("/");
                if (lastIndex > -1) {
                    String downloadFileName = newFilename;
                    File downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    String downloadDirectoryPath =
                            downloadDirectory.getPath()
                            + File.separator
                            + Constants.APP_NAME;

                    dir = new File(downloadDirectoryPath);
                    if(!dir.exists()){
                        dir.mkdirs();
                    }

                    ret = new File(downloadDirectoryPath + "/" + downloadFileName);

                    if (!ret.exists()) {
                      ret.createNewFile();
                    }
                }
            }
        }catch(IOException ex)
        {
            //Log.e("Download Manager", ex.getMessage(), ex);
        }finally {
            return ret;
        }
    }

    /* Update download async task progress. */
    public void updateTaskProgress(Integer newDownloadProgress)
    {
        lastDownloadProgress = newDownloadProgress;
        downloadListener.onUpdateDownloadProgress(newDownloadProgress);
    }

    public void pauseDownload()
    {
        this.setDownloadPaused(true);
    }

    public void cancelDownload()
    {
        this.setDownloadCanceled(true);
    }


}