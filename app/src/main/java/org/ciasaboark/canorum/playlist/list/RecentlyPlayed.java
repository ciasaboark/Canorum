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

package org.ciasaboark.canorum.playlist.list;

import android.content.Context;
import android.util.Log;

import org.ciasaboark.canorum.song.Track;

import java.util.Stack;

/**
 * Created by Jonathan Nelson on 1/25/15.
 */
public class RecentlyPlayed {
    private static final String TAG = "RecentlyPlayed";
    private static final int MAX_STACK_SIZE = 5;
    private final Context mContext;
    private Stack<Track> mStack = new Stack<Track>();

    public RecentlyPlayed(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;

        //TODO query sharedpreferences for stored list
    }

    public boolean hasPrevious() {
        return !isEmpty();
    }

    public boolean isEmpty() {
        return mStack.isEmpty();
    }

    public boolean addTrack(Track track) {
        if (mStack.size() >= MAX_STACK_SIZE) {
            Track forgetTrack = mStack.remove(0);
            Log.d(TAG, "forgetting track '" + forgetTrack + '"');
        }
        Log.d(TAG, "accepting track '" + track + "'");
        return mStack.add(track);
    }

    public void removeTrackIfExists(Track track) {
        if (mStack.contains(track)) {
            Log.d(TAG, "removing track '" + track + "' from recents list");
        } else {
            Log.d(TAG, "track '" + track + "' does not exists in recents list, ignoring remove request");
        }
    }

    public Track getLastTrackPlayed() {
        return mStack.pop();
    }

    public void clear() {
        mStack.clear();
    }
}