package com.myapplicationdev.android.p10_getting_my_location;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class MyService extends Service {

    private static final String DEBUG_TAG = MyService.class.getSimpleName();
    private boolean started;
    private MediaPlayer player = new MediaPlayer();

    public MyService() {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(DEBUG_TAG, "Service created");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        started = true;
        Log.d(DEBUG_TAG, started ? "Service started" : "Service is still running");
        // Log.d(DEBUG_TAG, playMusic() + " music int");
        return playMusic();
    }

    @Override
    public void onDestroy() {
        Log.d(DEBUG_TAG, "Service stopped");
        stopMusic();
        super.onDestroy();
    }

    private int playMusic() {
        String folderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyFolder";
        File filePath = new File(folderPath, "music.mp3");
        Log.d(DEBUG_TAG, filePath.getAbsolutePath());
        try {
            player.setDataSource(filePath.getPath());
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.setLooping(true);
        player.start();
        return START_STICKY;
    }

    private void stopMusic() {
        player.stop();
    }
}