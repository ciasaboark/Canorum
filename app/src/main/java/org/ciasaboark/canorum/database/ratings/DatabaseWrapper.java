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

package org.ciasaboark.canorum.database.ratings;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.ciasaboark.canorum.Song;

/**
 * Created by Jonathan Nelson on 1/23/15.
 */
public class DatabaseWrapper {
    private static final String TAG = "DatabaseWrapper";
    private static final String[] RATINGS_PROJECTION = {
            Columns.ARTIST,
            Columns.ALBUM,
            Columns.TITLE,
            Columns.PLAY_COUNT,
            Columns.RATING,
            Columns.TIMESTAMP
    };

    private static Context mContext = null;
    private static DatabaseWrapper instance = null;
    private static SQLiteDatabase ratingsDb = null;

    private DatabaseWrapper(Context ctx) {
        if (mContext == null) {
            mContext = ctx;
        }
        RatingsDatabaseOpenHelper dbHelper = new RatingsDatabaseOpenHelper(mContext);
        if (ratingsDb == null) {
            ratingsDb = dbHelper.getWritableDatabase();
        }
    }

    public static DatabaseWrapper getInstance(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        if (instance == null) {
            instance = new DatabaseWrapper(ctx);
        }
        return instance;
    }

//    public void incrementPlayCount(Song song) {
//        if (!isSongInDatabase(song)) {
//            insertSongInDatabase(song);
//        }
//
//    }

//    private boolean isSongInDatabase(Song song) {
//        return false;
//    }

    private boolean insertSongInDatabase(Song song) {
        ContentValues cv = new ContentValues();
        cv.put(Columns.ARTIST, song.getArtist());
        cv.put(Columns.ALBUM, song.getAlbum());
        cv.put(Columns.TITLE, song.getTitle());
        long rowId = ratingsDb.insertWithOnConflict(RatingsDatabaseOpenHelper.TABLE_RATINGS, null, cv, SQLiteDatabase.CONFLICT_ROLLBACK);
        boolean songInserted = rowId != -1;
        if (songInserted) {
            Log.d(TAG, "inserted song " + song + " with row id: " + rowId);
        } else {
            Log.e(TAG, "insert of song " + song + " failed with code " + rowId);
        }
        return songInserted;
    }

    private Song getSong(String artist, String album, String title) {
        return (Song) getExtendedSong(artist, album, title);
    }

