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

/**
 * Created by Jonathan Nelson on 1/30/15.
 */
public class ArtworkPrefs {
    private static final String TAG = "ArtworkPrefs";
    private static final String PREFS_FILE = "prefs.artwork";
    private final Context mContext;
    private final SharedPreferences mSharedPreferences;

    public ArtworkPrefs(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
        mSharedPreferences = mContext.getSharedPreferences(PREFS_FILE, mContext.MODE_PRIVATE);
    }

    public boolean isInternetSearchEnabled() {
        return mSharedPreferences.getBoolean(KeySet.INTERNET_SEARCH_ENABLED, true);
    }

    public void setInternetSearchEnabled(boolean internetSearchEnabled) {
        mSharedPreferences.edit().putBoolean(KeySet.INTERNET_SEARCH_ENABLED,
                internetSearchEnabled).apply();
    }

    public boolean isAutoSaveInternetResults() {
        return mSharedPreferences.getBoolean(KeySet.AUTO_SAVE_INTERNET_RESULTS, true);
    }

    public void setAutoSaveInternetResults(boolean autoSaveInternetResults) {
        mSharedPreferences.edit().putBoolean(KeySet.AUTO_SAVE_INTERNET_RESULTS,
                autoSaveInternetResults).apply();
    }

    public boolean isOverwriteLowQualityArtwork() {
        return mSharedPreferences.getBoolean(KeySet.OVERWRITE_LOW_QUALITY_ARTWORK, true);
    }

    public void setOverwriteLowQualityArtwork(boolean overwriteLowQualityArtwork) {
        mSharedPreferences.edit().putBoolean(KeySet.OVERWRITE_LOW_QUALITY_ARTWORK,
                overwriteLowQualityArtwork).apply();
    }

    private class KeySet {
        public static final String INTERNET_SEARCH_ENABLED = "internet_search_enabled";
        public static final String AUTO_SAVE_INTERNET_RESULTS = "auto_save_internet_results";
        public static final String OVERWRITE_LOW_QUALITY_ARTWORK = "overwrite_low_quality_artwork";
    }
}
