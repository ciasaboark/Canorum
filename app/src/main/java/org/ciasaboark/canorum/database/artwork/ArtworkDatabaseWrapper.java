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

package org.ciasaboark.canorum.database.artwork;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.ciasaboark.canorum.song.Artist;
import org.ciasaboark.canorum.song.extended.ExtendedAlbum;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by Jonathan Nelson on 1/29/15.
 */
public class ArtworkDatabaseWrapper {
    private static final String TAG = "DatabaseWrapper";
    private static final String[] ALBUM_PROJECTION = {
            Columns.ARTIST,
            Columns.ALBUM,
            Columns.ARTWORK_ALBUM_URI,
            Columns.ARTWORK_ALBUM_LOW_URI,
            Columns.TIMESTAMP,
    };

    private static final String[] ARTIST_PROJECTION = {
            Columns.ARTIST,
            Columns.ARTWORK_ARTIST_URI,
            Columns.ARTWORK_ARTIST_LOW_URI,
            Columns.TIMESTAMP
    };

    private static Context mContext = null;
    private static ArtworkDatabaseWrapper instance = null;
    private static SQLiteDatabase artworkDb = null;
    private MaxSizeHashMap<Artist, Artwork> artistArtCache;
    private MaxSizeHashMap<ExtendedAlbum, Artwork> albumArtCache;

    private ArtworkDatabaseWrapper(Context ctx) {
        if (mContext == null) {
            mContext = ctx;
        }
        ArtworkDatabaseOpenHelper dbHelper = new ArtworkDatabaseOpenHelper(mContext);
        if (artworkDb == null) {
            artworkDb = dbHelper.getWritableDatabase();
        }
        artistArtCache = new MaxSizeHashMap<Artist, Artwork>(10);
        albumArtCache = new MaxSizeHashMap<ExtendedAlbum, Artwork>(10);
    }

    public static ArtworkDatabaseWrapper getInstance(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        if (instance == null) {
            instance = new ArtworkDatabaseWrapper(ctx);
        }
        return instance;
    }

