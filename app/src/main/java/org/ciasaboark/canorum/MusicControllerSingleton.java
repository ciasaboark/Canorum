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
import org.ciasaboark.canorum.playlist.PlaylistOrganizer;
import org.ciasaboark.canorum.playlist.playlist.Playlist;
import org.ciasaboark.canorum.rating.PlayContext;
import org.ciasaboark.canorum.service.MusicService;
import org.ciasaboark.canorum.song.Track;
import org.ciasaboark.canorum.view.MusicControllerView;

import java.util.List;

/**
 * Created by Jonathan Nelson on 1/22/15.
 */
public class MusicControllerSingleton implements MusicControllerView.SimpleMediaPlayerControl {
    //attached to the local broadcast notifications
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PREV = "ACTION_PREV";
    public static final String ACTION_SEEK = "ACTION_SEEK";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String ACTION_PLAYLIST_CHANGED = "ACTION_PLAYLIST_CHANGED";

    private static final String TAG = "MusicController";
    private static MusicControllerSingleton sInstance;
    private static Context sContext;
    private static MusicService sMusicSrv;
    private static boolean sMusicBound = false;
    private static DatabaseWrapper sDatabaseWrapper;
    private static PlaylistOrganizer sPlayListOrganizer;

    private ServiceConnection mMusicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            //get service
            sMusicSrv = binder.getService();
            //pass list
            sMusicSrv.setPlaylist(sPlayListOrganizer);
            sMusicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            sMusicBound = false;
        }
    };
    private static boolean sPaused = false;
    private static boolean sPlaybackPaused = false;
    private Intent mPlayIntent;

    private MusicControllerSingleton(Context ctx) {
        //Singleton pattern
        if (ctx == null) {
            throw new IllegalArgumentException("Context can not be null");
        }
        sContext = ctx;
        sDatabaseWrapper = DatabaseWrapper.getInstance(sContext);
        sPlayListOrganizer = new PlaylistOrganizer(sContext);
        bindService();
    }

    public void bindService() {
        if (sMusicBound) {
            Log.e(TAG, "service is already bound, will not bind again");
        } else {
            if (mPlayIntent == null) {
                mPlayIntent = new Intent(sContext, MusicService.class);
            }
            sContext.bindService(mPlayIntent, mMusicConnection, Context.BIND_AUTO_CREATE);
            sContext.startService(mPlayIntent);
        }
    }

    public static void attachPlaylist(Playlist playlist) {
        sPlayListOrganizer.attachPlaylist(playlist);
    }

    public static void detatchPlaylist() {
        sPlayListOrganizer.detatchPlaylist();
    }

    public static MusicControllerSingleton getInstance(Context ctx) {
        if (sInstance == null) {
            sInstance = new MusicControllerSingleton(ctx);
        }
        return sInstance;
    }

    /**
     * Return the current instance or null if an instance has not yet been created.
     *
     * @param context
     * @return
     */
    public static MusicControllerSingleton getInstanceNoCreate(Context context) {
        return sInstance;
    }

    public boolean isServiceBound() {
        return sMusicBound;
    }

    public void unbindService() {
        if (!sMusicBound) {
            Log.e(TAG, "service is not bound, can not unbind");
        } else {
            sContext.unbindService(mMusicConnection);
        }
    }

    public List<Track> getQueuedTracks() {
        return sPlayListOrganizer.getQueuedTracks();
    }

    public int getTrackRating(Track track) {
        return sDatabaseWrapper.getRatingForTrack(track);
    }

    public void dislikeTrack(Track track) {
        int curRating = sDatabaseWrapper.getRatingForTrack(track);
        double newRating = curRating - MusicService.RATING_INCREASE.THUMBS_DOWN.value;
        newRating = clamp((int) newRating, 0, 100);
        sDatabaseWrapper.setRatingForTrack(track, (int) newRating);
    }

    private int clamp(int val, int min, int max) {
        val = val < min ? min : val;
        val = val > max ? max : val;
        return val;
    }

    public void likeTrack(Track track) {
        int curRating = sDatabaseWrapper.getRatingForTrack(track);
        double newRating = curRating + MusicService.RATING_INCREASE.THUMBS_UP.value;
        newRating = clamp((int) newRating, 0, 100);
        sDatabaseWrapper.setRatingForTrack(track, (int) newRating);
    }

    @Override
    public void play() {
        sMusicSrv.go();
        //if we send an ACTION_PLAY notification to the controller views here then it may arrive
        //before the music has started to play, so we let the service send the broadcast message
        //from onPrepared().  If the player is paused then we send the notification from here
        if (isPaused()) {
            sendNotification(ACTION_PLAY);
        }

    }

    @Override
    public void pause(boolean actionFromNotification) {
        sPlaybackPaused = true;
        if (actionFromNotification) {
            sMusicSrv.pausePlayerKeepNotification();
        } else {
            sMusicSrv.pausePlayerDismissNotification();
        }
        sendNotification(ACTION_PAUSE);
    }

    @Override
    public void pause() {
        pause(false);
    }

    @Override
    public void stop() {
        sContext.stopService(mPlayIntent);
        sendNotification(ACTION_STOP);
        sMusicSrv = null;
    }

    @Override
    public boolean hasPrev() {
        return sPlayListOrganizer.hasPrevious();
    }

    @Override
    public void playNext() {
        sMusicSrv.playNext();
        if (sPlaybackPaused) {
            sPlaybackPaused = false;
        }
        sendNotification(ACTION_NEXT);
    }

    @Override
    public boolean hasNext() {
        return sPlayListOrganizer.hasNext();
    }

    @Override
    public void playPrev() {
        sMusicSrv.playPrev();
        if (sPlaybackPaused) {
            sPlaybackPaused = false;
        }
        sendNotification(ACTION_PREV);
    }

    @Override
    public int getDuration() {
        int duration = 0;
        if (sMusicSrv != null && sMusicBound) {
            //music service will return -1 if no duration is available
            int dur = sMusicSrv.getDur();
            if (dur != -1) {
                duration = dur;
            }
        }
        return duration;
    }

    @Override
    public int getCurrentPosition() {
        int pos = 0;
        if (sMusicSrv != null && sMusicBound) {
            pos = sMusicSrv.getPosn();
        }
        return pos;
    }

    @Override
    public void seekTo(int pos) {
        sMusicSrv.seek(pos);
        sendNotification(ACTION_SEEK);
    }

    @Override
    public boolean isPlaying() {
        boolean isPlaying = false;
        if (sMusicSrv != null && sMusicBound) {
            isPlaying = sMusicSrv.isPlaying();
        }

        return isPlaying;
    }

    @Override
    public MusicControllerView.RepeatMode getRepeatMode() {
        return null;
    }

    @Override
    public MusicControllerView.ShuffleMode getShuffleMode() {
        return null;
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public boolean isPaused() {
        return sPlaybackPaused;
    }

    @Override
    public boolean isEmpty() {
        return sPlayListOrganizer.isPlayListEmpty();
    }

    private void sendNotification(String action) {
        LocalBroadcastManager.getInstance(sContext).sendBroadcast(new Intent(action));
    }

    public void updateNotification() {
        sMusicSrv.updateNotification();
    }

    public void clearHistory() {
        sPlayListOrganizer.clearHistory();
    }

    public void addTracksToQueue(List<Track> tracks) {
        for (Track track : tracks) {
            addTrackToQueue(track);
        }
    }

    public void addTrackToQueue(Track track) {
        Log.d(TAG, "added " + track + " to queue");
        sPlayListOrganizer.addTrackToQueue(track);
    }

    public void addTracksToQueueHead(List<Track> songs) {
        //add the tracks in reverse order so that the head of the list
        // becomes the head of the play queue
        for (int i = songs.size() - 1; i >= 0; i--) {
            Track track = songs.get(i);
            addTrackToQueueHead(track);
        }
    }

    public void addTrackToQueueHead(Track song) {
        Log.d(TAG, "added " + song + " to queue head");
        sPlayListOrganizer.addSongToQueueHead(song);
    }

    public void replaceQueue(List<Track> newQueue) {
        sPlayListOrganizer.replaceQueue(newQueue);
    }

    public PlayContext getPlayContext() {
        PlayContext playContext = new PlayContext(sContext);
        playContext.setCurPosition(getCurrentPosition());
        playContext.setCurTrack(getCurTrack());
        playContext.setPlaylistOrganizer(sPlayListOrganizer);
        return playContext;
    }

    public Track getCurTrack() {
        return sMusicSrv == null ? null : sMusicSrv.getCurTrack();
    }
}
