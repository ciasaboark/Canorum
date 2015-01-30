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

package org.ciasaboark.canorum.artwork.writer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.ciasaboark.canorum.Song;
import org.ciasaboark.canorum.artwork.fetcher.FileSystemFetcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created by Jonathan Nelson on 1/29/15.
 */
public class FileSystemWriter {
    private static final String TAG = "FileSystemWriter";
    private Context mContext;

    public FileSystemWriter(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
    }

    public boolean writeArtworkToFilesystem(Song song, BitmapDrawable albumArt) {
        if (song == null) {
            throw new IllegalArgumentException("song can not be null");
        }
        if (albumArt == null) {
            throw new IllegalArgumentException("album art can not be null");
        }

        boolean fileWritten = false;
        if (isExternalStorageWritable()) {
            String fileName = song.getArtist() + "-" + song.getmAlbum();
            File f = new File(mContext.getExternalFilesDir(null), fileName);
            try {
                FileOutputStream fout = new FileOutputStream(f);
                Bitmap bitmap = albumArt.getBitmap();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fout);
                fout.flush();
                fout.close();
                Toast.makeText(mContext, "Saved album art for " + song, Toast.LENGTH_SHORT).show();
                fileWritten = true;
            } catch (Exception e) {
                Log.e(TAG, "error writing album art to disk for song " + song + " " + e.getMessage());
                Toast.makeText(mContext, "Error saving album art for " + song, Toast.LENGTH_SHORT).show();
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
