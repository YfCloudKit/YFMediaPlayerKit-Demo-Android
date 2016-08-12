/*
 * Copyright (C) 2013 yixia.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yunfan.player.demo.advance;


import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import com.example.yfcloudplayer.R;
import com.yunfan.player.adapter.EntranceAdapter;
import com.yunfan.player.bean.VideoItem;
import com.yunfan.player.demo.BasePlayerDemo;
import com.yunfan.player.demo.YfCloudPlayerDemo;
import com.yunfan.player.extra.VideoPaths;
import com.yunfan.player.widget.YfCloudPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class SinglePlayerDemo extends Activity implements SurfaceHolder.Callback {
    private String TAG = "YfSinglePlayerDemo";
    private GridView gv;
    public static List<String> videoPaths = new ArrayList<String>();
    private List<VideoItem> itemList = new ArrayList<VideoItem>();
    private EntranceAdapter adapter;
    public static YfCloudPlayer YfMediaPlayer;
    private SurfaceView mSurface;
    private SurfaceHolder holder;
    private int mVideoWidth;
    private int mVideoHeight;
    private boolean mIsVideoSizeKnown = false;
    private boolean mIsVideoReadyToBePlayed = false;
    private String path = "";
    private boolean keepPlayerAlive = false;
    private YfCloudPlayer.OnVideoSizeChangedListener mSizeChangedListener;
    private YfCloudPlayer.OnPreparedListener mPreparedListener;
    private YfCloudPlayer.OnCompletionListener mCompletionListener;
    private YfCloudPlayer.OnErrorListener mErrorListener;
    public static final String PLAY_PATH = "play_path";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_singleplayer);
        gv = (GridView) findViewById(R.id.gview);
        mSurface = (SurfaceView) findViewById(R.id.surface_view);
        holder = mSurface.getHolder();
        holder.addCallback(this);
        holder.setFormat(PixelFormat.RGBA_8888);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        init();
    }

    private Bitmap getCapture(String path) {
        Bitmap bitmap = null;
        return bitmap;
    }

    private void init() {
        videoPaths = getVideoPaths();
        int i;
        for (i = 0; i < VideoPaths.Videos.length; i++) {
            videoPaths.add(VideoPaths.Videos[i]);
        }
        String videoName;
        for (i = 0; i < videoPaths.size(); i++) {
            VideoItem si = new VideoItem();
            videoName = videoPaths.get(i).substring(videoPaths.get(i).lastIndexOf("/") + 1);
            si.setVideoName(videoName);
            si.setVideoCaptrue(getCapture(videoPaths.get(i)));
            itemList.add(si);
        }
        adapter = new EntranceAdapter(this, itemList);
        gv.setAdapter(adapter);
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                openVideoInOrtherActivity(videoPaths.get(position), position);
            }
        });
    }

    public void PlayManualUrl(View v) {
        openVideoInOrtherActivity(((EditText) findViewById(R.id.manual_add)).getText().toString(), 0);
    }

    private void openVideoInOrtherActivity(String path, int currentPosition) {
        YfMediaPlayer.reset();
        keepPlayerAlive = true;//因为在后续打开的activity要使用当前player，所以标志不允许操作该player
        Intent videoViewDemo = new Intent(this, YfCloudPlayerDemo.class);
        videoViewDemo.putExtra(BasePlayerDemo.PLAY_PATH, path);
        videoViewDemo.putExtra(BasePlayerDemo.CURRENT_POSITION, currentPosition);
        videoViewDemo.putExtra(YfCloudPlayerDemo.SINGLE_MODE, true);
        startActivityForResult(videoViewDemo, 10086);
        overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
    }

    private List<String> getVideoPaths() {
        List<String> videoPathsList = new ArrayList<String>();
        ContentResolver contentResolver = getContentResolver();
        String[] projection = new String[]{MediaStore.Video.Media.DATA};
        Cursor cursor = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection,
                null, null, MediaStore.Video.Media.DEFAULT_SORT_ORDER);
        int fileNum = 0;
        if (cursor != null) {
            cursor.moveToFirst();
            fileNum = cursor.getCount();
            for (int counter = 0; counter < fileNum; counter++) {
                videoPathsList.add(cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA)));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return videoPathsList;
    }

    @Override
    protected void onDestroy() {
        for (int i = 0; i < itemList.size(); i++) {
            if (itemList.get(i).getVideoCaptrue() != null)
                itemList.get(i).getVideoCaptrue().recycle();
        }
        Log.d(TAG, "onDestroy");
        if(YfMediaPlayer!=null) {
            YfMediaPlayer.release();
            YfMediaPlayer = null;
        }
        super.onDestroy();
    }

    public void openVideo() {
        Log.d(TAG, "播放地址是:" + path);
        YfMediaPlayer = YfCloudPlayer.Factory.createPlayer(this,YfCloudPlayer.MODE_HARD);
        if (!TextUtils.isEmpty(path)) {
            initPlayListener();
            mVideoWidth = 0;
            mVideoHeight = 0;
            mIsVideoReadyToBePlayed = false;
            mIsVideoSizeKnown = false;
            YfMediaPlayer.setBufferSize(15 * 1024 * 1024);
            try {
                YfMediaPlayer.setDataSource(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "进来设置display");
            YfMediaPlayer.setDisplay(holder);
            YfMediaPlayer.prepareAsync();
            YfMediaPlayer.setOnCompletionListener(mCompletionListener);
            YfMediaPlayer.setOnPreparedListener(mPreparedListener);
            YfMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            YfMediaPlayer.setOnErrorListener(mErrorListener);
            mSurface.requestFocus();
        }
    }

    public void initPlayListener() {
        mPreparedListener = new YfCloudPlayer.OnPreparedListener() {
            public void onPrepared(YfCloudPlayer mp) {
                mIsVideoReadyToBePlayed = true;
                if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
                    startVideoPlayback();
                }
            }
        };
        mSizeChangedListener =
                new YfCloudPlayer.OnVideoSizeChangedListener() {
                    public void onVideoSizeChanged(YfCloudPlayer mp, int width, int height,
                                                   int sar_num, int sar_den) {
                        if (width == 0 || height == 0) {
                            return;
                        }
                        mIsVideoSizeKnown = true;
                        mVideoWidth = width;
                        mVideoHeight = height;
                        if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
                            startVideoPlayback();
                        }
                    }
                };
        mErrorListener = new YfCloudPlayer.OnErrorListener() {

            @Override
            public boolean onError(YfCloudPlayer mp, int what, int extra) {
                return false;
            }
        };
        mCompletionListener =
                new YfCloudPlayer.OnCompletionListener() {
                    public void onCompletion(YfCloudPlayer mp) {
                        Toast.makeText(SinglePlayerDemo.this, "播放结束", Toast.LENGTH_LONG).show();
                    }
                };

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        this.holder = holder;
        keepPlayerAlive = false;
        if ("".equals(path)) {
            path = getIntent().getStringExtra(PLAY_PATH);
        }
        Log.d(TAG,"YfMediaPlayer:"+YfMediaPlayer+(YfMediaPlayer!=null?("___canContinueRender:"+YfMediaPlayer.canContinueRender()):""));
        if (YfMediaPlayer != null && YfMediaPlayer.canContinueRender()) {
            YfMediaPlayer.continueRender(holder);
        } else {
            openVideo();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        if (!keepPlayerAlive&&YfMediaPlayer!=null) {
            YfMediaPlayer.release();
            YfMediaPlayer=null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop");
        super.onStop();
    }

    private void startVideoPlayback() {
        Log.v(TAG, "startVideoPlayback");
        holder.setFixedSize(mVideoWidth, mVideoHeight);
        YfMediaPlayer.start();
    }


}
