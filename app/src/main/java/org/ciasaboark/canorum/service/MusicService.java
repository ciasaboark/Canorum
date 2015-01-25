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

package org.ciasaboark.canorum.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.Song;
import org.ciasaboark.canorum.activity.MainActivity;

import java.util.ArrayList;

import org.ciasaboark.canorum.database.DatabaseWrapper;

/**
 * Created by Jonathan Nelson on 1/16/15.
 */
public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private static final String TAG = "MediaService";
    private static final int NOTIFY_ID = 1;
    private final IBinder musicBind = new MusicBinder();
    private DatabaseWrapper databaseWrapper;
    private Notification mNotification = null;
    private boolean mPreparing = true;
    private Song mCurSong;
    private MediaPlayer player;
    private ArrayList<Song> songs;
    private int songPos;
    private String songTitle = "";

    @Override
    public void onCreate() {
        super.onCreate();
        songPos = 0;
        player = new MediaPlayer();
        initMusicPlayer();
        databaseWrapper = DatabaseWrapper.getInstance(this);
    }

    public void initMusicPlayer() {
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnErrorListener(this);
        player.setOnCompletionListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent i) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent i) {
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (player.getCurrentPosition() > 0) {
            if (mCurSong != null) {
                adjustRatingForSong(mCurSong, RATING_INCREASE.COMPLETION);
                mCurSong = null;
            }
            mp.reset();
            playNext();

        }
    }

    private void adjustRatingForSong(Song song, RATING_INCREASE reason) {
        Log.d(TAG, "adjustRatingForSong()");
        int curRating = databaseWrapper.getRatingForSong(mCurSong);
        double newRating;
        switch (reason) {
            case COMPLETION:
                newRating = curRating * RATING_INCREASE.COMPLETION.value;
                break;
            case SKIP_LONG:
                newRating = curRating * RATING_INCREASE.SKIP_LONG.value;
                break;
            case SKIP_MED:
                newRating = curRating * RATING_INCREASE.SKIP_MED.value;
                break;
            case SKIP_SHORT:
                newRating = curRating * RATING_INCREASE.SKIP_SHORT.value;
                break;
            case THUMBS_UP:
                newRating = curRating + RATING_INCREASE.THUMBS_UP.value;
                break;
            case THUMBS_DOWN:
                newRating = curRating - RATING_INCREASE.THUMBS_DOWN.value;
                break;
            default:
                newRating = curRating;
        }

        //the new rating might be over the min/max values (0, 100)
        newRating = newRating < 0 ? 0 : newRating;
        newRating = newRating > 100 ? 100 : newRating;
        Log.d(TAG, "changing rating for song " + song + " from " + curRating + " to " + newRating);
        databaseWrapper.setRatingForSong(song, (int) newRating);

    }

    public void playNext() {
        Log.d(TAG, "playNext()");
        if (mCurSong != null) {
            int duration = player.getDuration();
            int curPos = player.getCurrentPosition();
            if (duration != 0) {
                double percentPlayed = (double)curPos / (double)duration;
                if (percentPlayed >= 0 && percentPlayed < 0.25) { //between [0-25)% played
                    Log.d(TAG, "adjust rating for song " + mCurSong + " as SKIP_LONG");
                    adjustRatingForSong(mCurSong, RATING_INCREASE.SKIP_LONG);
                } else if (percentPlayed >= 0.25 && percentPlayed < 0.75) { //between [25-75)% played
                    Log.d(TAG, "adjust rating for song " + mCurSong + " as SKIP_MED");
                    adjustRatingForSong(mCurSong, RATING_INCREASE.SKIP_MED);
                } else if (percentPlayed >= 0.75) { //between [75-100]% played
                    Log.d(TAG, "adjust rating for song " + mCurSong + " as SKIP_SHORT");
                    adjustRatingForSong(mCurSong, RATING_INCREASE.SKIP_SHORT);
                }
            }
        }
        songPos++;
        if (songPos >= songs.size()) {
            songPos = 0;
        }
        playSong();
    }

    public void playSong() {
        mPreparing = true;
        player.reset();
        Song playSong = songs.get(songPos);
        mCurSong = playSong;
        songTitle = playSong.getTitle();
        long currSong = playSong.getId();
        Uri trackUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);
        try {
            player.setDataSource(getApplicationContext(), trackUri);
            player.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "Error setting player data source: " + e);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mPreparing = false;
        mp.start();
        Intent playIntent = new Intent(MusicControllerSingleton.ACTION_PLAY);
        playIntent.putExtra("curSong", mCurSong);
        LocalBroadcastManager.getInstance(this).sendBroadcast(playIntent);
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.android_music_player_play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle);
        mNotification = builder.build();

        startForeground(NOTIFY_ID, mNotification);
    }

    public void setList(ArrayList<Song> songs) {
        this.songs = songs;
    }

    public Song getCurSong() {
        //if we are still preparing the song to be played then we cant trust that it will load
        //correctly
        return mPreparing ? null : mCurSong;
    }

    public void setSong(int songIndex) {
        songPos = songIndex;
    }

    public int getPosn() {
        return player.getCurrentPosition();
    }

    public int getDur() {
        return player.getDuration();
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public void pausePlayer() {
        player.pause();
        stopForeground(true);
    }

    public void seek(int posn) {
        player.seekTo(posn);
    }

    public void go() {
        player.start();
        if (mNotification != null) {
            startForeground(NOTIFY_ID, mNotification);
        }
    }

    public void playPrev() {
        Log.d(TAG, "playPrev()");

        songPos--;
        if (songPos < 0) {
            songPos = songs.size() - 1;
        }
        playSong();
    }

    public enum RATING_INCREASE {
        COMPLETION(1.1),     //song completed playing all the way through
        SKIP_LONG(0.6),      //user skipped a large amount of the song
        SKIP_MED(0.75),       //user skipped a medium amount of the song
        SKIP_SHORT(0.9),     //user skipped a short amount of the song
        THUMBS_UP(40),      //user clicked thumbs up button
        THUMBS_DOWN(40);   //user clicked thumbs down button

        public final double value;

        RATING_INCREASE(double val) {
            this.value = val;
        }

        }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }
}
