package com.yunfan.player.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.example.yfcloudplayer.R;
import com.yunfan.player.MainActivity;
import com.yunfan.player.extra.CustomMediaController;
import com.yunfan.player.widget.YfPlayerKit;

import java.util.HashMap;


/**
 * Created by xjx-pc on 2016/1/8 0008.
 */
public abstract class BasePlayerDemo extends AppCompatActivity {
    private String TAG = "Yf_BasePlayerDemo";
    public static final String PLAY_PATH = "play_path";
    public static final String CURRENT_POSITION = "current_position";
    public static final String P2P_MODE = "p2p_mode";
    protected boolean isFulllScreen;
    protected boolean mInitEnd = false;
    protected boolean currentDecodeMode = MainActivity.DECODE_MODE > 1 ? false : true;
    protected boolean p2pMode = false;
    protected CustomMediaController YfMediaController;
    protected int mCurrentVideoIndex = 0;
    private NetworkConnectChangedReceiver receiver;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        isFulllScreen = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? true : false;
        p2pMode = getIntent().getBooleanExtra(P2P_MODE, false);
        if (enableGravityListener())
            startListener();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        receiver = new NetworkConnectChangedReceiver();
        registerReceiver(receiver, filter);
    }

    protected boolean enableGravityListener() {
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_change_ratio:
                mCurrentRatioIndex++;
                mCurrentRatioIndex %= s_allAspectRatio.length;
                changeRatio(mCurrentRatioIndex);
                break;
            case R.id.action_decode_mode:
                currentDecodeMode = !currentDecodeMode;
                changeDecodeMode(currentDecodeMode);
                break;
            case R.id.action_next:
                next();
                break;
            case R.id.action_replay:
                //release();
                replay();
                break;
            case R.id.action_show_info:
                Log.d(TAG, "show info");
                showInfo();
                break;
            case R.id.action_show_log:
                Log.d(TAG, "show log");
                showLog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private static final int[] s_allAspectRatio = {
            YfPlayerKit.VIDEO_LAYOUT_FIT_PARENT,
            YfPlayerKit.VIDEO_LAYOUT_FILL_PARENT,
            YfPlayerKit.VIDEO_LAYOUT_WRAP_CONTENT,
            YfPlayerKit.VIDEO_LAYOUT_16_9_FIT_PARENT,
            YfPlayerKit.VIDEO_LAYOUT_4_3_FIT_PARENT};
    private int mCurrentRatioIndex = 0;

    /**
     * 切换成窗口模式
     */
    protected void setToWindowMode(boolean auto) {
        Log.d(TAG, "setToWindowMode~");
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setAttributes(params);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        if (!auto)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        FrameLayout.LayoutParams lp = getLayoutParams(false);
        setLayoutParams(lp);
    }


    /**
     * 切换成全屏居中模式
     */
    protected void setToFullScreenMode(boolean auto) {
        Log.d(TAG, "setToFullScreenMode~");
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(params);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        if (!auto)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        FrameLayout.LayoutParams lp = getLayoutParams(true);
        setLayoutParams(lp);

    }

    protected FrameLayout.LayoutParams getLayoutParams(boolean fullScreen) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        if (fullScreen) {
            lp.gravity = Gravity.CENTER;
            Log.d(TAG, "设置全屏" + lp.width + "___" + lp.height);
        } else {
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            lp.gravity = Gravity.NO_GRAVITY;
            lp.width = dm.widthPixels;
            lp.height = (int) ((lp.width * 9) / 16 + 0.5f);
            Log.d(TAG, "设置窗口模式宽高" + lp.width + "___" + lp.height);
        }
        return lp;
    }

    /**
     * 开启监听器
     */
    private final void startListener() {
        mOrientationListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int rotation) {

                if (startRotation == -2) {//初始化角度
                    startRotation = rotation;
                }
                //变化角度大于30时，开启自动旋转，并关闭监听
                int r = Math.abs(startRotation - rotation);
                r = r > 180 ? 360 - r : r;
                if (r > 30) {
                    //开启自动旋转，响应屏幕旋转事件
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                    this.disable();
                }
            }

        };
        mOrientationListener.enable();
    }

    @Override
    protected void onDestroy() {
        if (receiver != null)
            unregisterReceiver(receiver);
//        mYfp2p.JCloseChannel();
        Log.i(TAG, "JCloseChannel");
        super.onDestroy();
    }

    private OrientationEventListener mOrientationListener; // 屏幕方向改变监听器
    private int startRotation;
    Handler orientationHandler = new Handler() {
        public void handleMessage(Message msg) {
            startRotation = -2;
            mOrientationListener.enable();
        }
    };

    public abstract void changeDecodeMode(boolean hw);

    public abstract void replay();

    public abstract int getOrientation();

    public abstract void next();

    public abstract void release();

    public abstract boolean isPlaying();

    public abstract void onWifiConnected();

    public abstract void showLog();

    public abstract void showInfo();

    public abstract void changeRatio(int ratio);

    public abstract void setLayoutParams(FrameLayout.LayoutParams lp);

    public class NetworkConnectChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (null != parcelableExtra) {
                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                    NetworkInfo.State state = networkInfo.getState();
                    boolean isConnected = state == NetworkInfo.State.CONNECTED;// 当然，这边可以更精确的确定状态
//                    Log.e(Tag, "isConnected" + isConnected);
                    if (isConnected) {
                        if (!isPlaying()) {
                            onWifiConnected();
                        }
                    } else {

                    }
                }
            }
        }
    }

    protected HashMap<String, Object> createInfoMap(String title, String info) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("title", title);
        map.put("content", info);
        return map;
    }
}
