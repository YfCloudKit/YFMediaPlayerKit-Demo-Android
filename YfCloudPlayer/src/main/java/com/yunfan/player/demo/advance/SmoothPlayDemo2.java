package com.yunfan.player.demo.advance;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;

import com.example.yfcloudplayer.R;
import com.yunfan.player.MainActivity;
import com.yunfan.player.demo.YfCloudPlayerDemo;
import com.yunfan.player.extra.CustomMediaController;
import com.yunfan.player.widget.YfCloudPlayer;

import java.io.IOException;

/**
 * Created by xjx on 2016/6/7.
 */
public class SmoothPlayDemo2 extends Activity implements MediaController.MediaPlayerControl {
    private static final String TAG = "SmoothPlayDemo";
    private YfCloudPlayer[] yfPlayer = new YfCloudPlayer[2];//两个播放器循环复用
    private PlayerManager[] playerManager = new PlayerManager[2];//播放器封装，简化代码
    private SurfaceView surfaceView;
    private String[] path = new String[2];
    public static final String PLAY_PATH = "play_path";
    public static final String CURRENT_POSITION = "current_position";
    private static final int STOP_REST_DURATION_COUNTER = 1008610010;
    private Handler requestRestDuration;
    private int PREPARE_TIME = 10000;//开始准备下一个播放器的时间
    private int currentPlayerIndex = 0;
    private int mCurrentVideoIndex = 0;
    private CustomMediaController yfMediaController;
    private boolean lockSurfaceCallback = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yfcloudplayer);
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        for (int i = 0; i < yfPlayer.length; i++) {
            yfPlayer[i] = YfCloudPlayer.Factory.createPlayer(this, YfCloudPlayer.MODE_HARD);//以硬解模式初始化
            yfPlayer[i].setBufferSize(8 * 1024 * 1024);
        }
        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mMediaController != null) {
                    toggleMediaControlsVisiblity();
                }
                return false;
            }
        });
        path[0] = getIntent().getStringExtra(PLAY_PATH);
        mCurrentVideoIndex = getIntent().getIntExtra(CURRENT_POSITION, 0);
        playerManager[0] = new PlayerManager(yfPlayer[0], path[0]);
        playerManager[0].setSurface(surfaceView);//此时会自动开始播放

        requestRestDuration = new Handler();
        requestRestDuration.removeCallbacks(updateRestDuration);
        requestRestDuration.postDelayed(updateRestDuration, PREPARE_TIME / 5);//每隔2s请求一次
        yfMediaController = new CustomMediaController(this);
        setMediaController(yfMediaController);
    }

    Runnable updateRestDuration = new Runnable() {
        @Override
        public void run() {
            requestRestDuration.removeCallbacks(updateRestDuration);
            long restTime = playerManager[currentPlayerIndex].getRestTime();
            Log.d(TAG, "restTime:" + restTime);
            if (restTime == STOP_REST_DURATION_COUNTER) {
                return;//直播流，退出计时
            }
            if (restTime <= PREPARE_TIME) {
                Log.d(TAG, "开始准备下一个播放器");
                path[1 - currentPlayerIndex] = getNextPath();
                playerManager[1 - currentPlayerIndex] = new PlayerManager(yfPlayer[1 - currentPlayerIndex], path[1 - currentPlayerIndex]);
                playerManager[currentPlayerIndex].setNextPlayer(yfPlayer[1 - currentPlayerIndex], path[1 - currentPlayerIndex], surfaceViewController);
            } else {
                requestRestDuration.postDelayed(updateRestDuration, PREPARE_TIME / 5);
            }

        }
    };

    YfCloudPlayer.SurfaceViewController surfaceViewController = new YfCloudPlayer.SurfaceViewController() {
        @Override
        public void dettachSurfaceForcedly() {
            //最简单的彻底重置surface的方法
            lockSurfaceCallback = true;//会触发surfacedestroy和create，把相关的回调禁止掉
            surfaceView.setVisibility(View.GONE);
            surfaceView.setVisibility(View.VISIBLE);
            lockSurfaceCallback = false;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        requestRestDuration.removeCallbacks(updateRestDuration);
    }

    public String getNextPath() {
        mCurrentVideoIndex++;
        if (mCurrentVideoIndex > MainActivity.videoPaths.size() - 1) {
            mCurrentVideoIndex = 0;
        }
        return MainActivity.videoPaths.get(mCurrentVideoIndex);
    }

    @Override
    public void start() {
        yfPlayer[currentPlayerIndex].start();
    }

    @Override
    public void pause() {
        yfPlayer[currentPlayerIndex].pause();
    }

    @Override
    public int getDuration() {
        return (int) yfPlayer[currentPlayerIndex].getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return (int) yfPlayer[currentPlayerIndex].getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        yfPlayer[currentPlayerIndex].seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return yfPlayer[currentPlayerIndex].isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
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

    /**
     * 简化版的YfCloudPlayer调用逻辑，更具体使用参考{@link YfCloudPlayerDemo}
     */
    class PlayerManager {
        private YfCloudPlayer inerPlayer;
        private String inerPath;
        private SurfaceView inerSurfaceView;
        private long duration = -1;
        private boolean isPrepared = false;

        public PlayerManager(YfCloudPlayer tempPlayer, String path) {
            inerPlayer = tempPlayer;
            inerPath = path;
        }

        public void setNextPlayer(YfCloudPlayer nextPlayer, String path, YfCloudPlayer.SurfaceViewController controller) {
            inerPlayer.setNextPlayer(nextPlayer, path, controller);
        }

//        public boolean isInit() {
//            if (inerPlayer != null && inerPath != null) {
//                return true;
//            }
//            return false;
//        }

        public void setSurface(SurfaceView surfaceView) {
            Log.d(TAG, "set surface!!!" + surfaceView + ",holder:" + surfaceView.getHolder() + ",player:" + inerPlayer);
            inerSurfaceView = surfaceView;
            surfaceView.getHolder().addCallback(surfaceHolder);
            inerPlayer.setDisplay(surfaceView.getHolder());
        }


        public void preparePlayer() {
            Log.d(TAG, "prepare  player!");
            setListener();
            try {
                inerPlayer.setDataSource(inerPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            inerPlayer.setDisplay(inerSurfaceView.getHolder());
            inerPlayer.prepareAsync();
        }


        private void setListener() {
            YfCloudPlayer.OnPreparedListener mPreparedListener = new YfCloudPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(YfCloudPlayer mp) {
                    isPrepared = true;
                    startVideoPlayback();
                }
            };
            inerPlayer.setOnPreparedListener(mPreparedListener);
        }


        private void releasePlayer() {
            Log.d(TAG, "releasePlayer");
            inerPlayer.reset();
        }

        private void startVideoPlayback() {
            Log.d(TAG, "startVideoPlayback:" + inerPlayer);
            if (inerSurfaceView != null) {
                if (isPrepared) {
                    Log.d(TAG, "正式播放~");
                    inerPlayer.start();
                    duration = inerPlayer.getDuration();
                } else {
                    Toast.makeText(SmoothPlayDemo2.this, "播放失败", Toast.LENGTH_SHORT).show();
                }

            }
        }

        SurfaceHolder.Callback surfaceHolder = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (inerPlayer != null && !lockSurfaceCallback)
                    preparePlayer();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (inerPlayer != null && !lockSurfaceCallback)
                    releasePlayer();
            }
        };

        /**
         * 获取剩余时间，需保证在收到prepared之后获取
         *
         * @return
         */
        public long getRestTime() {
            if (duration == 0) {//0代表直播流
                return STOP_REST_DURATION_COUNTER;
            }
            return duration - inerPlayer.getCurrentPosition();
        }

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
        View anchorView = surfaceView.getParent() instanceof View ?
                (View) surfaceView.getParent() : surfaceView;
        mMediaController.setAnchorView(anchorView);
        mMediaController.setEnabled(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK &&
                keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                keyCode != KeyEvent.KEYCODE_MENU &&
                keyCode != KeyEvent.KEYCODE_CALL &&
                keyCode != KeyEvent.KEYCODE_ENDCALL;
        if (isKeyCodeSupported && mMediaController != null) {
            if (keyCode != KeyEvent.KEYCODE_HOME) {
                toggleMediaControlsVisiblity();
            }
        }

        return super.onKeyDown(keyCode, event);
    }


    private void toggleMediaControlsVisiblity() {
        if (mMediaController.isShowing()) {
            mMediaController.hide();
        } else {
            mMediaController.show(10000);
        }
    }
}
