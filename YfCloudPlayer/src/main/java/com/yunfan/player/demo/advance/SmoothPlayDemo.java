package com.yunfan.player.demo.advance;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;

import com.example.yfcloudplayer.R;
import com.yunfan.player.MainActivity;
import com.yunfan.player.demo.YfCloudPlayerDemo;
import com.yunfan.player.extra.CustomMediaController;
import com.yunfan.player.extra.IRenderView;
import com.yunfan.player.extra.TextureRenderView;
import com.yunfan.player.widget.YfCloudPlayer;

import java.io.IOException;

/**
 * Created by xjx on 2016/6/7.
 */
public class SmoothPlayDemo extends Activity implements MediaController.MediaPlayerControl {
    private static final String TAG = "SmoothPlayDemo";
    private YfCloudPlayer[] yfPlayer = new YfCloudPlayer[2];//两个播放器循环复用
    private PlayerManager[] playerManager = new PlayerManager[2];//播放器封装，简化代码
    private TextureRenderView surfaceView;
    private String[] path = new String[2];
    public static final String PLAY_PATH = "play_path";
    public static final String CURRENT_POSITION = "current_position";
    private static final int STOP_REST_DURATION_COUNTER = 1008610010;
    private Handler requestRestDuration;
    private int PREPARE_TIME = 10000;//开始准备下一个播放器的时间
    private int currentPlayerIndex = 0;
    private int mCurrentVideoIndex = 0;
    private CustomMediaController yfMediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yfcloudplayer);
        surfaceView = (TextureRenderView) findViewById(R.id.render_view);
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
            if (restTime != -1 && restTime <= PREPARE_TIME) {
                Log.d(TAG, "开始准备下一个播放器");
                path[1 - currentPlayerIndex] = getNextPath();
                playerManager[1 - currentPlayerIndex] = new PlayerManager(yfPlayer[1 - currentPlayerIndex], path[1 - currentPlayerIndex]);
                playerManager[1 - currentPlayerIndex].prepareAsNextPlayer();
            } else {
                requestRestDuration.postDelayed(updateRestDuration, PREPARE_TIME / 5);
            }
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

    /**
     * 无缝播放下一个视频
     */
    public void next() {
        Log.d(TAG, "播放下一个视频");
        currentPlayerIndex = 1 - currentPlayerIndex;
        if (playerManager[currentPlayerIndex] == null || !playerManager[currentPlayerIndex].isInit()) {
            Log.d(TAG, "没做好准备 重新跑~");
            path[currentPlayerIndex] = getNextPath();
            playerManager[currentPlayerIndex] = new PlayerManager(yfPlayer[currentPlayerIndex], path[currentPlayerIndex]);
            playerManager[currentPlayerIndex].setSurface(surfaceView);
            playerManager[currentPlayerIndex].preparePlayer();
        } else {
            playerManager[currentPlayerIndex].setSurface(surfaceView);//正式播放前才设置surface
            playerManager[currentPlayerIndex].startVideoPlayback();//开始播放
            requestRestDuration.removeCallbacks(updateRestDuration);
            requestRestDuration.postDelayed(updateRestDuration, PREPARE_TIME / 5);//每隔2s请求一次
        }
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
        private TextureRenderView inerSurfaceView;
        private long duration = -1;
        private boolean isPrepared = false;
        private boolean preparing = false;
        private boolean playAsNextPlayer = false;

        public PlayerManager(YfCloudPlayer tempPlayer, String path) {
            inerPlayer = tempPlayer;
            inerPath = path;
        }

        public boolean isInit() {
            if (inerPlayer != null && inerPath != null) {
                return true;
            }
            return false;
        }

        public void setSurface(TextureRenderView surfaceView) {
            Log.d(TAG, "set surface!!!" + surfaceView + ",holder:" + surfaceView.getSurfaceHolder() + ",player:" + inerPlayer);
            inerSurfaceView = surfaceView;
            inerSurfaceView.getSurfaceHolder().bindToMediaPlayer(inerPlayer);
            inerSurfaceView.addRenderCallback(callback);

        }

        public void prepareAsNextPlayer() {
            //和一般的prepare不一样的在于，display设置为null，在setDataSource之前调用setBuffingOnPrepared
            Log.d(TAG, "prepare as next player!" + inerPlayer);
            playAsNextPlayer = true;
            setListener();//设置回调
            inerPlayer.setDisplay(null);
            try {
                inerPlayer.setBuffingOnPrepared();
                inerPlayer.setDataSource(inerPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            inerPlayer.prepareAsync();//记得在收到onprepared()回调的时候判断一下，不要立即start()
        }

        public void preparePlayer() {
            preparing = true;
            Log.d(TAG, "prepare  player!");
            setListener();
            try {
                inerPlayer.setDataSource(inerPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            inerSurfaceView.getSurfaceHolder().bindToMediaPlayer(inerPlayer);
            inerPlayer.prepareAsync();
        }


        private void setListener() {
            YfCloudPlayer.OnPreparedListener mPreparedListener = new YfCloudPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(YfCloudPlayer mp) {
                    isPrepared = true;
                    preparing = false;
                    if (!playAsNextPlayer)//预加载的视频不要在onPrepared中播放，等待上个视频complete后手动发起start命名
                        startVideoPlayback();
                }
            };
            YfCloudPlayer.OnCompletionListener mCompleListener = new YfCloudPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(YfCloudPlayer mp) {
                    inerPlayer.setDisplay(null);
                    Log.d(TAG, "开始reset");
                    inerPlayer.reset();//释放资源，并重置该播放器
                    Log.d(TAG, "结束reset");
                    if (inerSurfaceView != null){
                        inerSurfaceView.removeRenderCallback(callback);
                        inerSurfaceView.addRenderCallback(callback);
                    }

//                    if (inerSurfaceView != null) {

//                        surfaceView.getHolder().removeCallback(surfaceHolder);
//                        inerSurfaceView.setVisibility(View.GONE);
//                        Log.d(TAG, "重置surface");
//                        inerSurfaceView.setVisibility(View.VISIBLE);
//                        surfaceView.getHolder().addCallback(surfaceHolder);
//                    }
                    next();//通知预加载的播放器开始播放
                }
            };
            inerPlayer.setOnPreparedListener(mPreparedListener);
            inerPlayer.setOnCompletionListener(mCompleListener);
        }


        private void releasePlayer() {
            Log.d(TAG, "releasePlayer");
            inerPlayer.reset();
            preparing = false;
            isPrepared = false;
        }

        public void startVideoPlayback() {
            Log.d(TAG, "startVideoPlayback:" + inerPlayer);
            if (inerSurfaceView != null) {
                if (isPrepared) {//即使间隔了10s，也需要确认isPrepared
                    Log.d(TAG, "正式播放~");
                    inerPlayer.start();
                    playAsNextPlayer = false;
                    duration = inerPlayer.getDuration();
                } else {
                    Toast.makeText(SmoothPlayDemo.this, "播放失败", Toast.LENGTH_SHORT).show();
                }

            }
        }

        IRenderView.IRenderCallback callback = new IRenderView.IRenderCallback() {
            @Override
            public void onSurfaceCreated(IRenderView.ISurfaceHolder holder, int width, int height) {
                if (inerPlayer != null && !preparing && !isPrepared)
                    preparePlayer();
            }

            @Override
            public void onSurfaceChanged(IRenderView.ISurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void onSurfaceDestroyed(IRenderView.ISurfaceHolder holder) {
                if (inerPlayer != null)
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
