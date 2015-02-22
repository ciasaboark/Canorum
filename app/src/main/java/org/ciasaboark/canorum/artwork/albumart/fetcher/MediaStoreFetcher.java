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

package org.ciasaboark.canorum.artwork.albumart.fetcher;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import org.ciasaboark.canorum.artwork.watcher.LoadingWatcher;
import org.ciasaboark.canorum.song.Album;

/**
 * Created by Jonathan Nelson on 1/29/15.
 */
public class MediaStoreFetcher implements android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "SystemArtFetcher";
    private static final int ALBUM_ART_LOADER = 1;
    private final Context mContext;
    private Album mAlbum;
    private LoadingWatcher mWatcher;
    private LoaderManager mLoaderManager;

    public MediaStoreFetcher(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
        try {
            mLoaderManager = ((ActionBarActivity) mContext).getSupportLoaderManager();
        } catch (Exception e) {
            Log.e(TAG, "error getting support loader manager from context, probably not an " +
                    "actionbar activity");
        }
    }

    public MediaStoreFetcher setLoadingWatcher(LoadingWatcher watcher) {
        mWatcher = watcher;
        return this;
    }

    public MediaStoreFetcher setAlbum(Album album) {
        mAlbum = album;
        return this;
    }


    public MediaStoreFetcher loadInBackground() {
        Log.d(TAG, "beginning MediaStore album art fetch");
        if (mAlbum == null || mWatcher == null) {
            Log.e(TAG, "will not perform load unless watcher and song are not null");
        } else {
            Bundle bundle = new Bundle();
            bundle.putLong("albumId", mAlbum.getAlbumId());
            try {
                if (mLoaderManager != null) {
                    mLoaderManager.destroyLoader(ALBUM_ART_LOADER);
                    mLoaderManager.initLoader(ALBUM_ART_LOADER, bundle, this);
                }
            } catch (Exception e) {
                Log.e(TAG, "error getting cursor loader manager from context, probably not an Activity");
            }
        }
        return this;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = null;
        switch (id) {
            case ALBUM_ART_LOADER:
                if (args == null) {
                    Log.d(TAG, "onCreateLoader() can not fetch album art without album id");
                } else {
                    long albumId = args.getLong("albumId", -1);
                    cursorLoader = new CursorLoader(
                            mContext,
                            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                            new String[]{
                                    MediaStore.Audio.Albums._ID,
                                    MediaStore.Audio.Albums.ALBUM_ART,
                                    MediaStore.Audio.Albums.ARTIST
                            },
                            MediaStore.Audio.Albums._ID + "=?",
                            new String[]{String.valueOf(albumId)},
                            null);
                }
                break;
        }
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            try {
                String albumArtPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                String albumArtist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                long albumID = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));

                Bitmap bitmap = BitmapFactory.decodeFile(albumArtPath);

                BitmapDrawable albumArt = null;

                if (bitmap == null) {
                    Log.d(TAG, "could not decode album art from MediaStore, art might not exist, " +
                            "is corrupted, or is in an unsupported format");
                } else {
                    albumArt = new BitmapDrawable(mContext.getResources(), bitmap);
                }

                mWatcher.onLoadFinished(albumArt, albumArtPath);


            } catch (Exception e) {
                Log.e(TAG, "error getting album art uri from cursor: " + e.getMessage());
                mWatcher.onLoadFinished(null, null);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
