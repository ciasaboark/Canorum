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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.ciasaboark.canorum.service.MusicService;
import org.ciasaboark.canorum.view.MusicController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.ciasaboark.canorum.database.DatabaseWrapper;

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
    private static final String TAG = "MusicControllerSingleton";
    private static MusicControllerSingleton instance;
    private static Context context;
    private static MusicService musicSrv;
    private static boolean musicBound = false;
    private static ArrayList<Song> songList;
    private static DatabaseWrapper databaseWrapper;
    private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected()");
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected()");
            musicBound = false;
        }
    };
    private static boolean paused = false;
    private static boolean playbackPaused = false;
    private Intent playIntent;

    private MusicControllerSingleton() {
        //Singleton pattern
        songList = new ArrayList<Song>();
        if (playIntent == null) {
            playIntent = new Intent(context, MusicService.class);
            context.bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            context.startService(playIntent);
        }
    }

    public static MusicControllerSingleton getInstance(Context c) {
        Log.d(TAG, "getInstance()");
        if (c == null) {
            throw new IllegalArgumentException("Context can not be null");
        }
        context = c;
        databaseWrapper = DatabaseWrapper.getInstance(context);
        if (instance == null) {
            instance = new MusicControllerSingleton();
        }
        return instance;
    }

    public int getSongRating(Song song) {
        return databaseWrapper.getRatingForSong(song);
    }

    public void dislikeSong(Song song) {
        Log.d(TAG, "dislikeSong()");
        int curRating = databaseWrapper.getRatingForSong(song);
        double newRating = curRating - MusicService.RATING_INCREASE.THUMBS_DOWN.value;
        newRating = clamp((int)newRating, 0, 100);
        databaseWrapper.setRatingForSong(song, (int)newRating);
    }

    public void likeSong(Song song) {
        Log.d(TAG, "likeSong()");
        int curRating = databaseWrapper.getRatingForSong(song);
        double newRating = curRating + MusicService.RATING_INCREASE.THUMBS_UP.value;
        newRating = clamp((int)newRating, 0, 100);
        databaseWrapper.setRatingForSong(song, (int)newRating);
    }

    private int clamp(int val, int min, int max) {
        val = val < min ? min : val;
        val = val > max ? max : val;
        return val;
    }

    public Song getCurSong() {
        Log.d(TAG, "getCurSong()");
        return musicSrv == null ? null : musicSrv.getCurSong();
    }

    public int getSongListSize() {
        Log.d(TAG, "getSongListSize()");
        return songList.size();
    }

    @Override
    public void play() {
        Log.d(TAG, "play()");
        musicSrv.go();
        //if we send an ACTION_PLAY notification to the controller views here then it may arrive
        //before the music has started to play, so we let the service send the broadcast message
        //from onPrepared()
    }

    @Override
    public void pause() {
        Log.d(TAG, "pause()");
        playbackPaused = true;
        musicSrv.pausePlayer();
        sendNotification(ACTION_PAUSE);
    }

    private void sendNotification(String action) {
        Log.d(TAG, "sendNotification()");
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(action));
    }

    @Override
    public boolean hasPrev() {
        Log.d(TAG, "hasPrev()");
        //TODO
        return true;
    }

    @Override
    public void playNext() {
        Log.d(TAG, "playNext()");
        musicSrv.playNext();
        if (playbackPaused) {
            playbackPaused = false;
        }
//        sendNotification(ACTION_NEXT);
    }

    @Override
    public boolean hasNext() {
        Log.d(TAG, "hasNext()");
        return true; //TODO
    }

    @Override
    public void playPrev() {
        Log.d(TAG, "playPrev()");
        musicSrv.playPrev();
        if (playbackPaused) {
            playbackPaused = false;
        }
//        sendNotification(ACTION_PREV);
    }

    @Override
    public int getDuration() {
//        Log.d(TAG, "getDuration()");
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
//        Log.d(TAG, "getCurrentPosition()");
        int pos = 0;
        if (musicSrv != null && musicBound) {
            pos = musicSrv.getPosn();
        }
        return pos;
    }

    @Override
    public void seekTo(int pos) {
        Log.d(TAG, "seekTo()");
        musicSrv.seek(pos);
        sendNotification(ACTION_SEEK);
    }

    @Override
    public boolean isPlaying() {
//        Log.d(TAG, "isPlaying()");
        boolean isPlaying = false;
        if (musicSrv != null && musicBound) {
            isPlaying = musicSrv.isPlaying();
        }

        return isPlaying;
    }

    @Override
    public MusicController.RepeatMode getRepeatMode() {
        Log.d(TAG, "getRepeatMode()");
        return null;
    }

    @Override
    public MusicController.ShuffleMode getShuffleMode() {
        Log.d(TAG, "getShuffleMode()");
        return null;
    }

    @Override
    public boolean isReady() {
        Log.d(TAG, "isReady()");
        return false;
    }

    public boolean isPaused() {
        Log.d(TAG, "isPaused()");
        return playbackPaused;
    }

    public void getSongList() {
        Log.d(TAG, "getSongList()");
        ContentResolver musicResolver = context.getContentResolver();
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, selection, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int yearColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.YEAR);
            int albumIdColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

            do {
                long songId = musicCursor.getLong(idColumn);
                String songTitle = musicCursor.getString(titleColumn);
                String songArtist = musicCursor.getString(artistColumn);
                String songAlbum = musicCursor.getString(albumColumn);
                long albumId = musicCursor.getLong(albumIdColumn);
                Song song = new Song(songId, songTitle, songArtist, songAlbum, albumId);
                songList.add(song);
            } while (musicCursor.moveToNext());
        }

        //sort the list by title
        Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song lhs, Song rhs) {
                return lhs.getTitle().compareTo(rhs.getTitle());
            }
        });
    }

    public void stop() {
        Log.d(TAG, "stop()");
        context.stopService(playIntent);
        musicSrv = null;
    }
}
