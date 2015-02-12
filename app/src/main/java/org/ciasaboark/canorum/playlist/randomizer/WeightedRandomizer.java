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

package org.ciasaboark.canorum.playlist.randomizer;

import android.content.Context;
import android.util.Log;

import org.ciasaboark.canorum.song.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Jonathan Nelson on 1/26/15.
 */
public class WeightedRandomizer extends Randomizer {
    private static final String TAG = "WeightedRandomizer";

    public WeightedRandomizer(Context ctx) {
        super(ctx);
    }

    @Override
    public Track getNextTrack(List<Track> trackList, Track curTrack) {
        //TODO non-optimized algorithm, there should be a faster way to do this
        List<Track> bucket = new ArrayList<Track>();
        for (Track track : trackList) {
            if (track.equals(curTrack)) {
                Log.d(TAG, "skipping track " + track + ", is current track");
            } else {
//            Log.d(TAG, "adding " + track.getRating() + " copies of " + track + " to bucket");
                for (int i = 0; i < track.getRating(); i++) {
                    bucket.add(track);
                }
            }
        }

        Random random = new Random();
        int max = bucket.size() + 1;  //TODO check that this will not overflow container size
        int index = random.nextInt(max);
        Track randomTrack = bucket.get(index);
        Log.d(TAG, "picked track " + randomTrack);
        return randomTrack;
    }
}
