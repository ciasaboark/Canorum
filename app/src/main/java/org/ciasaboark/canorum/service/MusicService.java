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
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
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
import org.ciasaboark.canorum.database.ratings.DatabaseWrapper;
import org.ciasaboark.canorum.playlist.Playlist;
import org.ciasaboark.canorum.rating.RatingAdjuster;

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
    private String songTitle = "";
    private Playlist mPlaylist;
    private MediaSession mMediaSession;

    @Override
    public void onCreate() {
        super.onCreate();
        player = new MediaPlayer();
//        mMediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
//        mMediaSession = new MediaSession(this, "media session tag");
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
                //no need to get the actual duration and position, so long as the position is is
                //not sort enough to trigger the automatic skip detection
                adjustRatingForSong(mCurSong, 10, 10);
                mCurSong = null;
            }
            mp.reset();
            int curPos = player.getCurrentPosition();
            int duration = player.getDuration();
            if (curPos == duration) {
                //automatically advance to the next song if this one was played completely
                //TODO check repeat repeat settings to see whether to advance, repeat same song, etc...
                playNext();
            }

        }
    }

    private void adjustRatingForSong(Song song, int duration, int position) {
        Log.d(TAG, "adjustRatingForSong()");
        RatingAdjuster adjuster = new RatingAdjuster(this);
        adjuster.adjustSongRating(song, duration, position);
    }

    public void playNext() {
        Log.d(TAG, "playNext()");
        if (mCurSong != null) {
            int duration = player.getDuration();
            int curPos = player.getCurrentPosition();
            if (duration != 0) {
                adjustRatingForSong(mCurSong, duration, curPos);
            }
        }
        Song nextSong = mPlaylist.getNextSong();
        playSong(nextSong);
    }

    private void playSong(Song song) {
        mPreparing = true;
        player.reset();
        mCurSong = song;
        songTitle = song.getTitle();
        long currSong = song.getId();
        Uri trackUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);
        try {
            player.setDataSource(getApplicationContext(), trackUri);
            player.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "Error setting player data source: " + e);
        }
    }

    public void playSong() {
        Song playSong = mPlaylist.getNextSong();
        playSong(playSong);
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


        /*
         //TESTING L NOTIFICATIONS
        mMediaSession.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_ALBUM, mCurSong.getmAlbum())
                .putString(MediaMetadata.METADATA_KEY_ARTIST, mCurSong.getArtist())
                .putString(MediaMetadata.METADATA_KEY_TITLE, mCurSong.getTitle())
                .putString(MediaMetadata.METADATA_KEY_DURATION, String.valueOf(mp.getDuration()))
                .build());
        mMediaSession.setActive(true);
        mMediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                playSong();
            }

            @Override
            public void onPause() {
                super.onPause();
                pausePlayer();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                playNext();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                playPrev();
            }

            @Override
            public void onStop() {
                super.onStop();
                //TODO
            }

//            @Override
//            public void onSetRating(RatingCompat rating) {
//                super.onSetRating(rating);
//                //TODO
//                // databaseWrapper.setRatingForSong(mCurSong, rating.);
//            }
        });
        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        final Notification notification = new Notification.Builder(this)
                .setShowWhen(false)
                .setStyle(new Notification.MediaStyle()
                        .setMediaSession(mMediaSession.getSessionToken())
                         .setShowActionsInCompactView(0, 1, 2)
                )
                .setColor(getResources().getColor(R.color.color_primary))
                .setSmallIcon(R.drawable.controls_play)
                .setContentTitle(mCurSong.getTitle())
                .setContentText(mCurSong.getArtist())
                .setContentInfo(mCurSong.getmAlbum())
                .addAction(R.drawable.controls_prev, "prev", getActionPendingIntent(ACTION.PREV))
                .addAction(R.drawable.controls_pause, "pause",getActionPendingIntent(ACTION.PAUSE))
                .addAction(R.drawable.controls_next, "next", getActionPendingIntent(ACTION.NEXT))
                .build();

        final MediaController.TransportControls transportControls = mMediaSession.getController().getTransportControls();
        */

    }

    private enum ACTION {
        PLAY,
        PAUSE,
        NEXT,
        PREV,
        STOP;
    }

    private PendingIntent getActionPendingIntent(ACTION a) {
        Intent intent;
        final ComponentName serviceName = new ComponentName(this, MusicService.class);
        switch (a) {
            case PAUSE:
                // Play and pause
                intent = new Intent(ACTION.PAUSE.toString());
                intent.setComponent(serviceName);
                break;
            case STOP:
                // Play and pause
                intent = new Intent(ACTION.STOP.toString());
                intent.setComponent(serviceName);
                break;
            case NEXT:
                // Play and pause
                intent = new Intent(ACTION.NEXT.toString());
                intent.setComponent(serviceName);
                break;
            case PREV:
                // Play and pause
                intent = new Intent(ACTION.PREV.toString());
                intent.setComponent(serviceName);
                break;
            default:    //default to PLAY
                intent = new Intent(ACTION.PLAY.toString());
                intent.setComponent(serviceName);
        }
        PendingIntent pendingIntent = PendingIntent.getService(this, 1, intent, 0);
        return pendingIntent;
    }

    public void setPlaylist(Playlist playlist) {
        mPlaylist = playlist;
    }

    public Song getCurSong() {
        //if we are still preparing the song to be played then we cant trust that it will load
        //correctly
        return mPreparing ? null : mCurSong;
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


        if (!mPlaylist.hasPrevious()) {
            Log.w(TAG, "asked to play previous song, but none exists");
        } else {
            //since the playlist might return null as the previous song we will loop through until
            //a non-null song is found or the playlist reports that no previous songs are availablee
            boolean stillLooking = true;
            Song prevSong = null;
            while (stillLooking && mPlaylist.hasPrevious()) {
                Song s = mPlaylist.getPrevSong();
                if (s != null) {
                    prevSong = s;
                    stillLooking = false;
                }
            }
            if (prevSong != null) {
                playSong(prevSong);
            }
        }
    }

    public enum RATING_INCREASE {
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
