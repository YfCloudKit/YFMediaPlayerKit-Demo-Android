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


import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;

import com.example.yfcloudplayer.R;
import com.yunfan.player.MainActivity;
import com.yunfan.player.extra.CustomMediaController;
import com.yunfan.player.widget.MediaInfo;
import com.yunfan.player.widget.YfCloudPlayer;
import com.yunfan.player.widget.YfMediaFormat;
import com.yunfan.player.widget.YfSurfaceContainer;
import com.yunfan.player.widget.YfTrackInfo;
import com.yunfan.player.widget.YfVRLibrary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


public class VRYfCloudPlayerDemo extends BasePlayerDemo implements MediaController.MediaPlayerControl, YfCloudPlayer.OnBufferingUpdateListener, YfCloudPlayer.OnInfoListener {
    private String TAG = "Yf_MediaPlayer";

    public static final String PLAY_PATH = "play_path";
    public static final String CURRENT_POSITION = "current_position";
    public YfCloudPlayer YfMediaPlayer;
    private GLSurfaceView mSurface1, mSurface2;
    private Surface mSurface;
    private YfSurfaceContainer mTouchFix;
    private int mDecodeMode = MainActivity.DECODE_MODE > 1 ? YfCloudPlayer.MODE_SOFT : YfCloudPlayer.MODE_HARD;
    private YfVRLibrary mYfVRLibrary;
    private String path = "";
    private View rootView;
    private boolean mIsVideoReadyToBePlayed,isGyroEnable=true;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        rootView = LayoutInflater.from(this).inflate(
                R.layout.activity_yfcloudplayer_vr, null);
        setContentView(rootView);
        mSurface1 = (GLSurfaceView) findViewById(R.id.glview1);
        mSurface2 = (GLSurfaceView) findViewById(R.id.glview2);
        mTouchFix = (YfSurfaceContainer) findViewById(R.id.touchFix);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (YfMediaPlayer != null && mMediaController != null) {
                    Log.d(TAG,"toggle controller");
                    toggleMediaControlsVisiblity();
                }
                return false;
            }
        });
        mTouchFix.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                return mYfVRLibrary.handleTouchEvent(event);
            }
        });
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        YfMediaController = new CustomMediaController(this);
        YfMediaController.setSupportActionBar(actionBar);
        setMediaController(YfMediaController);
        mYfVRLibrary = createVRLibrary();

        Log.d(TAG, "onCreate，fullScreen:" + isFulllScreen);
        mCurrentVideoIndex = getIntent().getIntExtra(CURRENT_POSITION, 0);

