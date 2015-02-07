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

package org.ciasaboark.canorum;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.ciasaboark.canorum.database.ratings.DatabaseWrapper;
import org.ciasaboark.canorum.playlist.Playlist;
import org.ciasaboark.canorum.service.MusicService;
import org.ciasaboark.canorum.view.MusicController;

import java.util.List;

/**
 * Created by Jonathan Nelson on 1/22/15.
 */
public class MusicControllerSingleton implements MusicController.SimpleMediaPlayerControl {
    //attached to the local broadcast notifications
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PREV = "ACTION_PREV";
    public static final String ACTION_SEEK = "ACTION_SEEK";
    public static final String ACTION_PLAYLIST_CHANGED = "ACTION_PLAYLIST_CHANGED";

    private static final String TAG = "MusicControllerSingleton";
    private static MusicControllerSingleton instance;
    private static Context mContext;
    private static MusicService musicSrv;
    private static boolean musicBound = false;
    private static DatabaseWrapper databaseWrapper;
    private static Playlist mPlayList;
    private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setPlaylist(mPlayList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };
    private static boolean paused = false;
    private static boolean playbackPaused = false;
    private Intent playIntent;

    private MusicControllerSingleton(Context ctx) {
        //Singleton pattern
        if (ctx == null) {
            throw new IllegalArgumentException("Context can not be null");
        }
        mContext = ctx;
        databaseWrapper = DatabaseWrapper.getInstance(mContext);
        mPlayList = new Playlist(mContext);
        if (playIntent == null) {
            playIntent = new Intent(mContext, MusicService.class);
            mContext.bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            mContext.startService(playIntent);
        }
    }

    public static MusicControllerSingleton getInstance(Context ctx) {
        if (instance == null) {
            instance = new MusicControllerSingleton(ctx);
        }
        return instance;
    }

    public int getSongRating(Song song) {
        return databaseWrapper.getRatingForSong(song);
    }

    public void dislikeSong(Song song) {
        int curRating = databaseWrapper.getRatingForSong(song);
        double newRating = curRating - MusicService.RATING_INCREASE.THUMBS_DOWN.value;
        newRating = clamp((int) newRating, 0, 100);
        databaseWrapper.setRatingForSong(song, (int) newRating);
    }

    private int clamp(int val, int min, int max) {
        val = val < min ? min : val;
        val = val > max ? max : val;
        return val;
    }

    public void likeSong(Song song) {
        int curRating = databaseWrapper.getRatingForSong(song);
        double newRating = curRating + MusicService.RATING_INCREASE.THUMBS_UP.value;
        newRating = clamp((int) newRating, 0, 100);
        databaseWrapper.setRatingForSong(song, (int) newRating);
    }

    public Song getCurSong() {
        return musicSrv == null ? null : musicSrv.getCurSong();
    }

    @Override
    public void play() {
        musicSrv.go();
        //if we send an ACTION_PLAY notification to the controller views here then it may arrive
        //before the music has started to play, so we let the service send the broadcast message
        //from onPrepared()
    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicSrv.pausePlayer();
        sendNotification(ACTION_PAUSE);
    }

    private void sendNotification(String action) {
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(action));
    }

    @Override
    public void stop() {
        mContext.stopService(playIntent);
        musicSrv = null;
    }

    @Override
    public boolean hasPrev() {
        return mPlayList.hasPrevious();
    }

    @Override
    public void playNext() {
        musicSrv.playNext();
        if (playbackPaused) {
            playbackPaused = false;
        }
        sendNotification(ACTION_NEXT);
    }

    @Override
    public boolean hasNext() {
        return mPlayList.hasNext();
    }

    @Override
    public void playPrev() {
        musicSrv.playPrev();
        if (playbackPaused) {
            playbackPaused = false;
        }
        sendNotification(ACTION_PREV);
    }

    @Override
    public int getDuration() {
        int duration = 0;
        if (musicSrv != null && musicBound) {
            //music service will return -1 if no duration is available
            int dur = musicSrv.getDur();
            if (dur != -1) {
                duration = dur;
            }
        }
        return duration;
    }

    @Override
    public int getCurrentPosition() {
        int pos = 0;
        if (musicSrv != null && musicBound) {
            pos = musicSrv.getPosn();
        }
        return pos;
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
        sendNotification(ACTION_SEEK);
    }

    @Override
    public boolean isPlaying() {
        boolean isPlaying = false;
        if (musicSrv != null && musicBound) {
            isPlaying = musicSrv.isPlaying();
        }

        return isPlaying;
    }

    @Override
    public MusicController.RepeatMode getRepeatMode() {
        return null;
    }

    @Override
    public MusicController.ShuffleMode getShuffleMode() {
        return null;
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public boolean isPaused() {
        return playbackPaused;
    }

    @Override
    public boolean isEmpty() {
        return mPlayList.isPlayListEmpty();
    }

    public void clearHistory() {
        mPlayList.clearHistory();
    }

    public void addSongsToQueue(List<Song> songs) {
        for (Song song : songs) {
            addSongToQueue(song);
        }
    }

    public void addSongToQueue(Song song) {
        Log.d(TAG, "added " + song + " to queue");
        mPlayList.addSongToQueue(song);
    }


    public void addSongsToQueueHead(List<Song> songs) {
        for (Song song : songs) {
            addSongToQueueHead(song);
        }
    }

    public void addSongToQueueHead(Song song) {
        Log.d(TAG, "added " + song + " to queue head");
        mPlayList.addSongToQueueHead(song);
    }
}
