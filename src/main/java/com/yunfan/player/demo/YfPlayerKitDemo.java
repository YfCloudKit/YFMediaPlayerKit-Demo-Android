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
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yfcloudplayer.R;
import com.yunfan.player.MainActivity;
import com.yunfan.player.extra.CustomMediaController;
import com.yunfan.player.extra.LogRecorder;
import com.yunfan.player.extra.Utils;
import com.yunfan.player.extra.YfP2PHelper;
import com.yunfan.player.widget.MediaInfo;
import com.yunfan.player.widget.YfCloudPlayer;
import com.yunfan.player.widget.YfMediaFormat;
import com.yunfan.player.widget.YfPlayerKit;
import com.yunfan.player.widget.YfTrackInfo;

import java.util.ArrayList;
import java.util.HashMap;


public class YfPlayerKitDemo extends BasePlayerDemo implements YfCloudPlayer.OnErrorListener, YfCloudPlayer.OnPreparedListener, YfCloudPlayer.OnInfoListener, YfCloudPlayer.OnCompletionListener, YfCloudPlayer.OnBufferingUpdateListener {
    private String TAG = "YfVIDEOVIEW_VideoViewDemo";
    private String path = "";
    private YfPlayerKit mVideoView;
    private FrameLayout mVideoView_layout;
    private LinearLayout cacheInfo_layout;
    private View rootView;
    private LogRecorder logRecorder = new LogRecorder();
    private TextView mVd, mAd, mVb, mAb;
    private Handler getCacheHandler;
    private long bufferSpend = 0;
    private long bufferTimes = -1;
    private long timeSpend = 0;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        timeSpend = SystemClock.elapsedRealtime();
        rootView = LayoutInflater.from(this).inflate(
                R.layout.activity_yfplayerkit, null);
        setContentView(rootView);
        mVideoView = (YfPlayerKit) findViewById(R.id.surface_view);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        YfMediaController = new CustomMediaController(this);
        YfMediaController.setSupportActionBar(actionBar);

        cacheInfo_layout = (LinearLayout) findViewById(R.id.cache_info_layout);
        mVb = (TextView) findViewById(R.id.video_buffer);
        mAb = (TextView) findViewById(R.id.audio_buffer);
        mVd = (TextView) findViewById(R.id.video_frame_duaration);
        mAd = (TextView) findViewById(R.id.audio_frame_duaration);

