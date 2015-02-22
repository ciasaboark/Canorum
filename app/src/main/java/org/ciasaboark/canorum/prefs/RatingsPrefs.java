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

package org.ciasaboark.canorum.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by Jonathan Nelson on 1/26/15.
 */
public class RatingsPrefs {
    private static final String PREFS_FILE = "prefs.ratings";
    private static final String TAG = "RatingsPrefs";
    private final Context mContext;
    private final SharedPreferences mSharedPreferences;

    public RatingsPrefs(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
        mSharedPreferences = mContext.getSharedPreferences(PREFS_FILE, mContext.MODE_PRIVATE);
    }

    public boolean isAutoRatingsEnabled() {
        boolean ratingsEnabled = mSharedPreferences.getBoolean(KeySet.AUTO_RATINGS_ENABLED, true);
        return ratingsEnabled;
    }

    public void setAutoRatingsEnabled(boolean ratingsEnabled) {
        mSharedPreferences.edit().putBoolean(KeySet.AUTO_RATINGS_ENABLED, ratingsEnabled).apply();
    }

    public Mode getRatingAlgoritm() {
        Mode mode = Mode.STANDARD;
        try {
            String modeString = mSharedPreferences.getString(KeySet.RATING_ALGORITHM, "");
            mode = Mode.valueOf(modeString);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "no or bad rating mode set, setting default value of standard");
            setRatingAlgoritm(Mode.STANDARD);
        }
        return mode;
    }

    public void setRatingAlgoritm(Mode mode) {
        mSharedPreferences.edit().putString(KeySet.RATING_ALGORITHM, mode.toString()).apply();
    }

    public boolean willAvoidAccidentalSkips() {
        return mSharedPreferences.getBoolean(KeySet.AVOID_ACCIDENTAL_SKIPS, true);
    }

    public void setAvoidAccidentalSkips(boolean avoidAccidentalSkips) {
        mSharedPreferences.edit().putBoolean(KeySet.AVOID_ACCIDENTAL_SKIPS, avoidAccidentalSkips).apply();
    }

    public enum Mode {
        STANDARD,
        OPTIMISTIC,
        LINEAR,
        PREFER_FULL;

        public static Mode fromString(String myEnumString) {
            try {
                return valueOf(myEnumString);
            } catch (Exception ex) {
                throw new IllegalArgumentException("unknown Mode " + myEnumString);
            }
        }
    }

    private class KeySet {
        public static final String AUTO_RATINGS_ENABLED = "auto_ratings_enabled";
        public static final String RATING_ALGORITHM = "rating_algoritim";
        public static final String AVOID_ACCIDENTAL_SKIPS = "avoid_accidental_skips";
    }
}
