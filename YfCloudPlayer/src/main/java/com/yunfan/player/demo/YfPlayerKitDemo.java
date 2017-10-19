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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yfcloudplayer.R;
import com.kuaipai.fangyan.core.shooting.jni.RecorderJni;
import com.yunfan.auth.YfAuthentication;
import com.yunfan.encoder.entity.Params;
import com.yunfan.encoder.entity.YfLogo;
import com.yunfan.encoder.interfaces.OnEncoderCallback;
import com.yunfan.encoder.utils.Constants;
import com.yunfan.encoder.utils.RecorderUtil;
import com.yunfan.encoder.widget.YfEncoderKit;
import com.yunfan.encoder.widget.YfEncoderProxy;
import com.yunfan.encoder.widget.YfKitFactory;
import com.yunfan.encoder.widget.YfMuxerProxy;
import com.yunfan.player.MainActivity;
import com.yunfan.player.extra.CustomMediaController;
import com.yunfan.player.extra.LogRecorder;
import com.yunfan.player.extra.Utils;
import com.yunfan.player.extra.YfP2PHelper;
import com.yunfan.player.widget.MediaInfo;
import com.yunfan.player.widget.YfCloudPlayer;
import com.yunfan.player.widget.YfMediaFormat;
import com.yunfan.player.widget.YfMediaMeta;
import com.yunfan.player.widget.YfPlayerKit;
import com.yunfan.player.widget.YfTrackInfo;

import java.util.ArrayList;
import java.util.HashMap;


public class YfPlayerKitDemo extends BasePlayerDemo implements YfCloudPlayer.OnErrorListener, YfCloudPlayer.OnPreparedListener, YfCloudPlayer.OnInfoListener, YfCloudPlayer.OnCompletionListener, YfCloudPlayer.OnBufferingUpdateListener, YfCloudPlayer.OnCaptureResultListener, YfCloudPlayer.OnGenerateGifListener {
    private String TAG = "Yf_VideoViewDemo";
    private String mPath = "";
    private YfPlayerKit mVideoView;
    private LinearLayout cacheInfo_layout;
    private View rootView;
    private LogRecorder logRecorder = new LogRecorder();
    private TextView mVd, mAd, mVb, mAb;
    private Handler getCacheHandler;
    private long bufferSpend = 0;
    private long bufferTimes = -1;
    private long timeSpend = 0;
    private long mBufferTotalTime = 0;
    private String mSaveDir = Environment.getExternalStorageDirectory().getPath() + "/yunfanplayer";
    private ProgressDialog mDialog;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

        setLayoutParams(getLayoutParams(isFulllScreen));

        mVideoView.setHardwareDecoder(currentDecodeMode);//0/1都代表硬解，2代表软解
        configFrameCallback();
//        mVideoView.enableBufferState(true);//可开启/关闭缓冲状态
        mVideoView.setDelayTimeMs(4000, 4000);//有追帧及动态调整缓存大小的缓存模式
//        mVideoView.setSpeed(2f);//改变播放速度
//        mVideoView.setBufferSize(4 * 1024 * 1024);//设置了setMaxDelayTimeMs后setBufferSize会失效；设置缓存大小，不追帧
//        mVideoView.setHTTPTimeOutUs(20000000);//http的超时时间

