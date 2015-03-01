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

import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.song.Artist;
import org.ciasaboark.canorum.song.extended.ExtendedAlbum;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    public File getFilePathForType(ART_TYPE type) {
        return getFilePathForTypeAndSize(type, ArtSize.LARGE);
    }

    public File getFilePathForTypeAndSize(ART_TYPE type, ArtSize size) {
        if (size == null) {
            size = ArtSize.LARGE;
        }

        File outputDir = new File(mContext.getExternalFilesDir(null), type.directory);
        File outputSizedDir = new File(outputDir, size.toString() + "/");

        return outputSizedDir;
    }

    public boolean writeArtworkToFileSystem(Artist artist, BitmapDrawable artistArt, ArtSize artSize) {
        if (artist == null || artistArt == null || artSize == null) {
            Log.d(TAG, "will not write artist artwork to disk with null parameters");
            return false;
        } else {
            String filename = getFileName(artist);
            if (filename != null) {
                return writeArtworkToFileSystem(ART_TYPE.ARTIST, artSize, filename, artistArt);
            } else {
                return false;
            }
        }
    }

    public String getFileName(Artist artist) {
        return stringToSHA256(artist.getArtistName());
    }

    private boolean writeArtworkToFileSystem(ART_TYPE type, ArtSize artSize, String fileName, BitmapDrawable artwork) {
        if (artSize == null) {
            Log.d(TAG, "no art size given, assuming LARGE");
            artSize = ArtSize.LARGE;
        }

        boolean fileWritten = false;
        if (isExternalStorageWritable()) {
            File outputFile = getFilePathForTypeAndSizeAndFilename(type, artSize, fileName);
            try {
                getFilePathForTypeAndSize(type, artSize).mkdirs();
                FileOutputStream fout = new FileOutputStream(outputFile);
                Bitmap bitmap = artwork.getBitmap();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fout);
                fout.flush();
                fout.close();
                fileWritten = true;
            } catch (Exception e) {
                Log.e(TAG, "error writing art to disk for file " + fileName + " of type " +
                        type + " " + e.getMessage());
            }
        }
        return fileWritten;
    }

    private String stringToSHA256(String inputString) {
        String sha1 = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(inputString.getBytes("UTF-8"), 0, inputString.length());
            sha1 = new BigInteger(1, messageDigest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "unable to get MD5 algorithm from MessageDigest");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "unable to convert input string \"" + inputString + "\" to UTF-8 byte array");
        }
        return sha1;
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private File getFilePathForTypeAndSizeAndFilename(ART_TYPE type, ArtSize size, String filename) {
        File outputDir = getFilePathForTypeAndSize(type, size);
        File outputFile = new File(outputDir, filename);
        return outputFile;
    }

    private String stringToMD5(String inputString) {
        String md5 = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(inputString.getBytes("UTF-8"), 0, inputString.length());
            md5 = new BigInteger(1, messageDigest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "unable to get MD5 algorithm from MessageDigest");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "unable to convert input string \"" + inputString + "\" to UTF-8 byte array");
        }
        return md5;
    }

    public boolean writeArtworkToFileSystem(ExtendedAlbum album, BitmapDrawable albumArt, ArtSize artSize) {
        if (album == null || albumArt == null || artSize == null) {
            Log.d(TAG, "will not write album artwork to disk with null parameters");
            return false;
        } else {
            String filename = getFileName(album);
            if (filename != null) {
                return writeArtworkToFileSystem(ART_TYPE.ALBUM, artSize, filename, albumArt);
            } else {
                return false;
            }
        }
    }

    public String getFileName(ExtendedAlbum album) {
        return stringToSHA256(album.getArtistName() + album.getAlbumName());
    }

    public File getFilePathForTypeAndSizeAndFilename(ART_TYPE type, ArtSize size, Artist artist) {
        String filename = stringToSHA256(artist.getArtistName());
        return getFilePathForTypeAndSizeAndFilename(type, size, filename);
    }

    public File getFilePathForTypeAndSizeAndFilename(ART_TYPE type, ArtSize size, ExtendedAlbum album) {
        String filename = stringToSHA256(album.getArtistName() + album.getAlbumName());
        return getFilePathForTypeAndSizeAndFilename(type, size, filename);
    }


    public enum ART_TYPE {
        ARTIST("artist"),
        ALBUM("album");

        final String directory;

        ART_TYPE(String dir) {
            this.directory = dir;
        }
    }
}
