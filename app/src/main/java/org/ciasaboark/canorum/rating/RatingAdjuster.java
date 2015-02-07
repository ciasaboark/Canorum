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

package org.ciasaboark.canorum.rating;

import android.content.Context;
import android.util.Log;

import org.ciasaboark.canorum.Song;
import org.ciasaboark.canorum.database.ratings.DatabaseWrapper;
import org.ciasaboark.canorum.prefs.RatingsPrefs;
import org.ciasaboark.canorum.rating.rater.FullPlaythroughRater;
import org.ciasaboark.canorum.rating.rater.LinearRater;
import org.ciasaboark.canorum.rating.rater.OptimisticRater;
import org.ciasaboark.canorum.rating.rater.Rater;
import org.ciasaboark.canorum.rating.rater.StandardRater;

/**
 * Created by Jonathan Nelson on 1/26/15.
 */
public class RatingAdjuster {
    private static final String TAG = "RadingAdjuster";
    private static final int MIN_RATING = 0;
    private static final int MAX_RATING = 100;
    private final RatingsPrefs mRatingsPrefs;
    private Context mContext;

    public RatingAdjuster(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
        mRatingsPrefs = new RatingsPrefs(mContext);
    }

    public void adjustSongRating(Song song, int duration, int position) {
        if (mRatingsPrefs.willAvoidAccidentalSkips() && position <= 2000) {
            Log.d(TAG, "will not rate song " + song + " was only listened to for " +
                    position / 1000 + " seconds, assuming this was an accident");
        } else {
            float percentPlayed = (float) position / (float) duration;
            adjustSongRating(song, percentPlayed);
        }
    }

    private void adjustSongRating(Song song, float percentPlayed) {
        if (song == null) {
            throw new IllegalArgumentException("can not adjust rating for null song");
        }
        if (!isAutomaticRatingsEnabled()) {
            Log.d(TAG, "will not rate song " + song + " automatic ratings turned off in settings");
        } else {
            DatabaseWrapper databaseWrapper = DatabaseWrapper.getInstance(mContext);
            int oldRating = databaseWrapper.getRatingForSong(song);
            Rater rater = getBestRater();

            int adjustment = rater.getRatingAdjustmentForPercent(percentPlayed);
            int newRating = clampRating(oldRating + adjustment);
            Log.d(TAG, rater.getClass().getSimpleName() + " adjusted rating from " + oldRating + " to " + newRating + " for song '" + song + "'");
            databaseWrapper.setRatingForSong(song, newRating);
        }
    }

    private boolean isAutomaticRatingsEnabled() {
        return mRatingsPrefs.isAutoRatingsEnabled();
    }

    private Rater getBestRater() {
        Rater rater = null;
        switch (mRatingsPrefs.getRatingAlgoritm()) {
            case OPTIMISTIC:
                rater = new OptimisticRater();
                break;
            case LINEAR:
                rater = new LinearRater();
                break;
            case PREFER_FULL:
                rater = new FullPlaythroughRater();
                break;
            default: //STANDARD
                rater = new StandardRater();
                break;
        }
        return rater;
    }

    private int clampRating(int rating) {
        rating = rating < MIN_RATING ? MIN_RATING : rating;
        rating = rating > MAX_RATING ? MAX_RATING : rating;
        return rating;
    }
}