        mVideoView_layout = (FrameLayout) findViewById(R.id.videoView_layout);
        setLayoutParams(getLayoutParams(isFulllScreen));
        mVideoView.setHardwareDecoder(currentDecodeMode);//0/1都代表硬解，2代表软解
//        mVideoView.setDelayTimeMs(800,1500);
        mVideoView.setBufferSize(4 * 1024 * 1024);//设置了setMaxDelayTimeMs后setBufferSize会失效
        mVideoView.enableVideoSmoothing(true);
//        mVideoView.setHTTPTimeOutUs(5000000);
        mVideoView.setSurfaceCallBack(mSHCallback);
        mVideoView.setVideoLayout(YfPlayerKit.VIDEO_LAYOUT_FIT_PARENT);
        Log.d(TAG, "OnCreateis，FullScreen:" + isFulllScreen);
        path = getIntent().getStringExtra(PLAY_PATH);
        if (path != null) {
            mCurrentVideoIndex = getIntent().getIntExtra(CURRENT_POSITION, 0);
            openVideo();
        }
        getCacheHandler = new Handler();
        getCacheHandler.removeCallbacks(updateCacheDisplay);
        getCacheHandler.postDelayed(updateCacheDisplay, 500);
    }


    public void openVideo() {
        Log.d(TAG, "播放地址是:" + path);
        if (!TextUtils.isEmpty(path)) {
            if (p2pMode)
                path = YfP2PHelper.createDirectNetTast(path);
            mVideoView.setOnPreparedListener(this);
            mVideoView.setOnErrorListener(this);
            mVideoView.setOnBufferingUpdateListener(this);
            mVideoView.setOnInfoListener(this);
            mVideoView.setMediaController(YfMediaController);
            startPlayBack(path);
        }
        mInitEnd = true;
    }


    public int getOrientation() {
        return isFulllScreen ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
    }

    /**
     * 播放下一个视频
     */
    public void next() {
        mCurrentVideoIndex++;
        if (mCurrentVideoIndex > MainActivity.videoPaths.size() - 1) {
            mCurrentVideoIndex = 0;
        }
        path = MainActivity.videoPaths.get(mCurrentVideoIndex);
        startPlayBack(path);
    }

    @Override
    public void release() {
        mVideoView.release(true);
    }

    @Override
    public boolean isPlaying() {
        return mVideoView.isPlaying();
    }

    @Override
    public void onWifiConnected() {
        writeLog("wifi连接成功");
    }

    @Override
    public void showLog() {
        Log.d(TAG, "查看消息");
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Infos").setItems(
                logRecorder.getLogs(), null).setPositiveButton("清空", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                logRecorder.clearLogs();
                dialog.dismiss();
            }
        }).setNegativeButton(
                "关闭", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();
    }

    ArrayList<HashMap<String, Object>> listitem;
    SimpleAdapter adapter;

    @Override
    public void showInfo() {
        Log.d(TAG, "show infos!mVideoView:" + mVideoView + "isPlaying:" + mVideoView.isPlaying());
        if (mVideoView == null || !mVideoView.isPlaying()) {
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
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        ListView lv = (ListView) contentView.findViewById(R.id.list);
        MediaInfo mediaInfo = mVideoView.getMediaInfo();
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
        Log.d(TAG, "切换比例:" + ratio);
        mVideoView.setVideoLayout(ratio);
    }

    @Override
    public void setLayoutParams(FrameLayout.LayoutParams lp) {

    }

    @Override
    public void changeDecodeMode(boolean hw) {
        Log.d(TAG, "切换解码方式:" + hw);
        mVideoView.setHardwareDecoder(hw);
        startPlayBack(path);
    }

    @Override
    public void replay() {
        openVideo();
    }


    private void onPlayError() {
        Log.d(TAG, "wifi状态：" + Utils.isWiFiActive(this) + "————播放状态：" + mVideoView.isPlaying());
        mVideoView.pause();
        if (!Utils.isWiFiActive(this) && !mVideoView.isPlaying()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("继续播放吗?");
            builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    startPlayBack(path);
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

    private long startTime;

    private void startPlayBack(String path) {
        startTime = SystemClock.elapsedRealtime();
        mVideoView.setVideoPath(path);
        mVideoView.start();
    }

    boolean mBackPressed;

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        getCacheHandler.removeCallbacks(updateCacheDisplay);
        setResult(RESULT_OK);
        mBackPressed = true;

        Log.d(TAG, "这个activity已经onDestroy");
        if (p2pMode)
            YfP2PHelper.stopDirectNetBuffer();
        finish();
    }

    @Override
    public void onPrepared(YfCloudPlayer mp) {
        mVideoView.setRotation(90);
    }

    SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated!!" + holder);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "surfaceChanged!!" + holder);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed!!" + holder);
        }
    };

    @Override
    public boolean onInfo(YfCloudPlayer mp, int what, int extra) {
        Log.d(TAG, "onInfo from YfPlayerKitDemo:" + what + "__" + extra);
        switch (what) {
            case YfPlayerKit.INFO_CODE_BUFFERING_START:
                bufferSpend = SystemClock.elapsedRealtime();
                bufferTimes++;
                writeLog("onInfo_开始" + (bufferTimes != 0 ? ("第" + bufferTimes + "次") : "") + "缓冲...");
                break;
            case YfPlayerKit.INFO_CODE_BUFFERING_END:
                bufferSpend = SystemClock.elapsedRealtime() - bufferSpend;
                writeLog("onInfo_缓冲结束，所花时间为：" + bufferSpend);
                break;
            case YfPlayerKit.INFO_CODE_AUDIO_RENDERING_START:
                writeLog("onInfo_音频开始渲染：" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
            case YfPlayerKit.INFO_CODE_UNKNOWN:
                writeLog("onInfo_UNKNOWN：" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
            case YfPlayerKit.INFO_CODE_STARTED_AS_NEXT:
                writeLog("onInfo_：start as next" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
            case YfPlayerKit.INFO_CODE_VIDEO_RENDERING_START:
                Log.d(TAG, "打开完成，花费时间为：" + (SystemClock.elapsedRealtime() - startTime));
                writeLog("onInfo_视频开始渲染：" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
            case YfPlayerKit.INFO_CODE_VIDEO_TRACK_LAGGING:
                writeLog("onInfo_视频卡顿：" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
            case YfPlayerKit.INFO_CODE_NETWORK_BANDWIDTH:
                writeLog("onInfo_网络带宽：" + what + "___" + extra + "__" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
            case YfPlayerKit.INFO_CODE_BAD_INTERLEAVING:
                writeLog("onInfo_交错异常：" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
            case YfPlayerKit.INFO_CODE_NOT_SEEKABLE:
                writeLog("onInfo_无法seek：" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
            case YfPlayerKit.INFO_CODE_METADATA_UPDATE:
                writeLog("onInfo_mediadata更新：" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
            case YfPlayerKit.INFO_CODE_VIDEO_ROTATION_CHANGED:
                writeLog("onInfo_video rotation changed：" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
            case YfPlayerKit.INFO_CODE_REAL_IP:
//                IPAddress ipAddress = new IPAddress(BitConverter.GetBytes(intAddress)).ToString();
                writeLog("onInfo_真实IP地址回调：" + intToIp(extra));

                break;
            default:
                writeLog("onError_不懂。。。:" + what + "__" + extra + ":" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
        }
        return false;
    }

    public static String intToIp(int i) {
        return ((i >> 24) & 0xFF) + "." + ((i >> 16) & 0xFF) + "."
                + ((i >> 8) & 0xFF) + "." + (i & 0xFF);
    }

    @Override
    public boolean onError(YfCloudPlayer mp, int what, int extra) {
        Log.d(TAG, "onError from YfPlayerKitDemo:" + what + "__" + extra);
        switch (what) {
            case YfCloudPlayer.ERROR_CODE_UNKNOWN:
                writeLog("onError_unknown:" + extra + ":" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
            case YfCloudPlayer.ERROR_CODE_SERVER_DIED:
                writeLog("onError_服务器已挂:" + extra + ":" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
            case YfCloudPlayer.ERROR_CODE_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                writeLog("onError_播放异常:" + extra + ":" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
            case YfCloudPlayer.ERROR_CODE_IO:
                writeLog("onError_IO异常:" + extra + ":" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
            case YfCloudPlayer.ERROR_CODE_MALFORMED:
                writeLog("onError_格式异常:" + extra + ":" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
            case YfCloudPlayer.ERROR_CODE_UNSUPPORTED:
                writeLog("onError_支持有误:" + extra + ":" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
            case YfCloudPlayer.ERROR_CODE_TIMED_OUT:
                writeLog("onError_超时:" + extra + ":" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
            default:
                writeLog("onError_不懂。。。:" + what + "__" + extra + ":" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
        }
        return false;
    }

    private void writeLog(String content) {
        Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
        logRecorder.writeLog(content);
    }

    @Override
    public void onCompletion(YfCloudPlayer mp) {

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBackPressed) {
            if (mVideoView != null) {
                mVideoView.release(true);
                mVideoView = null;
            }
        } else {
            mVideoView.pause();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null)
            mVideoView.start();
    }

    Runnable updateCacheDisplay = new Runnable() {
        @Override
        public void run() {
            mVd.setText("v-sec:" + mVideoView.getVideoCachedDuration());
            mAd.setText("A-sec:" + mVideoView.getAudioCachedDuration());
            mVb.setText("v-buf:" + mVideoView.getVideoCachedBytes());
            mAb.setText("A-buf:" + mVideoView.getAudioCachedBytes());
            getCacheHandler.removeCallbacks(updateCacheDisplay);
            getCacheHandler.postDelayed(updateCacheDisplay, 500);
        }
    };

    @Override
    public void onBufferingUpdate(YfCloudPlayer mp, int percent) {

    }
}
