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

package org.ciasaboark.canorum.playlist;

import android.content.Context;

import org.ciasaboark.canorum.playlist.list.PlayQueue;
import org.ciasaboark.canorum.playlist.list.RecentlyPlayed;
import org.ciasaboark.canorum.playlist.list.SystemSink;
import org.ciasaboark.canorum.playlist.playlist.Playlist;
import org.ciasaboark.canorum.song.Track;

import java.util.List;

/**
 * Created by Jonathan Nelson on 1/25/15.
 */
public class PlaylistOrganizer {
    private static final String TAG = "PlayList";
    private final Context mContext;
    private PlayQueue mPlayQueue;
    private SystemSink mSystemSink;
    private RecentlyPlayed mRecentlyPlayed;
    private Track mCurrentTrack;
    private Playlist mPlaylist;

    public PlaylistOrganizer(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
        mPlayQueue = new PlayQueue(mContext);
        mSystemSink = new SystemSink(mContext);
        mRecentlyPlayed = new RecentlyPlayed(mContext);
    }

    public void attachPlaylist(Playlist playlist) {
        mPlaylist = playlist;
    }

    public void detatchPlaylist() {
        mPlaylist = null;
    }

    public boolean isPlayListEmpty() {
        return mPlayQueue.isEmpty() && mSystemSink.isEmpty();
    }

    public boolean hasNext() {
        return mPlayQueue.hasNext() || mSystemSink.hasNext();
    }

    public boolean hasPrevious() {
        return mRecentlyPlayed.hasPrevious();
    }

    public void notifyTrackCanNotBePlayed(Track track) {
        mPlayQueue.removeTrackIfExists(track);
        mSystemSink.removeTrackIfExists(track);
        mRecentlyPlayed.removeTrackIfExists(track);
        mCurrentTrack = null;
    }

    public List<Track> getQueuedTracks() {
        return mPlayQueue.getQueuedTracks();
    }

    public Track getNextTrack() {
        if (mCurrentTrack != null) {
            mRecentlyPlayed.addTrack(mCurrentTrack);
        }

        Track track;
        if (!mPlayQueue.isEmpty()) {
            track = mPlayQueue.getNextTrack();
        } else {
            track = mSystemSink.getTrack();
        }

        mCurrentTrack = track;
        return track;
    }

    public Track getPrevTrack() {
        Track t = mRecentlyPlayed.getLastTrackPlayed();
        mCurrentTrack = t;
        return mCurrentTrack;
    }

    public void clearHistory() {
        mRecentlyPlayed.clear();
    }

    public void addTrackToQueue(Track track) {
        mPlayQueue.addTrack(track);
    }

    public void addSongToQueueHead(Track track) {
        mPlayQueue.addTrackToHead(track);
    }

    public void replaceQueue(List<Track> newQueue) {
        mPlayQueue.replaceQueue(newQueue);
    }

    public Playlist getCurPlaylist() {
        return mPlaylist;
    }
}
