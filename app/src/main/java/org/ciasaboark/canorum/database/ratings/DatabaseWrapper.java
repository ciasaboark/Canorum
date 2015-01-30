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
            Columns.RATING
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
        cv.put(Columns.ALBUM, song.getmAlbum());
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

    public int getRatingForSong(Song song) {
        if (song == null) {
            throw new IllegalArgumentException("song can not be null");
        }

        int rating = 50;
        String whereClause = Columns.TITLE + " =? AND " + Columns.ARTIST + " =? AND " + Columns.ALBUM + " =?";
        String[] args = {
                song.getTitle(),
                song.getArtist(),
                song.getmAlbum()
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

//    public int getPlayCountForSong(Song song) {
//        if (song == null) {
//            throw new IllegalArgumentException("song can not be null");
//        }
//
//        int playCount = 0;
//        String whereClause = Columns.TITLE + " =? AND " + Columns.ARTIST + " =? AND " + Columns.ALBUM + " =?";
//        String[] args = {
//                song.getTitle(),
//                song.getArtist(),
//                song.getmAlbum()
//        };
//        Cursor cursor = null;
//        try {
//            cursor = ratingsDb.query(RatingsDatabaseOpenHelper.TABLE_RATINGS, RATINGS_PROJECTION, whereClause, args, null, null, null);
//            if (cursor.moveToFirst()) {
//                playCount = cursor.getInt(cursor.getColumnIndex(Columns.PLAY_COUNT));
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "unable to query org.ciasaboark.canorum.database play count for song: " + song);
//        } finally {
//            if (cursor != null) {
//                cursor.close();
//            }
//        }
//        return playCount;
//    }

    public void setRatingForSong(Song song, int rating) {
        if (song == null) {
            throw new IllegalArgumentException("can not store rating for null Song");
        }

        //TODO this will clobber the other column values
        ContentValues cv = new ContentValues();
        cv.put(Columns.ARTIST, song.getArtist());
        cv.put(Columns.ALBUM, song.getmAlbum());
        cv.put(Columns.TITLE, song.getTitle());
        cv.put(Columns.RATING, rating);
        long rowId = ratingsDb.insertWithOnConflict(RatingsDatabaseOpenHelper.TABLE_RATINGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        Log.d(TAG, "set rating for song '" + song + "' to: " + rating);
    }


}
