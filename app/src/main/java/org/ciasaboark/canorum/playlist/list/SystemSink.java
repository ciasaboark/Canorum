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

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.playlist.provider.MergedProvider;
import org.ciasaboark.canorum.playlist.randomizer.LeastOftenPlayedRandomizer;
import org.ciasaboark.canorum.playlist.randomizer.LinearRandomizer;
import org.ciasaboark.canorum.playlist.randomizer.Randomizer;
import org.ciasaboark.canorum.playlist.randomizer.TrueRandomizer;
import org.ciasaboark.canorum.playlist.randomizer.WeightedRandomizer;
import org.ciasaboark.canorum.prefs.ShufflePrefs;
import org.ciasaboark.canorum.song.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonathan Nelson on 1/25/15.
 */
public class SystemSink {
    private static final String TAG = "SystemSink";
    private final Context mContext;
    private final ShufflePrefs mShufflePrefs;
    private List<Track> mTracks = new ArrayList<Track>();


    public SystemSink(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
        buildSongList();
        mShufflePrefs = new ShufflePrefs(mContext);
    }

    public void buildSongList() {
        MergedProvider provider = MergedProvider.getInstance(mContext);
        mTracks = provider.getTrackList();
    }

    public List<Track> getTrackList() {
        return mTracks;
    }

    public void removeTrackIfExists(Track track) {
        if (mTracks.contains(track)) {
            Log.d(TAG, "removing track '" + track + "' from sink");
        } else {
            Log.d(TAG, "track '" + track + "' does not exists in sink, ignoring remove request");
        }
    }

    public Track getTrack() {
        Track track;
        Randomizer randomizer = getBestRandomizer();
        MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(mContext);
        Track curTrack = musicControllerSingleton.getCurTrack();
        track = randomizer.getNextTrack(mTracks, curTrack);

        return track;
    }

    private Randomizer getBestRandomizer() {
        Randomizer randomizer;
        switch (mShufflePrefs.getShuffleMode()) {
            case LEAST_RECENTLY_PLAYED:
                randomizer = new LeastOftenPlayedRandomizer(mContext);
                break;
            case RANDOM:
                randomizer = new TrueRandomizer(mContext);
                break;
            case LINEAR:
                randomizer = new LinearRandomizer(mContext);
                break;
            default: //WEIGHTED_RANDOM
                randomizer = new WeightedRandomizer(mContext);
                break;
        }
        return randomizer;
    }

    public boolean hasNext() {
        return !isEmpty();
    }

    public boolean isEmpty() {
        return mTracks.isEmpty();
    }
}
