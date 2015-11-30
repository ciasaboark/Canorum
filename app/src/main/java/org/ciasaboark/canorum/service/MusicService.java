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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.activity.MainActivity;
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.album.AlbumArtLoader;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedListener;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.database.ratings.DatabaseWrapper;
import org.ciasaboark.canorum.playlist.PlaylistOrganizer;
import org.ciasaboark.canorum.rating.PlayContext;
import org.ciasaboark.canorum.rating.RatingAdjuster;
import org.ciasaboark.canorum.receiver.RemoteControlsReceiver;
import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.song.Track;

/**
 * Created by Jonathan Nelson on 1/16/15.
 */
public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    public static final String KEY_ACTION_FROM_NOTIFICATION = "key_action_from_notification";
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
    private Bitmap mCurArtwork;
    private MediaSessionManager mMediaSessionManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mPlayer = new MediaPlayer();
        mMediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        mMediaSession = new MediaSession(this, "media session tag");
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
        try {
            mPlayer.reset();
        } catch (Exception e) {
            Log.d(TAG, "error resetting player");
        }

        try {
            mCurTrack = track;
            mSongTitle = track.getSong().getTitle();
            Uri contentUri = track.getContentUri();
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
        mCurArtwork = null;
        mPreparing = false;
        mp.start();
        Intent playIntent = new Intent(MusicControllerSingleton.ACTION_PLAY);
        playIntent.putExtra("curSong", mCurTrack);
        LocalBroadcastManager.getInstance(this).sendBroadcast(playIntent);
        initMediaSessionIfNeeded();
        updateNotification();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void initMediaSessionIfNeeded() {
        if (mMediaSession == null) {
            mMediaSession = new MediaSession(this, getResources().getString(R.string.app_name));
            mMediaSession.setActive(true);
        }
    }

    public void updateNotification() {
        if (mCurTrack == null) {
            hideNotification();
        } else {
            mNotification = getNotification(mCurTrack);
            Album album = Album.newSimpleAlbum(mCurTrack.getSong().getAlbum().getAlbumName(), mCurTrack.getSong().getAlbum().getArtist().getArtistName());
            AlbumArtLoader albumArtLoader = new AlbumArtLoader(this)
                    .setAlbum(album)
                    .setArtSize(ArtSize.LARGE)
                    .setInternetSearchEnabled(true)
                    .setProvideDefaultArtwork(false)
                    .setTag(mCurTrack)
                    .setArtLoadedListener(new ArtLoadedListener() {
                        @Override
                        public void onArtLoaded(Drawable artwork, Object tag) {
                            if (artwork instanceof BitmapDrawable && mCurTrack != null && mCurTrack.equals(tag)) {
                                mCurArtwork = ((BitmapDrawable) artwork).getBitmap();
                                mNotification = getNotification(mCurTrack);
                                updateMediaMetaData();
                                showNotification();
                            }
                        }

                        @Override
                        public void onLoadProgressChanged(LoadProgress progress) {

                        }
                    })
                    .loadInBackground();
            updateMediaMetaData();
            showNotification();
        }
    }

    private void hideNotification() {
    }

    private Notification getNotification(Track curTrack) {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendIntent = PendingIntent.getActivity(this, 0,
                openIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent closeIntent = new Intent(RemoteControlsReceiver.ACTION_STOP);
        PendingIntent closePendIntent = PendingIntent.getBroadcast(this, 1, closeIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Bitmap albumArt;
        if (mCurArtwork != null) {
            albumArt = mCurArtwork;
        } else {
            albumArt = ((BitmapDrawable) getResources().getDrawable(R.drawable.default_album_art)).getBitmap();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            initMediaSessionIfNeeded();
            return getLollipopNotification(curTrack, albumArt, openPendIntent, closePendIntent);
        } else {
            return getOlderNotification(curTrack, albumArt, openPendIntent, closePendIntent);
        }
    }

    private void updateMediaMetaData() {
        if (mMediaSession == null) {
            Log.d(TAG, "can not update media meta data while media session is null");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mCurTrack == null) {
                //TODO use blank stuff
            } else {
                MediaMetadata.Builder mMetaBuilder = new MediaMetadata.Builder();

                mMetaBuilder.putText(MediaMetadata.METADATA_KEY_TITLE, mCurTrack.getSong().getTitle());
                mMetaBuilder.putText(MediaMetadata.METADATA_KEY_ALBUM, mCurTrack.getSong().getAlbum().getAlbumName());
                mMetaBuilder.putText(MediaMetadata.METADATA_KEY_ARTIST, mCurTrack.getSong().getAlbum().getArtist().getArtistName());
                mMetaBuilder.putText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, mCurTrack.getSong().getAlbum().getArtist().getArtistName());
                mMetaBuilder.putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, mCurTrack.getSong().getFormattedTrackNum());
                mMetaBuilder.putLong(MediaMetadata.METADATA_KEY_DISC_NUMBER, mCurTrack.getSong().getDiskNum());

                Bitmap albumArt;
                if (mCurArtwork != null) {
                    albumArt = mCurArtwork;
                } else {
                    albumArt = ((BitmapDrawable) getResources().getDrawable(R.drawable.default_album_art)).getBitmap();
                }

                mMetaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArt);

                PlaybackState.Builder stateBuilder = new PlaybackState.Builder();

                stateBuilder.setActiveQueueItemId(MediaSession.QueueItem.UNKNOWN_ID);

                long actions = PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_STOP | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS;

                stateBuilder.setActions(actions);
                stateBuilder.setState(PlaybackState.STATE_PLAYING, 0, 1.0f);

                mMediaSession.setMetadata(mMetaBuilder.build());
                mMediaSession.setPlaybackState(stateBuilder.build());
            }
        }
    }

    private void showNotification() {
        if (mNotification == null) {
            Log.e(TAG, "showNotification(), mNotification is null, can not post notification");
        } else {
            if (isPlaying()) {
                startForeground(NOTIFY_ID, mNotification);
            } else {
                stopForeground(false); //TODO remove this
                NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(NOTIFY_ID, mNotification);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private Notification getLollipopNotification(Track curTrack, Bitmap albumArt,
                                                 PendingIntent contentIntent, PendingIntent closeIntent) {
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher_mono)
                .setContentTitle(mSongTitle)
                .setContentText(mCurTrack.getSong().getAlbum().getArtist().getArtistName() +
                        " - " + mCurTrack.getSong().getAlbum().getAlbumName())
                .setLargeIcon(albumArt)
                .setContentIntent(contentIntent)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setStyle(new Notification.MediaStyle()
                        .setMediaSession(mMediaSession.getSessionToken()))
                .addAction(getAction(RemoteControlsReceiver.ACTION_PREV))
                .setDeleteIntent(closeIntent);


        if (isPlaying()) {
            builder.addAction(getAction(RemoteControlsReceiver.ACTION_PAUSE));
        } else {
            builder.addAction(getAction(RemoteControlsReceiver.ACTION_PLAY));
        }

        builder.addAction(getAction(RemoteControlsReceiver.ACTION_NEXT));

        return builder.build();
    }

    private Notification getOlderNotification(Track mCurTrack, Bitmap albumArt,
                                              PendingIntent contentIntent, PendingIntent closeIntent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentIntent(contentIntent)
                .setDeleteIntent(closeIntent)
                .setSmallIcon(R.drawable.ic_launcher_mono)
                .setLargeIcon(albumArt)
                .setTicker(mSongTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(mSongTitle)
                .setWhen(0)
                .setPriority(Notification.PRIORITY_MAX)
                .addAction(getCompatAction(RemoteControlsReceiver.ACTION_PREV));
        if (isPlaying()) {
            builder.addAction(getCompatAction(RemoteControlsReceiver.ACTION_PAUSE));
        } else {
            builder.addAction(getCompatAction(RemoteControlsReceiver.ACTION_PLAY));
        }

        builder.addAction(getCompatAction(RemoteControlsReceiver.ACTION_NEXT));


        return builder.build();
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private Notification.Action getAction(String action) {
        Notification.Action a = null;
        Intent i = new Intent(this, RemoteControlsReceiver.class);
        int icon;
        String title;
        int requestCode;

        switch (action) {
            case RemoteControlsReceiver.ACTION_PAUSE:
                icon = R.drawable.ic_pause_white_24dp;
                title = "pause";
                i.setAction(RemoteControlsReceiver.ACTION_PAUSE);
                i.putExtra(KEY_ACTION_FROM_NOTIFICATION, true);
                requestCode = 1;
                break;
            case RemoteControlsReceiver.ACTION_PREV:
                icon = R.drawable.ic_skip_previous_white_24dp;
                title = "prev";
                i.setAction(RemoteControlsReceiver.ACTION_PREV);
                i.putExtra(KEY_ACTION_FROM_NOTIFICATION, true);
                requestCode = 2;
                break;
            case RemoteControlsReceiver.ACTION_NEXT:
                icon = R.drawable.ic_skip_next_white_24dp;
                title = "next";
                i.setAction(RemoteControlsReceiver.ACTION_NEXT);
                i.putExtra(KEY_ACTION_FROM_NOTIFICATION, true);
                requestCode = 3;
                break;
            default: //play
                icon = R.drawable.ic_play_white_24dp;
                title = "play";
                i.setAction(RemoteControlsReceiver.ACTION_PLAY);
                i.putExtra(KEY_ACTION_FROM_NOTIFICATION, true);
                requestCode = 0;
        }

        PendingIntent pi = PendingIntent.getBroadcast(this, requestCode, i,
                PendingIntent.FLAG_CANCEL_CURRENT);
        a = new Notification.Action(icon, title, pi);
        return a;
    }

    private NotificationCompat.Action getCompatAction(String action) {
        NotificationCompat.Action a = null;
        Intent i = new Intent(this, RemoteControlsReceiver.class);
        int icon;
        String title;
        int requestCode;

        switch (action) {
            case RemoteControlsReceiver.ACTION_PAUSE:
                icon = R.drawable.ic_pause_white_24dp;
                title = "pause";
                i.setAction(RemoteControlsReceiver.ACTION_PAUSE);
                i.putExtra(KEY_ACTION_FROM_NOTIFICATION, true);
                requestCode = 1;
                break;
            case RemoteControlsReceiver.ACTION_PREV:
                icon = R.drawable.ic_skip_previous_white_24dp;
                title = "prev";
                i.setAction(RemoteControlsReceiver.ACTION_PREV);
                i.putExtra(KEY_ACTION_FROM_NOTIFICATION, true);
                requestCode = 2;
                break;
            case RemoteControlsReceiver.ACTION_NEXT:
                icon = R.drawable.ic_skip_next_white_24dp;
                title = "next";
                i.setAction(RemoteControlsReceiver.ACTION_NEXT);
                i.putExtra(KEY_ACTION_FROM_NOTIFICATION, true);
                requestCode = 3;
                break;
            default: //play
                icon = R.drawable.ic_play_white_24dp;
                title = "play";
                i.setAction(RemoteControlsReceiver.ACTION_PLAY);
                i.putExtra(KEY_ACTION_FROM_NOTIFICATION, true);
                requestCode = 0;
        }

        PendingIntent pi = PendingIntent.getBroadcast(this, requestCode, i,
                PendingIntent.FLAG_CANCEL_CURRENT);
        a = new NotificationCompat.Action(icon, title, pi);
        return a;
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

    public void setPlaylistOrganizer(PlaylistOrganizer playlistOrganizer) {
        mPlaylistOrganizer = playlistOrganizer;
    }

    public Track getCurTrack() {
        //if we are still preparing the song to be played then we cant trust that it will load
        //correctly
        return mPreparing ? null : mCurTrack;
    }

    public int getPosn() {
        int pos = -1;
        try {
            pos = mPlayer.getCurrentPosition();
        } catch (IllegalStateException e) {
            Log.e(TAG, "unable to get current position from player: " + e.getMessage());
        }
        return pos;
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
            Log.e(TAG, "" + e.getMessage());
        }

        return duration;
    }

    public void pausePlayerKeepNotification() {
        mPlayer.pause();
        stopForeground(false);
    }

    public void pausePlayerDismissNotification() {
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
