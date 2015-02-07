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

package org.ciasaboark.canorum.artwork.artist.writer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.util.Log;

import org.ciasaboark.canorum.Artist;
import org.ciasaboark.canorum.artwork.artist.ArtistArtLoader;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by Jonathan Nelson on 1/29/15.
 */
public class FileSystemWriter {
    public static final String STORAGE_DIR = "artists/";
    private static final String TAG = "FileSystemWriter";
    private Context mContext;
    private ArtistArtLoader.ArtSize mArtSize;

    public FileSystemWriter(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
    }

    public FileSystemWriter setArtSize(ArtistArtLoader.ArtSize artSize) {
        mArtSize = artSize;
        return this;
    }

    public File getFilePath() {
        return getFilePath(ArtistArtLoader.ArtSize.LARGE);
    }

    public File getFilePath(ArtistArtLoader.ArtSize artSize) {
        if (artSize == null) {
            artSize = ArtistArtLoader.ArtSize.LARGE;
        }
        File outputDir = new File(mContext.getExternalFilesDir(null), STORAGE_DIR);
        File outputSizedDir = new File(outputDir, artSize.toString() + "/");

        return outputSizedDir;
    }

    public File getFilePathForArtist(Artist artist) {
        return getFilePathForArtist(artist, ArtistArtLoader.ArtSize.LARGE);
    }

    public File getFilePathForArtist(Artist artist, ArtistArtLoader.ArtSize artSize) {
        if (artSize == null) {
            artSize = ArtistArtLoader.ArtSize.LARGE;
        }

        File outputFile = new File(getFilePath(artSize), artist.getArtistName());
        return outputFile;
    }

    public boolean writeArtworkToFilesystem(Artist artist, BitmapDrawable albumArt) {
        if (artist == null) {
            throw new IllegalArgumentException("song can not be null");
        }
        if (albumArt == null) {
            throw new IllegalArgumentException("album art can not be null");
        }

        if (mArtSize == null) {
            Log.d(TAG, "no art size given, assuming LARGE");
            mArtSize = ArtistArtLoader.ArtSize.LARGE;
        }

        boolean fileWritten = false;
        if (isExternalStorageWritable()) {
            File outputFile = getFilePathForArtist(artist, mArtSize);
            try {
                getFilePath(mArtSize).mkdirs();
                FileOutputStream fout = new FileOutputStream(outputFile);
                Bitmap bitmap = albumArt.getBitmap();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fout);
                fout.flush();
                fout.close();
                fileWritten = true;
            } catch (Exception e) {
                Log.e(TAG, "error writing artist art to disk for artist " + artist + " " + e.getMessage());
            }
        }
        return fileWritten;
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}
