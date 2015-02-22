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
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.activity.MainActivity;
import org.ciasaboark.canorum.database.ratings.DatabaseWrapper;
import org.ciasaboark.canorum.playlist.Playlist;
import org.ciasaboark.canorum.rating.RatingAdjuster;
import org.ciasaboark.canorum.song.Track;

/**
 * Created by Jonathan Nelson on 1/16/15.
 */
public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private static final String TAG = "MusicService";
    private static final int NOTIFY_ID = 1;
    private final IBinder musicBind = new MusicBinder();
    private DatabaseWrapper databaseWrapper;
    private Notification mNotification = null;
    private boolean mPreparing = true;
    private Track mCurTrack;
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
            if (mCurTrack != null) {
                //no need to get the actual duration and position, so long as the position is is
                //not sort enough to trigger the automatic skip detection
                adjustRatingForTrack(mCurTrack, 10, 10);
                mCurTrack = null;
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

    private void adjustRatingForTrack(Track track, int duration, int position) {
        Log.d(TAG, "adjustRatingForTrack()");
        RatingAdjuster adjuster = new RatingAdjuster(this);
        adjuster.adjustSongRating(track, duration, position);
    }

    public void playNext() {
        Log.d(TAG, "playNext()");
        if (mCurTrack != null) {
            int duration = player.getDuration();
            int curPos = player.getCurrentPosition();
            if (duration != 0) {
                adjustRatingForTrack(mCurTrack, duration, curPos);
                adjustPlayCountForTrack(mCurTrack);
            }
        }
        if (mPlaylist.hasNext()) {
            Track nextTrack = mPlaylist.getNextTrack();
            playTrack(nextTrack);
        } else {
            Log.e(TAG, "playlist has no more songs");
        }
    }

    private void adjustPlayCountForTrack(Track track) {
        databaseWrapper.incrementPlayCountForTrack(track);
    }

    private void playTrack(Track track) {
        mPreparing = true;
        player.reset();
        mCurTrack = track;
        songTitle = track.getSong().getTitle();
        long currSong = track.getSong().getId();
        Uri trackUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);
        try {
            player.setDataSource(getApplicationContext(), trackUri);
            player.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "Error setting player data source, removing track '" + track + "' from playlist: " + e);
            mPlaylist.notifyTrackCanNotBePlayed(track);
            playNext();
        }
    }

    public void playSong() {
        Track playTrack = mPlaylist.getNextTrack();
        playTrack(playTrack);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "caught media player error what(" + what + "), extras(" + extra + ")");
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mPreparing = false;
        mp.start();
        Intent playIntent = new Intent(MusicControllerSingleton.ACTION_PLAY);
        playIntent.putExtra("curSong", mCurTrack);
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
                .putString(MediaMetadata.METADATA_KEY_ALBUM, mCurTrack.getAlbum())
                .putString(MediaMetadata.METADATA_KEY_ARTIST, mCurTrack.getArtist())
                .putString(MediaMetadata.METADATA_KEY_TITLE, mCurTrack.getTitle())
                .putString(MediaMetadata.METADATA_KEY_DURATION, String.valueOf(mp.getDuration()))
                .build());
        mMediaSession.setActive(true);
        mMediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                playTrack();
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
//                // databaseWrapper.setRatingForTrack(mCurTrack, rating.);
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
                .setContentTitle(mCurTrack.getTitle())
                .setContentText(mCurTrack.getArtist())
                .setContentInfo(mCurTrack.getAlbum())
                .addAction(R.drawable.controls_prev, "prev", getActionPendingIntent(ACTION.PREV))
                .addAction(R.drawable.controls_pause, "pause",getActionPendingIntent(ACTION.PAUSE))
                .addAction(R.drawable.controls_next, "next", getActionPendingIntent(ACTION.NEXT))
                .build();

        final MediaController.TransportControls transportControls = mMediaSession.getController().getTransportControls();
        */

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

    public Track getCurTrack() {
        //if we are still preparing the song to be played then we cant trust that it will load
        //correctly
        return mPreparing ? null : mCurTrack;
    }

    public int getPosn() {
        return player.getCurrentPosition();
    }

    public int getDur() {
        int duration = -1;
        try {
            duration = player.getDuration();
        } catch (IllegalStateException e) {
            Log.e(TAG, "call to getDur() wihtout proper media player setup " + e.getMessage());
        }
        return duration;
    }

    public boolean isPlaying() {
        boolean isPlaying = false;
        try {
            isPlaying = player.isPlaying();
        } catch (IllegalStateException e) {
            Log.e(TAG, "caught IllegalStateException: " + e.getMessage());
        }
        return isPlaying;
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
            Log.w(TAG, "asked to play previous track, but none exists");
        } else {
            //since the playlist might return null as the previous song we will loop through until
            //a non-null song is found or the playlist reports that no previous songs are availablee
            boolean stillLooking = true;
            Track prevTrack = null;
            while (stillLooking && mPlaylist.hasPrevious()) {
                Track t = mPlaylist.getPrevTrack();
                if (t != null) {
                    prevTrack = t;
                    stillLooking = false;
                }
            }
            if (prevTrack != null) {
                playTrack(prevTrack);
            }
        }
    }

    private enum ACTION {
        PLAY,
        PAUSE,
        NEXT,
        PREV,
        STOP;
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
