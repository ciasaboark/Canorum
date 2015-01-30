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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import org.ciasaboark.canorum.Song;
import org.ciasaboark.canorum.database.ratings.DatabaseWrapper;
import org.ciasaboark.canorum.playlist.randomizer.LeastOftenPlayedRandomizer;
import org.ciasaboark.canorum.playlist.randomizer.LinearRandomizer;
import org.ciasaboark.canorum.playlist.randomizer.Randomizer;
import org.ciasaboark.canorum.playlist.randomizer.TrueRandomizer;
import org.ciasaboark.canorum.playlist.randomizer.WeightedRandomizer;
import org.ciasaboark.canorum.prefs.ShufflePrefs;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonathan Nelson on 1/25/15.
 */
public class SystemSink {
    private static final String TAG = "SystemSink";
    private final Context mContext;
    private final ShufflePrefs mShufflePrefs;
    private List<Song> mSongs = new ArrayList<Song>();


    public SystemSink(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
        buildSongList();
        mShufflePrefs = new ShufflePrefs(mContext);
    }

    private void buildSongList() {
        Log.d(TAG, "getSongList()");
        ContentResolver musicResolver = mContext.getContentResolver();
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = null;

        try {
            musicCursor = musicResolver.query(musicUri, null, selection, null, null);
            if (musicCursor != null && musicCursor.moveToFirst()) {
                int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
                int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int albumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                int yearColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.YEAR);
                int albumIdColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

                do {
                    long songId = musicCursor.getLong(idColumn);
                    String songTitle = musicCursor.getString(titleColumn);
                    String songArtist = musicCursor.getString(artistColumn);
                    String songAlbum = musicCursor.getString(albumColumn);
                    long albumId = musicCursor.getLong(albumIdColumn);
                    //TODO try to speed this up a bit
                    Song song = new Song(songId, songTitle, songArtist, songAlbum, albumId);
                    DatabaseWrapper databaseWrapper = DatabaseWrapper.getInstance(mContext);
                    int rating = databaseWrapper.getRatingForSong(song);
                    song.setRating(rating);
                    mSongs.add(song);
                } while (musicCursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (musicCursor != null) {
                musicCursor.close();
            }
        }
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

    public Song getSong() {
        Randomizer randomizer = getBestRandomizer();
        Song song = randomizer.getNextSong(mSongs);
        return song;
    }

    public boolean hasNext() {
        return !isEmpty();
    }

    public boolean isEmpty() {
        return mSongs.isEmpty();
    }


}
