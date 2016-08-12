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

package com.yunfan.player.demo;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;

import com.example.yfcloudplayer.R;
import com.yunfan.player.MainActivity;
import com.yunfan.player.demo.advance.SinglePlayerDemo;
import com.yunfan.player.extra.CustomMediaController;
import com.yunfan.player.extra.Utils;
import com.yunfan.player.extra.YfP2PHelper;
import com.yunfan.player.widget.MediaInfo;
import com.yunfan.player.widget.YfCloudPlayer;
import com.yunfan.player.widget.YfMediaFormat;
import com.yunfan.player.widget.YfTrackInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


public class YfCloudPlayerDemo extends BasePlayerDemo implements SurfaceHolder.Callback, MediaController.MediaPlayerControl, YfCloudPlayer.OnBufferingUpdateListener,YfCloudPlayer.OnInfoListener {
    private String TAG = "Yf_MediaPlayer";
    private int mVideoWidth;
    private int mVideoHeight;
    private String path = "";
    private View rootView;
    private boolean singleMode = false;
    private boolean keepPlayerAlive = false;
    private boolean mInitEnd = false;
    private boolean mIsVideoSizeKnown = false;
    private boolean mIsVideoReadyToBePlayed = false;
    public static final String PLAY_PATH = "play_path";
    public static final String SINGLE_MODE = "SINGLE_MODE";
    public static final String CURRENT_POSITION = "current_position";
    public YfCloudPlayer YfMediaPlayer;
    private SurfaceView mSurface;
    private SurfaceHolder holder;
    private FrameLayout mSurfaceView_layout;



    private int mDecodeMode = MainActivity.DECODE_MODE > 1 ? YfCloudPlayer.MODE_SOFT : YfCloudPlayer.MODE_HARD;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        rootView = LayoutInflater.from(this).inflate(
                R.layout.activity_yfcloudplayer, null);
        setContentView(rootView);
        singleMode = getIntent().getBooleanExtra(SINGLE_MODE, singleMode);
        mSurface = (SurfaceView) findViewById(R.id.surface_view);
        mSurface.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (YfMediaPlayer.isPlaying() && mMediaController != null) {
                    toggleMediaControlsVisiblity();
                }
                return false;
            }
        });
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        YfMediaController = new CustomMediaController(this);
        YfMediaController.setSupportActionBar(actionBar);


        mSurfaceView_layout = (FrameLayout) findViewById(R.id.videoView_layout);
        mSurfaceView_layout.setLayoutParams(getLayoutParams(isFulllScreen));
        holder = mSurface.getHolder();
        holder.addCallback(this);
        holder.setFormat(PixelFormat.RGBA_8888);
        Log.d(TAG, "OnCreateis，FullScreen:" + isFulllScreen);
        mCurrentVideoIndex = getIntent().getIntExtra(CURRENT_POSITION, 0);
        setMediaController(YfMediaController);
        Log.d(TAG, "set controller end,actionBar:"+actionBar);