    public boolean setArtworkForAlbum(ExtendedAlbum album, String artworkUri, ARTWORK_QUALITY quality) {
        if (album == null) {
            throw new IllegalArgumentException("can not insert values for null album");
        }
        if (quality == null) {
            throw new IllegalArgumentException("unknown quality");
        }
        removeFromCache(album);

        boolean storeHighQualityUri = quality == ARTWORK_QUALITY.HIGH_QUALITY;

        artworkUri = artworkUri == null ? "" : artworkUri;

        String artistName = album.getArtistName();
        String albumName = album.getAlbumName();
        long timeStamp = System.currentTimeMillis();

        Artwork prevArtwork = getArtwork(album);
        ContentValues cv = new ContentValues();
        cv.put(Columns.ARTIST, artistName);
        cv.put(Columns.ALBUM, albumName);
        if (storeHighQualityUri) {
            cv.put(Columns.ARTWORK_ALBUM_URI, artworkUri);
            if (prevArtwork != null)
                cv.put(Columns.ARTWORK_ALBUM_LOW_URI, prevArtwork.getLowQualityUri());
        } else {
            cv.put(Columns.ARTWORK_ALBUM_LOW_URI, artworkUri);
            if (prevArtwork != null)
                cv.put(Columns.ARTWORK_ALBUM_URI, prevArtwork.getHighQualityUri());
        }

        cv.put(Columns.TIMESTAMP, timeStamp);
        long rowId = artworkDb.insertWithOnConflict(ArtworkDatabaseOpenHelper.TABLE_ALBUMART, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        boolean artworkUpdated = rowId != -1;
        if (artworkUpdated) {
            Log.d(TAG, "updated album artwork for '" + album + "' with row id: " + rowId);
        } else {
            Log.e(TAG, "update of album artwork for '" + album + "' failed");
        }
        return artworkUpdated;
    }

    private void removeFromCache(ExtendedAlbum album) {
        if (albumArtCache.containsKey(album)) {
            albumArtCache.remove(album);
        }
    }

    private Artwork getArtwork(ExtendedAlbum album) {
        Artwork artwork = albumArtCache.get(album);
        if (artwork == null) {
            artwork = getArtworkAndCache(album);
        }

        return artwork;
    }

    private Artwork getArtworkAndCache(ExtendedAlbum album) {
        if (album == null) {
            throw new IllegalArgumentException("can not query for null album");
        }

        String whereClause = Columns.ARTIST + " = ? AND " + Columns.ALBUM + " = ? ";
        String[] whereArgs = {
                album.getArtistName(),
                album.getAlbumName()
        };
        Cursor cursor = null;
        Artwork artwork = null;

        try {
            cursor = artworkDb.query(ArtworkDatabaseOpenHelper.TABLE_ALBUMART,
                    ALBUM_PROJECTION, whereClause, whereArgs, null, null, null);
            if (cursor.moveToFirst()) {
                String artistName = cursor.getString(cursor.getColumnIndex(Columns.ARTIST));
                String albumName = cursor.getString(cursor.getColumnIndex(Columns.ALBUM));
                String highQualityUri = cursor.getString(cursor.getColumnIndex(Columns.ARTWORK_ARTIST_URI));
                String lowQualityUri = cursor.getString(cursor.getColumnIndex(Columns.ARTWORK_ALBUM_LOW_URI));
                long timestamp = cursor.getLong(cursor.getColumnIndex(Columns.TIMESTAMP));
                artwork = new Artwork(highQualityUri, lowQualityUri, timestamp);
            }
        } catch (Exception e) {
            Log.e(TAG, "caught exception while reading timestamp from artist table for '" +
                    album + "': " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (artwork != null) {
            albumArtCache.put(album, artwork);
        }

        return artwork;
    }

    public boolean setArtworkForArtist(Artist artist, String artworkUri, ARTWORK_QUALITY quality) {
        if (artist == null) {
            throw new IllegalArgumentException("can not insert values for null artist");
        }
        if (quality == null) {
            throw new IllegalArgumentException("unknown quality");
        }

        removeFromCache(artist);

        boolean storeHighQualityUri = quality == ARTWORK_QUALITY.HIGH_QUALITY;

        artworkUri = artworkUri == null ? "" : artworkUri;

        String artistName = artist.getArtistName();
        long timeStamp = System.currentTimeMillis();
        Artwork prevArtwork = getArtwork(artist);

        ContentValues cv = new ContentValues();
        cv.put(Columns.ARTIST, artistName);
        if (storeHighQualityUri) {
            cv.put(Columns.ARTWORK_ARTIST_URI, artworkUri);
            if (prevArtwork != null)
                cv.put(Columns.ARTWORK_ARTIST_LOW_URI, prevArtwork.getLowQualityUri());
        } else {
            cv.put(Columns.ARTWORK_ARTIST_LOW_URI, artworkUri);
            if (prevArtwork != null)
                cv.put(Columns.ARTWORK_ARTIST_URI, prevArtwork.getHighQualityUri());
        }

        cv.put(Columns.TIMESTAMP, timeStamp);
        long rowId = artworkDb.insertWithOnConflict(ArtworkDatabaseOpenHelper.TABLE_ARTISTART, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        boolean artworkUpdated = rowId != -1;
        if (artworkUpdated) {
            Log.d(TAG, "updated artist artwork for '" + artist + "' with row id: " + rowId);
        } else {
            Log.e(TAG, "update of artist artwork for '" + artist + "' failed");
        }
        return artworkUpdated;
    }

    private void removeFromCache(Artist artist) {
        if (artistArtCache.containsKey(artist)) {
            artistArtCache.remove(artist);
        }
    }

    private Artwork getArtwork(Artist artist) {
        Artwork artwork = artistArtCache.get(artist);
        if (artwork == null) {
            artwork = getArtworkAndCache(artist);
        }

        return artwork;
    }

    private Artwork getArtworkAndCache(Artist artist) {
        if (artist == null) {
            throw new IllegalArgumentException("can not query for null artist");
        }

        String whereClause = Columns.ARTIST + " = ? ";
        String[] whereArgs = {
                artist.getArtistName()
        };
        Cursor cursor = null;
        Artwork artwork = null;

        try {
            cursor = artworkDb.query(ArtworkDatabaseOpenHelper.TABLE_ARTISTART,
                    ARTIST_PROJECTION, whereClause, whereArgs, null, null, null);
            if (cursor.moveToFirst()) {
                String artistName = cursor.getString(cursor.getColumnIndex(Columns.ARTIST));
                String highQualityUri = cursor.getString(cursor.getColumnIndex(Columns.ARTWORK_ARTIST_URI));
                String lowQualityUri = cursor.getString(cursor.getColumnIndex(Columns.ARTWORK_ARTIST_LOW_URI));
                long timestamp = cursor.getLong(cursor.getColumnIndex(Columns.TIMESTAMP));
                artwork = new Artwork(highQualityUri, lowQualityUri, timestamp);
            }
        } catch (Exception e) {
            Log.e(TAG, "caught exception while reading timestamp from artist table for '" +
                    artist + "': " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (artwork != null) {
            artistArtCache.put(artist, artwork);
        }

        return artwork;
    }

    public boolean isArtworkOutdated(Artist artist) {
        boolean artworkOutdated = true;
        Artwork artwork = getArtwork(artist);

        if (artwork != null) {
            long timestamp = artwork.getTimestamp();
            long curTime = System.currentTimeMillis();
            long timeDiffMs = curTime - timestamp;
            if (timeDiffMs >= 0) {
                final long MS_IN_SECOND = 1000;
                final long MS_IN_DAY = MS_IN_SECOND * 60 * 60 * 24;
                final long MS_IN_WEEK = MS_IN_DAY * 7;
                final long MS_IN_THREE_WEEKS = MS_IN_WEEK * 3;
                //to avoid having all artist images refresh at the same time we will pick a random
                // time between one and three weeks
                Random rand = new Random();
                double randomSeed = Math.random();
                long upper = MS_IN_THREE_WEEKS;
                long lower = MS_IN_WEEK;
                long difference = ((MS_IN_THREE_WEEKS - MS_IN_WEEK) + 1l);
                long randomDifference = (long) (randomSeed * difference);
                long randomTimestamp = lower + randomDifference;

                if (timeDiffMs < randomTimestamp) {
                    artworkOutdated = false;
                }
            }
        }

        return artworkOutdated;
    }

    public boolean isArtworkOutdated(ExtendedAlbum album) {
        //TODO?
        return false;
    }

    public String getArtworkUri(Artist artist, ARTWORK_QUALITY quality) {
        if (artist == null) {
            throw new IllegalArgumentException("can not query for null artist");
        }
        if (quality == null) {
            throw new IllegalArgumentException("unknown quality");
        }

        Artwork artwork = getArtwork(artist);
        String artworkUri = null;
        if (artwork != null) {
            switch (quality) {
                case HIGH_QUALITY:
                    artworkUri = artwork.getHighQualityUri();
                    break;
                case LOW_QUALITY:
                    artworkUri = artwork.getLowQualityUri();
            }
        }

        return artworkUri;
    }

    public enum ARTWORK_QUALITY {
        LOW_QUALITY,
        HIGH_QUALITY
    }

    private class Artwork {
        private final long timestamp;
        private final String highQualityUri;
        private final String lowQualityUri;

        public Artwork(String highQualityUri, String lowQualityUri, long timestamp) {
            this.highQualityUri = highQualityUri;
            this.lowQualityUri = lowQualityUri;
            this.timestamp = timestamp;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getHighQualityUri() {
            return highQualityUri;
        }

        public String getLowQualityUri() {
            return lowQualityUri;
        }

    }


    public class MaxSizeHashMap<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        public MaxSizeHashMap(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }
}
