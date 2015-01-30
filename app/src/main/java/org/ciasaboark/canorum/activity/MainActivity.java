/*
 * Copyright (c) 2015, Jonathan Nelson
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.ciasaboark.canorum.activity;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.ciasaboark.canorum.CurrentPlayingActivity;
import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.Song;
import org.ciasaboark.canorum.artwork.AlbumArtLoader;
import org.ciasaboark.canorum.view.MusicController;
import org.ciasaboark.canorum.view.NowPlayingCard;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";
    private MusicControllerSingleton musicControllerSingleton;
    private MusicController controller;
    private NowPlayingCard mNowPlayingCard;
    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter mIntentFilter;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        setContentView(R.layout.activity_current_playing);

        mToolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
        setSupportActionBar(mToolbar);
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, mToolbar, R.string.nav_drawer_open, R.string.nav_drawer_closed);
        drawerLayout.setDrawerListener(actionBarDrawerToggle);
//        actionBarDrawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


        mNowPlayingCard = (NowPlayingCard) findViewById(R.id.now_playing);
        musicControllerSingleton = MusicControllerSingleton.getInstance(this);

        setupController();
        initBroadcastReceivers();
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(MusicControllerSingleton.ACTION_PLAY);
        mIntentFilter.addAction(MusicControllerSingleton.ACTION_PAUSE);
        mIntentFilter.addAction(MusicControllerSingleton.ACTION_NEXT);
        mIntentFilter.addAction(MusicControllerSingleton.ACTION_PREV);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case MusicControllerSingleton.ACTION_PLAY:
                        Song curSong = null;
                        curSong = (Song) intent.getSerializableExtra("curSong");
                        if (curSong == null) {
                            Log.w(TAG, "got broadcast notification that a song has began playing, but could " +
                                    "not get song from intent");
                        }
                        updateNowPlayCard();
                        break;
                    default:
                        Log.d(TAG, "got a broadcast notification with action type " +
                                intent.getAction() + " which is not yet supported");
                }
            }
        };
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }

    private void setupController() {
        Log.d(TAG, "setupController()");
        controller = (MusicController) findViewById(R.id.media_controls);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicControllerSingleton.playPrev();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicControllerSingleton.playNext();
            }
        });
        controller.setMediaPlayerController(MusicControllerSingleton.getInstance(this));
        controller.setEnabled(true);
    }

    private void initBroadcastReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int colorPrimary = getResources().getColor(R.color.color_primary);
                int newColor = intent.getIntExtra(AlbumArtLoader.BROADCAST_COLOR_CHANGED_PRIMARY, colorPrimary);
                Drawable d = mToolbar.getBackground();
                int oldColor = newColor;
                if (d instanceof ColorDrawable) {
                    oldColor = ((ColorDrawable) d).getColor();
                }
                final boolean useAlpha = !(newColor == colorPrimary);

                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), oldColor, newColor);
                colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator animator) {
                        int color = (Integer) animator.getAnimatedValue();
                        int colorWithAlpha = Color.argb(150, Color.red(color), Color.green(color),
                                Color.blue(color));
                        float[] hsv = new float[3];
                        Color.colorToHSV(color, hsv);
                        hsv[2] *= 0.8f; // value component
                        int darkColor = Color.HSVToColor(hsv);
                        int darkColorWithAlpha = Color.argb(150, Color.red(darkColor), Color.green(darkColor),
                                Color.blue(darkColor));

                        mToolbar.setBackgroundColor(useAlpha ? colorWithAlpha : color);
                        if (Build.VERSION.SDK_INT >= 21) {
                            getWindow().setStatusBarColor(useAlpha ? darkColorWithAlpha : darkColor);
                        }
                    }

                });
                colorAnimation.start();

            }
        }, new IntentFilter(AlbumArtLoader.BROADCAST_COLOR_CHANGED));
    }

    private void updateNowPlayCard() {
        Log.d(TAG, "updateNowPlayCard()");
        mNowPlayingCard.updateWidgets();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        mNowPlayingCard.updateWidgets();
        controller.updateWidgets();
    }

    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                Intent i = new Intent(this, TestActivity.class);
                startActivity(i);
                return true;
            case R.id.action_cur:
                Intent j = new Intent(this, CurrentPlayingActivity.class);
                startActivity(j);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