//        Toast.makeText(YfCloudPlayerDemo.this, "解码方式为:" + MainActivity.DecodeModes[MainActivity.DECODE_MODE], Toast.LENGTH_SHORT).show();
    }

    ////////////////////////////////////////点播网络库相关代码////////////////////////////////////////////
    //该地址每隔一段时间会变，所以要从这个地址获取：http://disp.titan.mgtv.com/vod.do?fmt=2&pno=1120&fid=5F1238A38D7C3F136BD69CDB7B49768F&file=/mp4/2016/zongyi/wsgsdsj_49299/5F1238A38D7C3F136BD69CDB7B49768F_20160120_1_1_417.mp4/playlist.m3u8&gsid=96bc782d15df4c849e712b057a1f8139&guid=690456323737260032
    String mStrOrgUrl = new String("http://14.18.142.198/mp4/2016/zongyi/wsgsdsj_49299/5F1238A38D7C3F136BD69CDB7B49768F_20160120_1_1_417.mp4/playlist.m3u8?uuid=b5af5d928f72487cb2a0a69338e67ba5&t=56ab78ed&win=300&pno=1120&srgid=29&urgid=372&srgids=29&nid=931&payload=usertoken%3duuid%3db5af5d928f72487cb2a0a69338e67ba5%5eruip%3d3395848389%5ehit%3d1&rdur=21600&arange=0&limitrate=0&fid=5F1238A38D7C3F136BD69CDB7B49768F&sign=9cfad2fbe3ce6a3834447531b0a0440a&ver=0x03");
    String mStrKeyUrl = new String("http://hunantv.com/hunantv.imgo.tv/2951506_1.m3u8");


    //////////////////////////////////////////////////////////////////////////////////////////////////
    private YfCloudPlayer.OnVideoSizeChangedListener mSizeChangedListener;
    private YfCloudPlayer.OnPreparedListener mPreparedListener;
    private YfCloudPlayer.OnCompletionListener mCompletionListener;
    private YfCloudPlayer.OnErrorListener mErrorListener;
    private long startTime=0;
    public void openVideo() {
        Log.d(TAG, "播放地址是:" + path+",play mode:"+singleMode);
        if (singleMode) {
            YfMediaPlayer = SinglePlayerDemo.YfMediaPlayer;
            YfMediaPlayer.setHardwareDecoder(true);
        } else {
            if(YfMediaPlayer==null)
                YfMediaPlayer = YfCloudPlayer.Factory.createPlayer(this, mDecodeMode);
        }
        if (!TextUtils.isEmpty(path)) {
            initPlayListener();
            mVideoWidth = 0;
            mVideoHeight = 0;
            mIsVideoReadyToBePlayed = false;
            mIsVideoSizeKnown = false;
            YfMediaPlayer.setHardwareDecoder(mDecodeMode==0?true:false);//reset之后解码方式会还原默认的硬解，所以在这里要再手动设置一次解码方式比较好
            YfMediaPlayer.setBufferSize(15 * 1024 * 1024);
//            YfMediaPlayer.setHTTPTimeOutUs(10000000);
            if (p2pMode)
                path = YfP2PHelper.createDemandNetTask(mStrOrgUrl, mStrKeyUrl);//demo这里将播放地址写死，所以打开的每个视频都是一样的，供测试使用
            try {
                startTime= SystemClock.elapsedRealtime();
                YfMediaPlayer.setDataSource(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "进来设置display");
            YfMediaPlayer.setDisplay(holder);
            YfMediaPlayer.prepareAsync();
            YfMediaPlayer.setOnCompletionListener(mCompletionListener);
            YfMediaPlayer.setOnInfoListener(this);
            YfMediaPlayer.setOnBufferingUpdateListener(this);
            YfMediaPlayer.setOnPreparedListener(mPreparedListener);
            YfMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            YfMediaPlayer.setOnErrorListener(mErrorListener);
            mSurface.requestFocus();
        }
        mInitEnd = true;
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
                        next();

                    }
                };

    }

    private void onPlayError() {
        Log.d(TAG, "wifi状态：" + Utils.isWiFiActive(this) + "————播放状态：" + isPlaying());
        YfMediaPlayer.pause();
        if (!Utils.isWiFiActive(this) && !isPlaying()) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("继续播放吗?");
            builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Log.d(TAG, "重置播放器2");
                    release();
                    openVideo();
                }
            });
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();

        }
    }

    public int getOrientation() {
        return isFulllScreen ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
    }

    /**
     * 播放下一个视频
     */
    public void next() {
        release();
        mCurrentVideoIndex++;
        if (mCurrentVideoIndex > MainActivity.videoPaths.size() - 1) {
            mCurrentVideoIndex = 0;
        }
        path = MainActivity.videoPaths.get(mCurrentVideoIndex);
        openVideo();
    }

    @Override
    public void start() {
        startVideoPlayback();
    }

    @Override
    public void pause() {
        if (YfMediaPlayer != null)
            YfMediaPlayer.pause();
    }

    @Override
    public int getDuration() {
        return (int) YfMediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return (int) YfMediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        YfMediaPlayer.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        if (YfMediaPlayer != null)
            return YfMediaPlayer.isPlaying();
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return bufferingPercentage;
    }

    private int bufferingPercentage = 0;

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public void onWifiConnected() {
//        openVideo();
    }

    @Override
    public void showLog() {
        Log.d(TAG, "查看消息");
    }

    ArrayList<HashMap<String, Object>> listitem;
    SimpleAdapter adapter;

    @Override
    public void showInfo() {
        if (YfMediaPlayer == null || !YfMediaPlayer.isPlaying()) {
            return;
        }
        if (listitem != null) {
            listitem.clear();
        } else {
            listitem = new ArrayList<HashMap<String, Object>>();
        }

        View contentView = LayoutInflater.from(this).inflate(
                R.layout.simple_list, null);
        PopupWindow pop = new PopupWindow(contentView,
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, true);
        ListView lv = (ListView) contentView.findViewById(R.id.list);
        MediaInfo mediaInfo = YfMediaPlayer.getMediaInfo();
        listitem.add(createInfoMap("VideoDecoder", mediaInfo.getVideoDecoder()));
        listitem.add(createInfoMap("AudioDecoder", mediaInfo.getAudioDecoder()));
        listitem.add(createInfoMap("duration", mediaInfo.getMeta().mDurationUS + ""));
        listitem.add(createInfoMap("publishtime", mediaInfo.getMeta().publishtime + ""));
        listitem.add(createInfoMap("currenttime", mediaInfo.getMeta().currenttime + ""));
        YfTrackInfo trackInfos[] = mediaInfo.getTrackInfo();
        if (trackInfos != null) {
            for (YfTrackInfo trackInfo : trackInfos) {
                YfMediaFormat mediaFormat = trackInfo.getFormat();
                int trackType = trackInfo.getTrackType();
                switch (trackType) {
                    case YfTrackInfo.MEDIA_TRACK_TYPE_VIDEO:
                        listitem.add(createInfoMap(YfMediaFormat.KEY_YF_CODEC_LONG_NAME_UI, mediaFormat.getString(YfMediaFormat.KEY_YF_CODEC_LONG_NAME_UI)));
                        listitem.add(createInfoMap(YfMediaFormat.KEY_YF_CODEC_PROFILE_LEVEL_UI, mediaFormat.getString(YfMediaFormat.KEY_YF_CODEC_PROFILE_LEVEL_UI)));
                        listitem.add(createInfoMap(YfMediaFormat.KEY_YF_CODEC_PIXEL_FORMAT_UI, mediaFormat.getString(YfMediaFormat.KEY_YF_CODEC_PIXEL_FORMAT_UI)));
                        listitem.add(createInfoMap(YfMediaFormat.KEY_YF_RESOLUTION_UI, mediaFormat.getString(YfMediaFormat.KEY_YF_RESOLUTION_UI)));
                        listitem.add(createInfoMap(YfMediaFormat.KEY_YF_FRAME_RATE_UI, mediaFormat.getString(YfMediaFormat.KEY_YF_FRAME_RATE_UI)));
                        listitem.add(createInfoMap(YfMediaFormat.KEY_YF_BIT_RATE_UI, mediaFormat.getString(YfMediaFormat.KEY_YF_BIT_RATE_UI)));
                        break;
                    case YfTrackInfo.MEDIA_TRACK_TYPE_AUDIO:
                        listitem.add(createInfoMap(YfMediaFormat.KEY_YF_CODEC_LONG_NAME_UI, mediaFormat.getString(YfMediaFormat.KEY_YF_CODEC_LONG_NAME_UI)));
                        listitem.add(createInfoMap(YfMediaFormat.KEY_YF_CODEC_PROFILE_LEVEL_UI, mediaFormat.getString(YfMediaFormat.KEY_YF_CODEC_PROFILE_LEVEL_UI)));
                        listitem.add(createInfoMap(YfMediaFormat.KEY_YF_SAMPLE_RATE_UI, mediaFormat.getString(YfMediaFormat.KEY_YF_SAMPLE_RATE_UI)));
                        listitem.add(createInfoMap(YfMediaFormat.KEY_YF_CHANNEL_UI, mediaFormat.getString(YfMediaFormat.KEY_YF_CHANNEL_UI)));
                        listitem.add(createInfoMap(YfMediaFormat.KEY_YF_BIT_RATE_UI, mediaFormat.getString(YfMediaFormat.KEY_YF_BIT_RATE_UI)));
                        break;
                    default:
                        break;
                }
            }
        }
        adapter = new SimpleAdapter(this,
                listitem,
                R.layout.item_menu_info_list,
                new String[]{"title", "content"},
                new int[]{R.id.title, R.id.content});
        lv.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        pop.setBackgroundDrawable(getResources().getDrawable(R.drawable.pop_back));
        pop.showAtLocation(rootView, Gravity.CENTER, 0, 0);
    }

    @Override
    public void changeRatio(int ratio) {

    }

    @Override
    public void setLayoutParams(FrameLayout.LayoutParams lp) {
        Log.d(TAG, "setLayoutParams" + lp);
        mSurfaceView_layout.setLayoutParams(lp);
        mSurface.setLayoutParams(lp);
    }


    /**
     * 切换窗口模式和全屏模式
     */
    public void changeSizeType(boolean auto) {
        if (mInitEnd) {
            Log.d(TAG, "isFullScreen:" + isFulllScreen + "__" + auto);
            if (!isFulllScreen) {
                setToFullScreenMode(auto);
                isFulllScreen = true;
            } else {
                setToWindowMode(auto);
                isFulllScreen = false;
            }
        }
        orientationHandler.sendEmptyMessageDelayed(0, 3000);
    }

    @Override
    public void changeDecodeMode(boolean hw) {
        long sp = YfMediaPlayer.getCurrentPosition();
        reset();
        mDecodeMode=hw?YfCloudPlayer.MODE_HARD:YfCloudPlayer.MODE_SOFT;
        YfMediaPlayer.setHardwareDecoder(hw);
        openVideo();
        YfMediaPlayer.seekTo(sp);
    }

    @Override
    public void replay() {
        reset();
        openVideo();
    }

    private void reset() {
        if (p2pMode)
            YfP2PHelper.stopDemandNetBuffer();
        YfMediaPlayer.reset();
    }
    private void stop() {
        if (p2pMode)
            YfP2PHelper.stopDemandNetBuffer();
        YfMediaPlayer.stop();
    }
    @Override
    public void release() {
        if (p2pMode)
            YfP2PHelper.stopDemandNetBuffer();
        YfMediaPlayer.release();
        YfMediaPlayer = null;
    }

    private void startVideoPlayback() {
        Log.v(TAG, "startVideoPlayback");
        if (YfMediaPlayer != null) {
            holder.setFixedSize(mVideoWidth, mVideoHeight);
            YfMediaPlayer.start();
        }

    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        if (singleMode) {
            keepPlayerAlive = true;//保证后续状态不会重置YfMediaPlayer
            YfMediaPlayer.stopRender();//单播放器模式下只停止渲染
        } else {
            release();
        }
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        if ("".equals(path)) {
            path = getIntent().getStringExtra(PLAY_PATH);
        }
        if (YfMediaPlayer == null) {
            openVideo();
        } else {
            YfMediaPlayer.setDisplay(holder);
            YfMediaPlayer.start();
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        if (!keepPlayerAlive && YfMediaPlayer != null) {
            YfMediaPlayer.setDisplay(null);
            YfMediaPlayer.pause();
        }
    }


    @Override
    public void onPause() {
        Log.d(TAG, "demo pause");
        super.onPause();

    }

    int currentOrientation = 0;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (currentOrientation == 0) {
            currentOrientation = getOrientation();
        }
        if (currentOrientation != newConfig.orientation) {
            Log.d(TAG, "开启屏幕切换啦full" + currentOrientation + "____" + newConfig.orientation);
            changeSizeType(true);
            currentOrientation = newConfig.orientation;
        }
        super.onConfigurationChanged(newConfig);
    }

    private MediaController mMediaController;

    private void setMediaController(MediaController controller) {
        if (mMediaController != null) {
            mMediaController.hide();
        }
        mMediaController = controller;
        attachMediaController();
    }

    private void attachMediaController() {
        Log.d(TAG, "attachMediaController:" + mMediaController);
        mMediaController.setMediaPlayer(this);
        View anchorView = mSurface.getParent() instanceof View ?
                (View) mSurface.getParent() : mSurface;
        mMediaController.setAnchorView(anchorView);
        mMediaController.setEnabled(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown:" + keyCode + "isplaying:" + isPlaying());
        boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK &&
                keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                keyCode != KeyEvent.KEYCODE_MENU &&
                keyCode != KeyEvent.KEYCODE_CALL &&
                keyCode != KeyEvent.KEYCODE_ENDCALL;
        if (isKeyCodeSupported && mMediaController != null) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (isPlaying()) {
                    pause();
                    mMediaController.show();
                } else {
                    start();
                    mMediaController.hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                    && isPlaying()) {
                pause();
                mMediaController.show();
            } else if (keyCode != KeyEvent.KEYCODE_HOME) {
                toggleMediaControlsVisiblity();
            }
        }

        return super.onKeyDown(keyCode, event);
    }


    private void toggleMediaControlsVisiblity() {
        if (mMediaController.isShowing()) {
            mMediaController.hide();
        } else {
            mMediaController.show();
        }
    }

    @Override
    public void onBufferingUpdate(YfCloudPlayer mp, int percent) {
        bufferingPercentage = percent;
    }

    @Override
    public boolean onInfo(YfCloudPlayer mp, int what, int extra) {
        if(what==YfCloudPlayer.INFO_CODE_VIDEO_RENDERING_START){
            Log.d(TAG,"开始渲染啦,花费时间："+(SystemClock.elapsedRealtime()-startTime));
        }
        return false;
    }
}
