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

package com.yunfan.player;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.example.yfcloudplayer.R;
import com.yunfan.player.adapter.EntranceAdapter;
import com.yunfan.player.adapter.SpinnerAdapter;
import com.yunfan.player.bean.VideoItem;
import com.yunfan.player.demo.BasePlayerDemo;
import com.yunfan.player.demo.YfCloudPlayerDemo;
import com.yunfan.player.demo.YfPlayerKitDemo;
import com.yunfan.player.demo.VRYfCloudPlayerDemo;
import com.yunfan.player.demo.advance.MultiPlayerDemo;
import com.yunfan.player.demo.advance.SinglePlayerDemo;
import com.yunfan.player.demo.advance.SmoothPlayDemo;
import com.yunfan.player.extra.VideoPaths;
import com.yunfan.player.extra.YfP2PHelper;
import com.yunfan.player.widget.Authentication;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {
    private static final String TOKEN = "7b90e88fad84de9e1333d5dd576093093ef4ae6d";
    private static final String TAG = "Yf_MainActivity";
    private static final String DEFAULT_PATH = "rtmp://play-yf.nanhailong.com/live/ombl";
    //    private static final String DEFAULT_PATH = "rtmp://play.yftest.yflive.net/live/test110";
    private GridView gv;
    private Switch mSwitch;
    public static List<String> videoPaths = new ArrayList<String>();
    private List<VideoItem> itemList = new ArrayList<VideoItem>();
    private EntranceAdapter adapter;
    private EditText manualPath;
    public static int DECODE_MODE = 1;//默认解码方式
    public static int PLAYER_MODE = 1;//默认播放器模式
    public static String[] DecodeModes = {
            "解码方式", "硬解", "软解"
    };
    public static String[] PlayModes = {
            "播放器", "YfPlayerKit",
            "YfCloudPlayer",
            "无缝渲染",
            "多屏播放",
            "无缝切换",
            "全景/VR播放"
    };
    private Spinner mDecodeSpinner, mPlayerSpinner;
    private boolean p2pMode = false;
    private String firstPath;//用于记录多窗口模式下的第一个播放地址

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_entrance);
        manualPath = (EditText) findViewById(R.id.manual_add);
        manualPath.setText(DEFAULT_PATH);
        Authentication.Authenticate(TOKEN, authCallBack);
//        YfPlayerKit.enableRotation(true);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        initViews();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPerssion();
        } else {
            initPlayList();
        }

