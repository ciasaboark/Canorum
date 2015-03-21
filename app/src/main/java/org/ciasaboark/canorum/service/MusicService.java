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
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.activity.MainActivity;
import org.ciasaboark.canorum.database.ratings.DatabaseWrapper;
import org.ciasaboark.canorum.playlist.PlaylistOrganizer;
import org.ciasaboark.canorum.rating.PlayContext;
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
    private MediaPlayer mPlayer;
    private String mSongTitle = "";
    private PlaylistOrganizer mPlaylistOrganizer;
    private MediaSession mMediaSession;

    @Override
    public void onCreate() {
        super.onCreate();
        mPlayer = new MediaPlayer();
//        mMediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
//        mMediaSession = new MediaSession(this, "media session tag");
        initMusicPlayer();
        databaseWrapper = DatabaseWrapper.getInstance(this);
    }

    public void initMusicPlayer() {
        mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnCompletionListener(this);
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
        mPlayer.stop();
        mPlayer.release();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mPlayer.getCurrentPosition() > 0) {
            if (mCurTrack != null) {
                //no need to get the actual duration and position, so long as the position is is
                //not sort enough to trigger the automatic skip detection
                adjustRatingForTrack(mCurTrack, 10, 10);
                mCurTrack = null;
            }

            playNext();

        }
    }

    private void adjustRatingForTrack(Track track, int duration, int position) {
        Log.d(TAG, "adjustRatingForTrack()");
        PlayContext playContext = getPlayContext();
        RatingAdjuster adjuster = new RatingAdjuster(playContext);
        adjuster.adjustSongRating(track, duration, position);
    }

    private PlayContext getPlayContext() {
        return MusicControllerSingleton.getInstance(this).getPlayContext();
    }

    public void playNext() {
        Log.d(TAG, "playNext()");
        if (mCurTrack != null) {
            int duration = mPlayer.getDuration();
            int curPos = mPlayer.getCurrentPosition();
            if (duration != 0) {
                adjustRatingForTrack(mCurTrack, duration, curPos);
                adjustPlayCountForTrack(mCurTrack);
            }
        }
        if (mPlaylistOrganizer.hasNext()) {
            Track nextTrack = mPlaylistOrganizer.getNextTrack();
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
        mPlayer.reset();
        mCurTrack = track;
        mSongTitle = track.getSong().getTitle();
        Uri contentUri = track.getContentUri();
        try {
            mPlayer.setDataSource(getApplicationContext(), contentUri);
            mPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "Error setting player data source, removing track '" + track + "' from playlist: " + e);
            mPlaylistOrganizer.notifyTrackCanNotBePlayed(track);
            playNext();
        }
    }

    public void playSong() {
        Track playTrack = mPlaylistOrganizer.getNextTrack();
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
                .setTicker(mSongTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(mSongTitle);
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

    public void setPlaylist(PlaylistOrganizer playlistOrganizer) {
        mPlaylistOrganizer = playlistOrganizer;
    }

    public Track getCurTrack() {
        //if we are still preparing the song to be played then we cant trust that it will load
        //correctly
        return mPreparing ? null : mCurTrack;
    }

    public int getPosn() {
        return mPlayer.getCurrentPosition();
    }

    public int getDur() {
        int duration = -1;
        try {
            if (mPlayer.isPlaying()) {
                duration = mPlayer.getDuration();
            } else {
                Log.e(TAG, "call to getDur() while media player is not playing, returning -1");
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage());
        }

        return duration;
    }

    public boolean isPlaying() {
        boolean isPlaying = false;
        try {
            isPlaying = mPlayer.isPlaying();
        } catch (IllegalStateException e) {
            Log.e(TAG, "caught IllegalStateException: " + e.getMessage());
        }
        return isPlaying;
    }

    public void pausePlayer() {
        mPlayer.pause();
        stopForeground(true);
    }

    public void seek(int posn) {
        mPlayer.seekTo(posn);
    }

    public void go() {
        mPlayer.start();
        if (mNotification != null) {
            startForeground(NOTIFY_ID, mNotification);
        }
    }

    public void playPrev() {
        Log.d(TAG, "playPrev()");


        if (!mPlaylistOrganizer.hasPrevious()) {
            Log.w(TAG, "asked to play previous track, but none exists");
        } else {
            //since the playlist might return null as the previous song we will loop through until
            //a non-null song is found or the playlist reports that no previous songs are availablee
            boolean stillLooking = true;
            Track prevTrack = null;
            while (stillLooking && mPlaylistOrganizer.hasPrevious()) {
                Track t = mPlaylistOrganizer.getPrevTrack();
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