//        Toast.makeText(YfCloudPlayerDemo.this, "解码方式为:" + MainActivity.DecodeModes[MainActivity.DECODE_MODE], Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mYfVRLibrary.onResume(this);
    }

    private YfVRLibrary createVRLibrary() {
        return YfVRLibrary.with(this)
                .displayMode(YfVRLibrary.DISPLAY_MODE_NORMAL)
                .interactiveMode(YfVRLibrary.INTERACTIVE_MODE_MOTION)
                .video(new YfVRLibrary.IOnSurfaceReadyCallback() {
                    @Override
                    public void onSurfaceReady(Surface surface) {
                        Log.d(TAG, "surface create:" + surface);
                        mSurface = surface;
                        openVideo(surface);

                    }
                })
                .ifNotSupport(new YfVRLibrary.INotSupportCallback() {
                    @Override
                    public void onNotSupport(int mode) {
//                        String tip = mode == YfVRLibrary.INTERACTIVE_MODE_MOTION
//                                ? "onNotSupport:MOTION" : "onNotSupport:" + String.valueOf(mode);
//                        Toast.makeText(VideoPlayerActivity.this, tip, Toast.LENGTH_SHORT).show();
                    }
                })
                .build(R.id.glview1, R.id.glview2);
    }

    private static final int CHANGE_DISPLAY_MODE=12451;
    private static final int CHANGE_INTERACTIVE_MODE=12323;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.addSubMenu(Menu.NONE,CHANGE_DISPLAY_MODE,Menu.NONE,"全景/3D");
        menu.addSubMenu(Menu.NONE,CHANGE_INTERACTIVE_MODE,Menu.NONE,"TOUCH/MOTION");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case CHANGE_DISPLAY_MODE:
                mYfVRLibrary.switchDisplayMode(this);
                break;
            case CHANGE_INTERACTIVE_MODE:
                isGyroEnable=!isGyroEnable;
                mYfVRLibrary.switchInteractiveMode(this);
                if(!isGyroEnable){
                    mMediaController.show(Integer.MAX_VALUE);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    private YfCloudPlayer.OnPreparedListener mPreparedListener;
    private YfCloudPlayer.OnCompletionListener mCompletionListener;
    private long startTime = 0;

    public void openVideo(Surface surface) {
        if ("".equals(path))
            path = getIntent().getStringExtra(PLAY_PATH);
        Log.d(TAG, "播放地址是:" + path);
        if (YfMediaPlayer == null)
            YfMediaPlayer = YfCloudPlayer.Factory.createPlayer(this, mDecodeMode);
        if (!TextUtils.isEmpty(path)) {
            initPlayListener();
            mIsVideoReadyToBePlayed = false;
            YfMediaPlayer.setHardwareDecoder(mDecodeMode == 0 ? true : false);//reset之后解码方式会还原默认的硬解，所以在这里要再手动设置一次解码方式比较好
            YfMediaPlayer.setBufferSize(4 * 1024 * 1024);
//            YfMediaPlayer.setHTTPTimeOutUs(10000000);
            try {
                startTime = SystemClock.elapsedRealtime();
                YfMediaPlayer.setDataSource(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "进来设置display");
            YfMediaPlayer.setSurface(surface);
            YfMediaPlayer.prepareAsync();
            YfMediaPlayer.setOnCompletionListener(mCompletionListener);
            YfMediaPlayer.setOnPreparedListener(mPreparedListener);
            YfMediaPlayer.setOnInfoListener(this);
            YfMediaPlayer.setOnBufferingUpdateListener(this);
        }
        mInitEnd = true;
    }

    public void initPlayListener() {
        mPreparedListener = new YfCloudPlayer.OnPreparedListener() {
            public void onPrepared(YfCloudPlayer mp) {
                mIsVideoReadyToBePlayed = true;
                if (mIsVideoReadyToBePlayed) {
                    startVideoPlayback();
                }
            }
        };
        mCompletionListener =
                new YfCloudPlayer.OnCompletionListener() {
                    public void onCompletion(YfCloudPlayer mp) {
                        next();
                    }
                };
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
        openVideo(mSurface);
    }

    @Override
    public void start() {
        startVideoPlayback();
    }

    @Override
    public void pause() {
        if (YfMediaPlayer != null)
            YfMediaPlayer.pause();
        mYfVRLibrary.onPause(this);
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

    private int bufferingPercentage = 0;

    @Override
    public int getBufferPercentage() {
        return bufferingPercentage;
    }


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

    protected boolean enableGravityListener() {
        return false;
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
    }



    @Override
    public void changeDecodeMode(boolean hw) {
        long sp = YfMediaPlayer.getCurrentPosition();
        reset();
        mDecodeMode = hw ? YfCloudPlayer.MODE_HARD : YfCloudPlayer.MODE_SOFT;
        YfMediaPlayer.setHardwareDecoder(hw);
        openVideo(mSurface);
        YfMediaPlayer.seekTo(sp);
    }

    @Override
    public void replay() {
        reset();
        openVideo(mSurface);
    }

    private void reset() {
        YfMediaPlayer.reset();
    }

    private void stop() {
        YfMediaPlayer.stop();
    }

    @Override
    public void release() {
        if (YfMediaPlayer != null) {
            YfMediaPlayer.release();
            YfMediaPlayer = null;
        }

    }

    private void startVideoPlayback() {
        Log.v(TAG, "startVideoPlayback");
        if (YfMediaPlayer != null) {
            YfMediaPlayer.start();
        }

    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        release();
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mYfVRLibrary.onDestroy();
    }



    @Override
    public void onPause() {
        Log.d(TAG, "demo pause");
        super.onPause();

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
        View anchorView = mTouchFix.getParent() instanceof View ?
                (View) mTouchFix.getParent() : mTouchFix;
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
        if (what == YfCloudPlayer.INFO_CODE_VIDEO_RENDERING_START) {
            Log.d(TAG, "开始渲染啦,花费时间：" + (SystemClock.elapsedRealtime() - startTime));
        }
        return false;
    }
}
