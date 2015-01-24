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

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import org.ciasaboark.canorum.CurrentPlayingActivity;
import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.Song;
import org.ciasaboark.canorum.view.MusicController;
import org.ciasaboark.canorum.view.NowPlayingCard;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";
    private MusicControllerSingleton musicControllerSingleton;
    private MusicController controller;
    private NowPlayingCard mNowPlayingCard;
    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter mIntentFilter;




    //drawer layout
    private String[] drawerList = {"one", "two", "settings"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        setContentView(R.layout.activity_current_playing);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
        toolbar.setNavigationIcon(R.drawable.controls_play);
        setSupportActionBar(toolbar);

        mNowPlayingCard = (NowPlayingCard) findViewById(R.id.now_playing);
        musicControllerSingleton = MusicControllerSingleton.getInstance(this);
        musicControllerSingleton.getSongList();

        setupController();
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



    @Override
    protected void onPause(){
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

    private void updateNowPlayCard() {
        Log.d(TAG, "updateNowPlayCard()");
        mNowPlayingCard.updateWidgets();
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
            case R.id.action_shuffle:
                //TODO
                break;
            case R.id.action_end:
                Toast.makeText(this, "no longer supported", Toast.LENGTH_SHORT).show();
                finish();
                break;
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
