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

package org.ciasaboark.canorum.artwork.album.fetcher;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import org.ciasaboark.canorum.artwork.exception.ArtworkNotFoundException;
import org.ciasaboark.canorum.song.Album;

/**
 * Created by Jonathan Nelson on 1/29/15.
 */
public class MediaStoreFetcher {
    private static final String TAG = "SystemArtFetcher";
    private static final int ALBUM_ART_LOADER = 1;
    private final Context mContext;
    private Album mAlbum;

    public MediaStoreFetcher(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
    }

    public MediaStoreFetcher setAlbum(Album album) {
        mAlbum = album;
        return this;
    }


    public BitmapDrawable loadArtwork() throws ArtworkNotFoundException {
        BitmapDrawable artwork = null;
        String exceptionMessage = "";

        Log.d(TAG, "beginning MediaStore album art fetch");
        if (mAlbum == null) {
            throw new ArtworkNotFoundException("Can not fetch artwork for unknown album");
        }

        Uri contentUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM_ART,
                MediaStore.Audio.Albums.ARTIST
        };
        String selection = MediaStore.Audio.Albums._ID + "=?";
        String[] selectionArgs = new String[]{String.valueOf(mAlbum.getAlbumId())};

        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(contentUri, projection, selection, selectionArgs, null);
            if (cursor.moveToFirst()) {
                do {
                    String albumArtPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                    String albumArtist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                    long albumID = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));

                    Bitmap bitmap = BitmapFactory.decodeFile(albumArtPath);

                    if (bitmap == null) {
                        exceptionMessage = "could not decode album art from MediaStore, art might not exist, " +
                                "is corrupted, or is in an unsupported format";
                        Log.d(TAG, exceptionMessage);
                    } else {
                        artwork = new BitmapDrawable(mContext.getResources(), bitmap);
                    }
                } while (cursor.moveToNext());
            } else {
                exceptionMessage = "media store does not have artwork for album '" + mAlbum + "'";
                Log.d(TAG, exceptionMessage);
            }
        } catch (Exception e) {
            exceptionMessage = "could not fetch artwork for album '" + mAlbum + "' " + e.getMessage();
            Log.d(TAG, exceptionMessage);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (artwork == null) {
            throw new ArtworkNotFoundException(exceptionMessage);
        }

        return artwork;
    }
}
