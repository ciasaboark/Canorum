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

package org.ciasaboark.canorum.database.albumart;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Jonathan Nelson on 3/13/15.
 */
public class ArtworkDatabaseOpenHelper extends SQLiteOpenHelper {
    public static final String TABLE_ALBUMART = "album_art";
    public static final String TABLE_ARTISTART = "artist_art";
    private static final String DB_NAME = "albumart.sqlite";
    private static final int VERSION = 1;

    public ArtworkDatabaseOpenHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db);
    }

    private void createTable(SQLiteDatabase db) {
        String albumSql = "CREATE TABLE " + TABLE_ALBUMART + " ( " +
                Columns.ARTIST + " VARCHAR(100) NOT NULL," +
                Columns.ALBUM + " VARCHAR(100) NOT NULL," +
                Columns.ARTWORK_ALBUM_URI + " VARCHAR(200) DEFAULT \'\'," +
                Columns.ARTWORK_ALBUM_LOW_URI + " VARCHAR(200) DEFAULT \'\'," +
                Columns.TIMESTAMP + " INTEGER, " +
                "PRIMARY KEY (" +
                Columns.ARTIST + ", " +
                Columns.ALBUM + ")" +
                " )";

        String artistSql = "CREATE TABLE " + TABLE_ARTISTART + " ( " +
                Columns.ARTIST + " VARCHAR(100) NOT NULL," +
                Columns.ARTWORK_ARTIST_URI + " VARCHAR(200) DEFAULT \'\'," +
                Columns.ARTWORK_ARTIST_LOW_URI + " VARCHAR(200) DEFAULT \'\'," +
                Columns.TIMESTAMP + " INTEGER, " +
                "PRIMARY KEY (" +
                Columns.ARTIST + ")" +
                " )";

        db.execSQL(albumSql);
        db.execSQL(artistSql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALBUMART);
        createTable(db);
    }
}
