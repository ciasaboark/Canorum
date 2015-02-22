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

package org.ciasaboark.canorum.artwork.artist.fetcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.Log;

import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.watcher.LoadingWatcher;
import org.ciasaboark.canorum.artwork.writer.FileSystemWriter;
import org.ciasaboark.canorum.song.Artist;

import java.io.File;

/**
 * Created by Jonathan Nelson on 1/29/15.
 */
public class FileSystemFetcher {
    private static final String TAG = "FileSystemFetcher";
    private Context mContext;
    private Artist mArtist;
    private LoadingWatcher mWatcher;
    private ArtSize mArtSize;

    public FileSystemFetcher(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
    }

    public FileSystemFetcher setArtLoadedWatcher(LoadingWatcher watcher) {
        mWatcher = watcher;
        return this;
    }

    public FileSystemFetcher setArtist(Artist artist) {
        mArtist = artist;
        return this;
    }

    public FileSystemFetcher setArtSize(ArtSize artSize) {
        mArtSize = artSize;
        return this;
    }

    public FileSystemFetcher loadInBackground() {
        Log.d(TAG, "(" + mArtist + ") beginning file system artist art fetch");
        if (mArtSize == null) {
            Log.d(TAG, "no art size given, assuming large size");
            mArtSize = ArtSize.LARGE;
        }

        if (mWatcher == null || mArtist == null) {
            Log.d(TAG, "will not load artist art until song and watcher are given");
        } else {
            FileSystemWriter writer = new FileSystemWriter(mContext);
            File inputFile = writer.getFilePathForTypeAndSizeAndFilename(FileSystemWriter.ART_TYPE.ARTIST, mArtSize, mArtist);

            FileFetcher fileFetcher = new FileFetcher();
            fileFetcher.execute(inputFile);
        }
        return this;
    }

    private class FileFetcher extends AsyncTask<File, Void, BitmapDrawable> {
        String filePath;

        @Override
        protected BitmapDrawable doInBackground(File... files) {
            File f = files[0];
            String path = f.getAbsolutePath();
            filePath = path;
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            BitmapDrawable d = null;
            if (bitmap != null) {
                d = new BitmapDrawable(bitmap);
            }
            return d;
        }

        @Override
        protected void onPostExecute(BitmapDrawable result) {
            mWatcher.onLoadFinished(result, filePath);
        }
    }
}
