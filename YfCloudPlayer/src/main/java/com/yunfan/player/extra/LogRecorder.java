package com.yunfan.player.extra;

import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xjx-pc on 2016/4/8 0008.
 */
public class LogRecorder {
    private List<String> logs = new ArrayList<>();
    private boolean writeToSdcard = false;
    String[] tempString;

    public void writeLog(String content) {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("hh:mm:ss.SSS");
        String date = sDateFormat.format(new java.util.Date());
        logs.add(content + "\ntime:" + date);
        if (writeToSdcard) saveToLocal(content + "\ntime:" + date + "\n");

    }

    public List<String> getLogsWithList() {
        return logs;
    }

    public String[] getLogs() {
        tempString = new String[logs.size()];
        for (int i = 0; i < logs.size(); i++) {
            tempString[i] = logs.get(i);
        }
        return tempString;
    }

    public void clearLogs() {
        logs.clear();
    }

    public void destroy() {
        clearLogs();
        saveToLocal("----------------------------------------------\n");
    }

    public void saveToLocal(String str) {
        String path = "/sdcard/yunfanplayer";
        String fileName = "log";
        try {
            File dir = new File(path);
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, fileName + ".txt");
            FileOutputStream outputStream = new FileOutputStream(f, true);
            outputStream.write(str.getBytes());
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
