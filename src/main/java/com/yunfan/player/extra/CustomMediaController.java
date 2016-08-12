/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (C) 2013 YIXIA.COM
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

package com.yunfan.player.extra;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.view.KeyEvent;
import android.view.View;
import android.widget.MediaController;

import com.yunfan.player.utils.Log;

import java.util.ArrayList;


public class CustomMediaController extends MediaController {
    private final String TAG="YfMediaController";
    private MediaPlayerControl tempPlayer;
    private int seekUnit=10000;
    private Context mContext;
    private ActionBar mActionBar;
    public CustomMediaController(Context context) {
        super(context);
        mContext=context;
    }

    @Override
    public void setMediaPlayer(MediaPlayerControl player) {
        super.setMediaPlayer(player);
        tempPlayer = player;
    }

    public void setTvSeekUnit(int unit){
        seekUnit=unit;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG,"isShowing:"+isShowing()+"__event.getKeyCode :"+event.getKeyCode()+"___tempPlayer.getCurrentPosition():"+tempPlayer.getCurrentPosition());
        if (event.getAction() == KeyEvent.ACTION_UP)
        {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_HOME)
            {
                 Intent home = new Intent(Intent.ACTION_MAIN);

                 home.addCategory(Intent.CATEGORY_HOME);
            }
        }
        else if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount()==0)
        {
            int keyCode = event.getKeyCode();
            this.show();
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
            {
                tempPlayer.seekTo(tempPlayer.getCurrentPosition()
                        - seekUnit);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
            {
                tempPlayer.seekTo(tempPlayer.getCurrentPosition()
                        + seekUnit);
                return true;
            }

        }
        else if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount()>0)
        {
            this.show();
            int keyCode = event.getKeyCode();

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
            {
                tempPlayer.seekTo(tempPlayer.getCurrentPosition()
                        - seekUnit*2);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
            {
                tempPlayer.seekTo(tempPlayer.getCurrentPosition()
                        + seekUnit*2);
                return true;
            }

        }
        return super.dispatchKeyEvent(event);
    }

    public void setSupportActionBar(ActionBar actionBar) {
        mActionBar = actionBar;
        if (isShowing()) {
            actionBar.show();
        } else {
            actionBar.hide();
        }
    }

    @Override
    public void show() {
        super.show();
        Log.d(TAG,"maCTIONBAR:"+mActionBar);
        if (mActionBar != null)
            mActionBar.show();
    }

    @Override
    public void hide() {
        super.hide();
        if (mActionBar != null)
            mActionBar.hide();
        for (View view : mShowOnceArray)
            view.setVisibility(View.GONE);
        mShowOnceArray.clear();;
    }

    private ArrayList<View> mShowOnceArray = new ArrayList<View>();

    public void showOnce(View view) {
        mShowOnceArray.add(view);
        view.setVisibility(View.VISIBLE);
        show();
    }
}
