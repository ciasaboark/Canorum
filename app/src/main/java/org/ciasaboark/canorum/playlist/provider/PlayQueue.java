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

package org.ciasaboark.canorum.playlist.provider;

import android.content.Context;
import android.util.Log;

import org.ciasaboark.canorum.Song;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Created by Jonathan Nelson on 1/25/15.
 */
public class PlayQueue {
    private static final String TAG = "PlayQueue";
    private final Context mContext;
    private final ArrayDeque<Song> songQueue;

    public PlayQueue(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Context can not be null");
        }
        mContext = ctx;
        songQueue = new ArrayDeque<Song>();
    }

    public void removeSongIfExists(Song song) {
        if (songQueue.contains(song)) {
            Log.d(TAG, "removing song '" + song + "' from sink");
        } else {
            Log.d(TAG, "song '" + song + "' does not exists in sink, ignoring remove request");
        }
    }

    public boolean hasNext() {
        return !isEmpty();
    }

    public boolean isEmpty() {
        return songQueue.isEmpty();
    }

    public boolean addSong(Song song) {
        songQueue.addLast(song);
        return true;
    }

    /**
     * Removes and returns the head of the play queue
     *
     * @return the head of the song queue, or null if the queue is empty
     */
    public Song getNextSong() {
        Song song = ((Queue<Song>) songQueue).poll();
        return song;
    }

    public void addSongToHead(Song song) {
        songQueue.addFirst(song);
    }
}