    private ExtendedSong getExtendedSong(String artist, String album, String title) {
        ExtendedSong song = null;
        String whereClause = Columns.TITLE + " =? AND " + Columns.ARTIST + " =? AND " + Columns.ALBUM + " =?";
        String[] args = {
                title,
                artist,
                album
        };

        Cursor cursor = null;
        try {
            cursor = ratingsDb.query(RatingsDatabaseOpenHelper.TABLE_RATINGS, RATINGS_PROJECTION, whereClause, args, null, null, null);
            if (cursor.moveToFirst()) {
                int rating = cursor.getInt(cursor.getColumnIndex(Columns.RATING));
                int playcount = cursor.getInt(cursor.getColumnIndex(Columns.PLAY_COUNT));
                long timestamp = cursor.getLong(cursor.getColumnIndex(Columns.TIMESTAMP));
                song = new ExtendedSong(-1, title, artist, album, -1, rating, playcount, timestamp);
            }
        } catch (Exception e) {
            Log.e(TAG, "unable to query database rating for song: " + song);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return song;
    }

    public void setRatingForSong(Song song, int rating) {
        if (song == null) {
            throw new IllegalArgumentException("can not store rating for null Song");
        }
        ExtendedSong curSong = getExtendedSong(song.getArtist(), song.getAlbum(), song.getTitle());

        //TODO this will clobber the other column values
        ContentValues cv = new ContentValues();
        cv.put(Columns.ARTIST, song.getArtist());
        cv.put(Columns.ALBUM, song.getAlbum());
        cv.put(Columns.TITLE, song.getTitle());
        cv.put(Columns.RATING, rating);
        cv.put(Columns.PLAY_COUNT, getPlaycountForSong(song));
        cv.put(Columns.TIMESTAMP, String.valueOf(System.currentTimeMillis()));

        try {
            long rowId = ratingsDb.insertWithOnConflict(RatingsDatabaseOpenHelper.TABLE_RATINGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            Log.d(TAG, "set rating for song (row) " + rowId + " '" + song + "' to: " + rating);
        } catch (Exception e) {
            Log.e(TAG, "unable to set new rating for song '" + song + "' " + e.getMessage());
        }
    }

    public int getPlaycountForSong(Song song) {
        if (song == null) {
            throw new IllegalArgumentException("song can not be null");
        }

        int playcount = 0;
        String whereClause = Columns.TITLE + " =? AND " + Columns.ARTIST + " =? AND " + Columns.ALBUM + " =?";
        String[] args = {
                song.getTitle(),
                song.getArtist(),
                song.getAlbum()
        };
        Cursor cursor = null;
        try {
            cursor = ratingsDb.query(RatingsDatabaseOpenHelper.TABLE_RATINGS, RATINGS_PROJECTION, whereClause, args, null, null, null);
            if (cursor.moveToFirst()) {
                playcount = cursor.getInt(cursor.getColumnIndex(Columns.PLAY_COUNT));
            }
        } catch (Exception e) {
            Log.e(TAG, "unable to query database rating for song: " + song);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return playcount;
    }

    public void incrementPlayCountForSong(Song song) {
        if (song == null) {
            throw new IllegalArgumentException("song can not be null");
        }

        ContentValues cv = new ContentValues();
        cv.put(Columns.ARTIST, song.getArtist());
        cv.put(Columns.ALBUM, song.getAlbum());
        cv.put(Columns.TITLE, song.getTitle());
        cv.put(Columns.TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        cv.put(Columns.RATING, getRatingForSong(song));
        int newPlayCount = getPlaycountForSong(song) + 1;
        cv.put(Columns.PLAY_COUNT, newPlayCount);

        try {
            long rowId = ratingsDb.insertWithOnConflict(RatingsDatabaseOpenHelper.TABLE_RATINGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            Log.d(TAG, "updated play count for song (row) " + rowId + " '" + song + "' to " + newPlayCount);
        } catch (Exception e) {
            Log.e(TAG, "unable to set new play count for song '" + song + "' " + e.getMessage());
        }
    }

    public int getRatingForSong(Song song) {
        if (song == null) {
            throw new IllegalArgumentException("song can not be null");
        }

        int rating = 50;
        String whereClause = Columns.TITLE + " =? AND " + Columns.ARTIST + " =? AND " + Columns.ALBUM + " =?";
        String[] args = {
                song.getTitle(),
                song.getArtist(),
                song.getAlbum()
        };
        Cursor cursor = null;
        try {
            cursor = ratingsDb.query(RatingsDatabaseOpenHelper.TABLE_RATINGS, RATINGS_PROJECTION, whereClause, args, null, null, null);
            if (cursor.moveToFirst()) {
                rating = cursor.getInt(cursor.getColumnIndex(Columns.RATING));
            }
        } catch (Exception e) {
            Log.e(TAG, "unable to query database rating for song: " + song);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return rating;
    }

    private class ExtendedSong extends Song {
        private final int mPlayCount;
        private final long mTimestamp;

        public ExtendedSong(long id, String title, String artist, String album, long albumId, int rating, int playCount, long timestamp) {
            super(id, title, artist, album, albumId, rating);
            mPlayCount = playCount;
            mTimestamp = timestamp;
        }

        public int getPlayCount() {
            return mPlayCount;
        }

        public long getTimeStamp() {
            return mTimestamp;
        }
    }


}
