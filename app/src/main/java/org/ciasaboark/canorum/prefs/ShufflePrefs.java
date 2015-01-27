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
public class ShufflePrefs {
    private static final String SHUFFLE_PREFERENCES = "prefs.shuffle";
    private static final String TAG = "ShufflePrefs";
    private final Context mContext;
    private final SharedPreferences mSharedPreferences;

    public ShufflePrefs(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
        mSharedPreferences = mContext.getSharedPreferences(SHUFFLE_PREFERENCES, mContext.MODE_PRIVATE);
    }

    public void setShuffleMode(Mode mode) {
        mSharedPreferences.edit().putString(KeySet.SHUFFLE_MODE, mode.toString()).apply();
    }

    public Mode getShuffleMode() {
        Mode mode = Mode.WEIGHTED_RANDOM;
        try {
            String enumString = mSharedPreferences.getString(KeySet.SHUFFLE_MODE, "");
            mode = Mode.fromString(enumString);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "unable to get shuffle mode from settings, saving default value as weighted_random");
            setShuffleMode(mode);
        }
        return mode;
    }


    public enum Mode {
        RANDOM,
        WEIGHTED_RANDOM,
        LINEAR,
        LEAST_RECENTLY_PLAYED;  //TODO

        public static Mode fromString (String myEnumString) {
            try {
                return valueOf(myEnumString);
            } catch (Exception ex) {
                throw new IllegalArgumentException("unknown Mode " + myEnumString);
            }
        }
    }

    private class KeySet {
        public static final String SHUFFLE_MODE = "shuffle_mode";
    }

}
