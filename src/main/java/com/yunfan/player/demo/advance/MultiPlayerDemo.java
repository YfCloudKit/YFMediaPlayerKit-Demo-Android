package com.yunfan.player.demo.advance;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.yfcloudplayer.R;
import com.yunfan.player.widget.YfCloudPlayer;

import java.io.IOException;

/**
 * Created by xjx-pc on 2016/4/5 0005.
 */
public class MultiPlayerDemo extends Activity {
    private String TAG = "Yf_MultiPlayerDemo";
    private SurfaceView[] surfaceView = new SurfaceView[8];
    private YfCloudPlayer[] yfPlayer = new YfCloudPlayer[2];
    private String[] path = new String[8];
    public static final String[] PLAY_PATH = {"play_path1", "play_path2"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_player);
        surfaceView[0] = (SurfaceView) findViewById(R.id.surface_view1);
        surfaceView[1] = (SurfaceView) findViewById(R.id.surface_view2);
//        surfaceView[2] = (SurfaceView) findViewById(R.id.surface_view3);
//        surfaceView[3] = (SurfaceView) findViewById(R.id.surface_view4);
//        surfaceView[4] = (SurfaceView) findViewById(R.id.surface_view5);
//        surfaceView[5] = (SurfaceView) findViewById(R.id.surface_view6);
//        surfaceView[6] = (SurfaceView) findViewById(R.id.surface_view7);
//        surfaceView[7] = (SurfaceView) findViewById(R.id.surface_view8);
        for (int i = 0; i < yfPlayer.length; i++) {
            path[i] = getIntent().getStringExtra(PLAY_PATH[i % 2 == 0 ? 0 : 1]);
            yfPlayer[i] = YfCloudPlayer.Factory.createPlayer(this, YfCloudPlayer.MODE_HARD);//以硬解模式初始化
//            yfPlayer[i].setAutoSwitchDecodeMode(true);//设置当硬解失败时，是否自动切换至软件
            yfPlayer[i].setBufferSize(8 * 1024 * 1024);
            HolderManager holderManagers = new HolderManager(yfPlayer[i], surfaceView[i], path[i]);
        }
    }

    class HolderManager {
        private YfCloudPlayer inerPlayer;
        private String inerPath;
        private SurfaceView inerSurfaceView;

        public HolderManager(YfCloudPlayer tempPlayer, SurfaceView surfaceView, String path) {
            inerPlayer = tempPlayer;
            inerPath = path;
            inerSurfaceView = surfaceView;
            setListener();
            surfaceView.getHolder().addCallback(surfaceHolder);
        }

        private void setListener() {
            YfCloudPlayer.OnPreparedListener mPreparedListener = new YfCloudPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(YfCloudPlayer mp) {
                    startVideoPlayback();
                }
            };
            inerPlayer.setOnPreparedListener(mPreparedListener);
        }

        private void openVideo() {
            try {
                inerPlayer.setDataSource(inerPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            inerPlayer.setDisplay(inerSurfaceView.getHolder());
            inerPlayer.prepareAsync();
        }

        private void releasePlayer() {
            inerPlayer.release();
        }

        private void startVideoPlayback() {
            Log.d(TAG, "startVideoPlayback");
//        holder.setFixedSize(mVideoWidth, mVideoHeight);
            inerPlayer.start();
        }

        SurfaceHolder.Callback surfaceHolder = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                openVideo();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                releasePlayer();
            }
        };
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