//        int codecCount= MediaCodecList.getCodecCount();
//        for(int a=0;a<codecCount;a++){
//            MediaCodecInfo mediaCodecInfo= MediaCodecList.getCodecInfoAt(a);
//            Log.d(TAG, "这个codecinfo的名字是:" + mediaCodecInfo.getName());
//            String[] supportTypes=mediaCodecInfo.getSupportedTypes();
//            for(String st:supportTypes){
//                Log.d(TAG,mediaCodecInfo.getName()+"支持的格式包括有:"+st);
//                MediaCodecInfo.CodecCapabilities cc=mediaCodecInfo.getCapabilitiesForType(st);
//                int[] color=cc.colorFormats;
//                for(i nt c:color){
//                    Log.d(TAG,st+"的colorFormats有："+c);
//                }
//                MediaCodecInfo.CodecProfileLevel[] lv=cc.profileLevels;
//                for(MediaCodecInfo.CodecProfileLevel cp:lv){
//                    Log.d(TAG,st+"的Levels有"+cp.level);
//                    Log.d(TAG,st+"的profile有"+cp.profile);
//                }
//            }
//            Log.d(TAG,"----------------------------------华丽的分割线-----------------------");
//        }
    }


    Authentication.AuthCallBack authCallBack = new Authentication.AuthCallBack() {
        @Override
        public void onAuthenticateSuccess() {
            Log.d(TAG, "鉴权成功~！");
        }

        @Override
        public void onAuthenticateError(int errorCode) {
            Log.d(TAG, "鉴权失败啦：" + errorCode);
        }
    };

    private void initViews() {
        gv = (GridView) findViewById(R.id.gview);
        mDecodeSpinner = (Spinner) findViewById(R.id.spinner1);
        mPlayerSpinner = (Spinner) findViewById(R.id.spinner2);
        mSwitch = (Switch) findViewById(R.id.openP2P);

        SpinnerAdapter _Adapter = new SpinnerAdapter(this, android.R.layout.simple_spinner_item, MainActivity.DecodeModes);
        SpinnerAdapter _Adapter2 = new SpinnerAdapter(this, android.R.layout.simple_spinner_item, MainActivity.PlayModes);
        mDecodeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                if (arg2 != 0) {
                    String modeName = DecodeModes[arg2];
                    DECODE_MODE = arg2;
//                    Toast.makeText(MainActivity.this, "切换模式为:" + modeName, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });
        mPlayerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                if (arg2 != 0) {
                    String modeName = PlayModes[arg2];
                    PLAYER_MODE = arg2;
//                    Toast.makeText(MainActivity.this, "切换播放器为:" + modeName, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });
        mDecodeSpinner.setAdapter(_Adapter);
        mPlayerSpinner.setAdapter(_Adapter2);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                p2pMode = isChecked;
                if (p2pMode) {
                    YfP2PHelper.initNetLib();
                } else if (!p2pMode) {
                    YfP2PHelper.clearNetSdk();
                }
            }
        });
    }

    private void initPlayList() {

        int i;
        for (i = 0; i < VideoPaths.Videos.length; i++) {
            videoPaths.add(VideoPaths.Videos[i]);
        }
        videoPaths.addAll(getVideoPaths());
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

                openVideo(getSafePath(videoPaths.get(position)), position);
            }
        });
    }

    public void PlayManualUrl(View v) {
        String path = ((EditText) findViewById(R.id.manual_add)).getText().toString();
        openVideo(getSafePath(path), 0);
    }

    private String getSafePath(String path) {
//
        return path;
    }

    private Bitmap getCapture(String path) {
        Bitmap bitmap = null;
        return bitmap;
    }

    private void openVideo(String path, int currentPosition) {
        if (!Authentication.isAuthenticateSucceed()) {
            Toast.makeText(this, "鉴权未通过，不能播放，开始重新鉴权~", Toast.LENGTH_SHORT).show();
            Authentication.Authenticate(TOKEN, authCallBack);
            return;
        }
        if (PLAYER_MODE == 3) {
            Intent playerIntent = new Intent(this, SinglePlayerDemo.class);
            playerIntent.putExtra(BasePlayerDemo.PLAY_PATH, path);
            playerIntent.putExtra(BasePlayerDemo.CURRENT_POSITION, currentPosition);
            startActivity(playerIntent);
            firstPath = null;
        } else if (PLAYER_MODE == 4) {
            if (firstPath == null) {
                firstPath = path;
            } else {
                Intent playerIntent = new Intent(this, MultiPlayerDemo.class);
                playerIntent.putExtra(MultiPlayerDemo.PLAY_PATH[0], firstPath);
                playerIntent.putExtra(MultiPlayerDemo.PLAY_PATH[1], path);
                startActivity(playerIntent);
                firstPath = null;
            }
        } else if (PLAYER_MODE == 5) {
            Intent playerIntent = new Intent(MainActivity.this, SmoothPlayDemo.class);
            playerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            playerIntent.putExtra(BasePlayerDemo.PLAY_PATH, path);
            playerIntent.putExtra(BasePlayerDemo.CURRENT_POSITION, currentPosition);
            startActivity(playerIntent);
            firstPath = null;
        }  else if (PLAYER_MODE == 6) {
            Intent playerIntent = new Intent(MainActivity.this, VRYfCloudPlayerDemo.class);
            playerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            playerIntent.putExtra(BasePlayerDemo.PLAY_PATH, path);
            playerIntent.putExtra(BasePlayerDemo.CURRENT_POSITION, currentPosition);
            startActivity(playerIntent);
            firstPath = null;
        }else {
            Intent playerIntent = new Intent(MainActivity.this, PLAYER_MODE < 2 ? YfPlayerKitDemo.class : YfCloudPlayerDemo.class);
            playerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            playerIntent.putExtra(BasePlayerDemo.PLAY_PATH, path);
            playerIntent.putExtra(BasePlayerDemo.CURRENT_POSITION, currentPosition);
            playerIntent.putExtra(BasePlayerDemo.P2P_MODE, p2pMode);
            startActivityForResult(playerIntent, 10086);
            firstPath = null;
        }
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
        if (p2pMode)
            YfP2PHelper.clearNetSdk();
        super.onDestroy();
    }


    private static final int CODE_FOR_WRITE_PERMISSION = 10010;

    @TargetApi(Build.VERSION_CODES.M)
    private void checkPerssion() {
        int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            Activity activty = this;
            ActivityCompat.requestPermissions(activty, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, CODE_FOR_WRITE_PERMISSION);
        } else {
            initPlayList();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CODE_FOR_WRITE_PERMISSION) {
            if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //用户同意使用write
                initPlayList();
            } else {
                //用户不同意，自行处理即可
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 10086 && resultCode == RESULT_OK) {
            mDecodeSpinner.setSelection(DECODE_MODE, false);
        }

    }

    private void showToastOnUIThread(final String content) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, content, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