        mVideoView.setSurfaceCallBack(mSHCallback);
        mVideoView.setVideoLayout(YfPlayerKit.VIDEO_LAYOUT_FILL_PARENT);
        Log.d(TAG, "OnCreateis，FullScreen:" + isFulllScreen);
        mPath = getIntent().getStringExtra(PLAY_PATH);
        if (mPath != null) {
            mCurrentVideoIndex = getIntent().getIntExtra(CURRENT_POSITION, 0);
            openVideo(mPath);
        }
        getCacheHandler = new Handler();
        getCacheHandler.removeCallbacks(updateCacheDisplay);
        getCacheHandler.postDelayed(updateCacheDisplay, 500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void openVideo(String path) {
        Log.d(TAG, "播放地址是:" + path);
        timeSpend = SystemClock.elapsedRealtime();
        if (!TextUtils.isEmpty(path) && mVideoView != null) {
            mVideoView.setOnPreparedListener(this);
            mVideoView.setOnErrorListener(this);
            mVideoView.setOnBufferingUpdateListener(this);
            mVideoView.setOnInfoListener(this);
            mVideoView.setMediaController(YfMediaController);
            mVideoView.setCaptureDir(mSaveDir);
            mVideoView.setonCaptureResultListener(this);
            mVideoView.setOnGenerateGifListener(this);
            if (path.contains("http://") || path.contains("rtmp://")) {
                if (p2pMode) {
                    path = YfP2PHelper.createDirectNetTast(path);
                    startPlayBack(path);
                } else startPlayBack(path);
            } else {
                startPlayBack(path);
            }

        }
        mInitEnd = true;
    }

    private void configFrameCallback() {
        //如果需要回调解码后待渲染的数据，通过以下方法设置；只有软解下可以回调数据
        mVideoView.setOnNativeVideoDecodedListener(!currentDecodeMode ? new YfCloudPlayer.OnNativeVideoDataDecoded() {
            @Override
            public void onVideoDataDecoded(YfCloudPlayer mp, byte[] data, int width, int height, long pts) {
                if (mEnableRecord && mYfEncoder != null) {
                    mYfEncoder.sendVideoData(data, width, height, data.length, pts);
                }
            }
        } : null);
        mVideoView.setOnNativeAudioDecodedListener(!currentDecodeMode ? new YfCloudPlayer.OnNativeAudioDataDecoded() {
            @Override
            public void onAudioDataDecoded(YfCloudPlayer mp, byte[] data, int length, long pts) {
                if (mEnableRecord && mYfEncoder != null) {
                    mYfEncoder.sendAudioData(data, length, pts);
                }
            }
        } : null);
    }

    public int getOrientation() {
        return isFulllScreen ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
    }

    /**
     * 播放下一个视频
     */
    public void next() {
        Log.d(TAG, "mVideoView.getFrameTimestamp:" + mVideoView.getFrameTimestamp());
        mCurrentVideoIndex++;
        if (mCurrentVideoIndex > MainActivity.videoPaths.size() - 1) {
            mCurrentVideoIndex = 0;
        }
        mPath = MainActivity.videoPaths.get(mCurrentVideoIndex);
        openVideo(mPath);
    }

    @Override
    public void release() {
        if (mVideoView != null)
            mVideoView.release(true);
    }

    @Override
    public boolean isPlaying() {
        return mVideoView == null ? false : mVideoView.isPlaying();
    }

    @Override
    public void onWifiConnected() {
        writeLog("wifi连接成功");
        Log.d(TAG, "onWifiConnected");
        if (videoRenderStart)
            openVideo(mPath);
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
            listitem = new ArrayList<>();
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
        listitem.add(createInfoMap("encoder-zq", mediaInfo.getMeta().encoder + ""));
        listitem.add(createInfoMap("publishedtime-zq", mediaInfo.getMeta().publishedtime + ""));
        listitem.add(createInfoMap("currenttime-zq", mediaInfo.getMeta().currenttime + ""));
        listitem.add(createInfoMap("basetime-zq", mediaInfo.getMeta().basetime + ""));
        listitem.add(createInfoMap("publishtime-zq", mediaInfo.getMeta().publishtime + ""));
        listitem.add(createInfoMap("emlogo-zq", mediaInfo.getMeta().emlogo + ""));
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
    public void capture() {
        if (mVideoView != null)
            mVideoView.captureVideo(System.currentTimeMillis() + "");
    }

    @Override
    public void onCaptureResult(final String result) {
        Log.d(TAG, "result: " + result);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (result == null) {
                    Toast.makeText(YfPlayerKitDemo.this, "截图失败！", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(YfPlayerKitDemo.this, "截图已保存在" + result, Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    @Override
    public void changeRatio(int ratio) {
        Log.d(TAG, "切换比例:" + ratio);
        if (mVideoView != null)
            mVideoView.setVideoLayout(ratio);
    }

    @Override
    public void setLayoutParams(FrameLayout.LayoutParams lp) {

    }

    @Override
    public void changeDecodeMode(boolean hw) {
        Log.d(TAG, "切换解码方式:" + hw);
        if (mVideoView != null) {
            mVideoView.setHardwareDecoder(hw);
            configFrameCallback();
            openVideo(mPath);
        }
    }

    @Override
    public void replay() {
        openVideo(mPath);
//        Message message = handler.obtainMessage(1);//测试联系打开视频
//        handler.sendMessageDelayed(message, 1000); //发送message

    }


    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {         // handle message
            switch (msg.what) {//测试连续打开视频
                case 1:
                    //UI操作
                    openVideo(mPath);
                    Message message = handler.obtainMessage(1);
                    handler.sendMessageDelayed(message, 1000);     //发送message , 这样消息就能循环发送
            }
            super.handleMessage(msg);
        }
    };

    private void onPlayError() {
        Log.d(TAG, "wifi状态：" + Utils.isWiFiActive(this) + "————播放状态：" + mVideoView.isPlaying());
        if (mVideoView == null)
            return;
        mVideoView.pause();
        if (!Utils.isWiFiActive(this) && !mVideoView.isPlaying()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("继续播放吗?");
            builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    startPlayBack(mPath);
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
        if (mVideoView == null)
            return;
        startTime = SystemClock.elapsedRealtime();
        mVideoView.enableUDP(mEnableUdp);
        mVideoView.enableHttpDNS(mEnableHttpDns);
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
        mVideoWidth = mp.getVideoWidth();
        mVideoHeight = mp.getVideoHeight();
//        FilterAuthentication.getInstance().authenticate(MainActivity.ACCESS_KEY,MainActivity.TOKEN, new YfAuthenticationInternal.AuthCallBack() {
//            @Override
//            public void onAuthenticateSuccess() {
//                initRecordParams();
//            }
//
//            @Override
//            public void onAuthenticateError(int errorCode) {
//                //do nothing
//            }
//        });

    }


    SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated!!" + holder);
            surfaceCreated = true;
            if (mVideoView != null) {
                if (needToReopenVideo) {
                    needToReopenVideo = false;
                    openVideo(mPath);
                }
                mVideoView.setVolume(1, 1);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "surfaceChanged!!" + holder);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            surfaceCreated = false;
            Log.d(TAG, "surfaceDestroyed!!" + holder);
        }
    };
    boolean surfaceCreated = false;
    boolean needToReopenVideo = false;
    boolean videoRenderStart;

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
                mBufferTotalTime = mBufferTotalTime + bufferSpend;
                writeLog("onInfo_缓冲结束，所花时间为：" + bufferSpend + " 缓冲累计时长：" + mBufferTotalTime);
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
                videoRenderStart = true;
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
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "."
                + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
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
            case YfCloudPlayer.ERROR_CODE_AUTH_FAILED:
                writeLog("鉴权失败，无法播放:" + extra + ":" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
            default:
                writeLog("onError_不懂。。。:" + what + "__" + extra + ":" + (SystemClock.elapsedRealtime() - timeSpend));
                break;
        }
        if (what != YfCloudPlayer.ERROR_CODE_AUTH_FAILED)
            if (surfaceCreated)//只有当surface存在的时候才能打开视频
                openVideo(mPath);//简单粗暴地重连
            else
                needToReopenVideo = true;//设置标志位，在surface初始化后打开视频
        return false;
    }


    private void writeLog(String content) {
//        Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
        logRecorder.writeLog(content);
    }

    @Override
    public void onCompletion(YfCloudPlayer mp) {

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mVideoView != null)
            if (mBackPressed) {
                mVideoView.release(true);
                mVideoView = null;
            } else {
                mVideoView.setVolume(0, 0);//可以使用暂停的方式，也可以使用设置音量为0的方式
//                Log.d(TAG,"mVideoView.pause()");
//                mVideoView.pause();
            }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null) {
            Log.d(TAG, "mVideoView.resume() ");
            mVideoView.setVolume(1, 1);
        }
    }

    Runnable updateCacheDisplay = new Runnable() {
        @Override
        public void run() {
            if (mVideoView == null) {
                getCacheHandler.removeCallbacks(updateCacheDisplay);
                return;
            }
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

    ////////////////////////////////////////////////////////录制模块///////////////////////////////////////////////////////////////////////////////////////////////
    private YfEncoderProxy mYfEncoder;
    private YfMuxerProxy mYfMuxer;
    private boolean mEnableRecord;
    private int mVideoWidth, mVideoHeight;
    private Params mParams = new Params();
    private final static String CACHE_DIRS = Environment.getExternalStorageDirectory().getPath() + "/yunfanencoder";


    protected void initRecordParams() {
        Log.d(TAG, "initRecordParams~~");
        RecorderUtil.initRecordUtil(CACHE_DIRS);//设置保存目录
        RecorderJni.getInstance();//加载so库
        mParams.setFrameRate(24);
        mParams.setLandscape(true);//恒设置为true即可
        mParams.setMode(YfEncoderKit.MODE_VOD, 800);
        mParams.setFrameSize(mVideoWidth, mVideoHeight);
        mParams.setEncoderMode(YfEncoderKit.ENCODER_HW);//推荐使用硬编，软编编码1080p的视频在低端手机会因为性能原因丢帧
        YfMediaMeta meta = mVideoView.getMediaInfo().getMeta();
        if (meta != null && meta.mAudioStream != null) {
            mParams.setAudioSampleRate(meta.mAudioStream.mSampleRate);
            if (meta.mAudioStream.mChannelLayout == YfMediaMeta.AV_CH_LAYOUT_MONO) {
                mParams.setAudioChannel(AudioFormat.CHANNEL_IN_MONO);
                mParams.setAudioChannelCount(1);
            } else if (meta.mAudioStream.mChannelLayout == YfMediaMeta.AV_CH_LAYOUT_STEREO) {
                mParams.setAudioChannel(AudioFormat.CHANNEL_IN_STEREO);
                mParams.setAudioChannelCount(2);
            }
        }
    }

    public void startRecord() {
        if (currentDecodeMode) {
            Toast.makeText(this, "只有软编下可以录制视频", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!YfAuthentication.getInstance().isAuthenticateSucceed()) {
            Toast.makeText(this, "鉴权失败，请检查token", Toast.LENGTH_SHORT).show();
            return;
        }
        mYfEncoder = initEncoder();
        mYfMuxer = initMuxer(this, mVideoWidth, mVideoHeight, 800, 10, 24, 1, 8, "PlayerFrameRecorder");
        if (mYfMuxer != null && mYfEncoder != null) {
            mYfMuxer.startMuxer();
            Bitmap logo = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            mYfEncoder.addLogo(new YfLogo(mVideoWidth - logo.getWidth(), 0, logo));
            mYfEncoder.startEncoder(mParams);
            mEnableRecord = true;
        } else {
            Toast.makeText(this, "鉴权失败，请检查Application ID", Toast.LENGTH_SHORT).show();
            return;
        }
    }


    public void stopRecord() {
        if (currentDecodeMode) {
            return;
        }
        mEnableRecord = false;
        if (mYfEncoder != null)
            mYfEncoder.stopEncoder();
        if (mYfMuxer != null)
            mYfMuxer.stopMuxer();

    }

    @Override
    public void startRecordGif() {
        mVideoView.startRecordGif(200, 80, 80, System.currentTimeMillis() + "", 2);
    }

    @Override
    public void stopRecordGif() {
        mVideoView.stopRecordGif();
        if (mDialog == null) mDialog = new ProgressDialog(this);
        mDialog.setMessage("正在生成gif");
        mDialog.setCancelable(false);
        mDialog.show();
    }


    protected YfEncoderProxy initEncoder() {
        if (mYfEncoder == null)
            try {
                mYfEncoder = new YfKitFactory.Factory(this).buildEncoderManager(new OnEncoderCallback() {
                    @Override
                    public void recycleAudioData(byte[] data) {

                    }

                    @Override
                    public void recycleSecondAudioData(byte[] data) {

                    }

                    @Override
                    public void recycleVideoData(byte[] data) {

                    }

                    @Override
                    public boolean onAudioEncode(int flag, int length, byte[] data, long pts, long dts, int linkAudio) {
                        if (mYfMuxer != null) {
                            mYfMuxer.sendAudioData(flag, length, pts, dts, data, linkAudio);
                        }
                        return false;
                    }

                    @Override
                    public boolean onVideoEncode(int flag, int length, byte[] data, long pts, long dts) {
                        if (mYfMuxer != null) {
                            mYfMuxer.sendVideoData(flag, length, pts, dts, data);
                        }
                        return false;
                    }

                    @Override
                    public void onVideoSizeChange(int width, int height) {

                    }

                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        return mYfEncoder;
    }

    protected YfMuxerProxy initMuxer(Context context, int wid, int hei, int bit, int iFrame, int frameRate,
                                     int bufferSec, int netReportInterval,
                                     String liveUrl) {
        if (mYfMuxer == null)
            try {
                mYfMuxer = new YfKitFactory.Factory(context).buildMuxManager(context, false, wid, hei, bit, iFrame, frameRate, bufferSec, netReportInterval,
                        false, liveUrl, false, Constants.STREAM_TYPE_NORMAL, null);
            } catch (Exception e) {
                e.printStackTrace();
            }

        return mYfMuxer;

    }

    @Override
    public void onGenerateGifSuccess(String path) {
        if (mDialog != null) mDialog.dismiss();
        Toast.makeText(YfPlayerKitDemo.this, "gif保存在" + path, Toast.LENGTH_LONG).show();

    }

    @Override
    public void onGenerateGifFail(String errorMessage) {
        if (mDialog != null) mDialog.dismiss();
        Toast.makeText(YfPlayerKitDemo.this, "gif保存失败：" + errorMessage, Toast.LENGTH_LONG).show();
    }
}
