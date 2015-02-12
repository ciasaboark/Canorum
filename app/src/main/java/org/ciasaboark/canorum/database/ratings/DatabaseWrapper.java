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

import org.ciasaboark.canorum.song.Track;

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

    private boolean insertTrackInDatabase(Track track) {
        ContentValues cv = new ContentValues();
        cv.put(Columns.ARTIST, track.getArtist().getArtistName());
        cv.put(Columns.ALBUM, track.getAlbum().getAlbumName());
        cv.put(Columns.TITLE, track.getSong().getTitle());
        long rowId = ratingsDb.insertWithOnConflict(RatingsDatabaseOpenHelper.TABLE_RATINGS, null, cv, SQLiteDatabase.CONFLICT_ROLLBACK);
        boolean trackInserted = rowId != -1;
        if (trackInserted) {
            Log.d(TAG, "inserted track " + track + " with row id: " + rowId);
        } else {
            Log.e(TAG, "insert of track " + track + " failed with code " + rowId);
        }
        return trackInserted;
    }

//    private Track getTrack(String artist, String album, String title) {
//        Track track = null;
//        String whereClause = Columns.TITLE + " =? AND " + Columns.ARTIST + " =? AND " + Columns.ALBUM + " =?";
//        String[] args = {
//                title,
//                artist,
//                album
//        };
//
//        Cursor cursor = null;
//        try {
//            cursor = ratingsDb.query(RatingsDatabaseOpenHelper.TABLE_RATINGS, RATINGS_PROJECTION, whereClause, args, null, null, null);
//            if (cursor.moveToFirst()) {
//                int rating = cursor.getInt(cursor.getColumnIndex(Columns.RATING));
//                int playcount = cursor.getInt(cursor.getColumnIndex(Columns.PLAY_COUNT));
//                long timestamp = cursor.getLong(cursor.getColumnIndex(Columns.TIMESTAMP));
//                track = new ExtendedSong(-1, title, artist, album, -1, rating, playcount, timestamp);
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "unable to query database rating for track: " + track);
//        } finally {
//            if (cursor != null) {
//                cursor.close();
//            }
//        }
//
//        return track;
//    }

    public void setRatingForTrack(Track track, int rating) {
        if (track == null) {
            throw new IllegalArgumentException("can not store rating for null Track");
        }

        //TODO this will clobber the other column values
        ContentValues cv = new ContentValues();
        cv.put(Columns.ARTIST, track.getArtist().getArtistName());
        cv.put(Columns.ALBUM, track.getAlbum().getAlbumName());
        cv.put(Columns.TITLE, track.getSong().getTitle());
        cv.put(Columns.RATING, rating);
        cv.put(Columns.PLAY_COUNT, getPlaycountForTrack(track));
        cv.put(Columns.TIMESTAMP, String.valueOf(System.currentTimeMillis()));

        try {
            long rowId = ratingsDb.insertWithOnConflict(RatingsDatabaseOpenHelper.TABLE_RATINGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            Log.d(TAG, "set rating for track (row) " + rowId + " '" + track + "' to: " + rating);
        } catch (Exception e) {
            Log.e(TAG, "unable to set new rating for track '" + track + "' " + e.getMessage());
        }
    }

    public int getPlaycountForTrack(Track track) {
        if (track == null) {
            throw new IllegalArgumentException("track can not be null");
        }

        int playcount = 0;
        String whereClause = Columns.TITLE + " =? AND " + Columns.ARTIST + " =? AND " + Columns.ALBUM + " =?";
        String[] args = {
                track.getSong().getTitle(),
                track.getArtist().getArtistName(),
                track.getAlbum().getAlbumName()
        };
        Cursor cursor = null;
        try {
            cursor = ratingsDb.query(RatingsDatabaseOpenHelper.TABLE_RATINGS, RATINGS_PROJECTION, whereClause, args, null, null, null);
            if (cursor.moveToFirst()) {
                playcount = cursor.getInt(cursor.getColumnIndex(Columns.PLAY_COUNT));
            }
        } catch (Exception e) {
            Log.e(TAG, "unable to query database rating for track: " + track);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return playcount;
    }

    public void incrementPlayCountForTrack(Track track) {
        if (track == null) {
            throw new IllegalArgumentException("track can not be null");
        }

        ContentValues cv = new ContentValues();
        cv.put(Columns.ARTIST, track.getArtist().getArtistName());
        cv.put(Columns.ALBUM, track.getAlbum().getAlbumName());
        cv.put(Columns.TITLE, track.getSong().getTitle());
        cv.put(Columns.TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        cv.put(Columns.RATING, getRatingForTrack(track));
        int newPlayCount = getPlaycountForTrack(track) + 1;
        cv.put(Columns.PLAY_COUNT, newPlayCount);

        try {
            long rowId = ratingsDb.insertWithOnConflict(RatingsDatabaseOpenHelper.TABLE_RATINGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            Log.d(TAG, "updated play count for track (row) " + rowId + " '" + track + "' to " + newPlayCount);
        } catch (Exception e) {
            Log.e(TAG, "unable to set new play count for track '" + track + "' " + e.getMessage());
        }
    }

    public int getRatingForTrack(Track track) {
        if (track == null) {
            throw new IllegalArgumentException("track can not be null");
        }

        int rating = 50;    //default track rating
        String whereClause = Columns.TITLE + " =? AND " + Columns.ARTIST + " =? AND " + Columns.ALBUM + " =?";
        String[] args = {
                track.getSong().getTitle(),
                track.getArtist().getArtistName(),
                track.getAlbum().getAlbumName()
        };
        Cursor cursor = null;
        try {
            cursor = ratingsDb.query(RatingsDatabaseOpenHelper.TABLE_RATINGS, RATINGS_PROJECTION, whereClause, args, null, null, null);
            if (cursor.moveToFirst()) {
                rating = cursor.getInt(cursor.getColumnIndex(Columns.RATING));
            }
        } catch (Exception e) {
            Log.e(TAG, "unable to query database rating for track: " + track);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return rating;
    }
}
