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

package org.ciasaboark.canorum.artwork.artist;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.Display;

import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.Util;
import org.ciasaboark.canorum.artwork.artist.fetcher.FileSystemArtistFetcher;
import org.ciasaboark.canorum.artwork.artist.fetcher.LastFmImageArtistFetcher;
import org.ciasaboark.canorum.artwork.exception.ArtworkNotFoundException;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.artwork.writer.FileSystemWriter;
import org.ciasaboark.canorum.database.artwork.ArtworkDatabaseWrapper;
import org.ciasaboark.canorum.prefs.ArtworkPrefs;
import org.ciasaboark.canorum.song.Artist;


/**
 * Created by Jonathan Nelson on 1/29/15.
 */
public class ArtistArtLoader {
    private static final String TAG = "ArtistArtLoader";
    private final BitmapDrawable mDefaultArtwork;
    private Activity mContext;
    private ArtLoadedWatcher mWatcher;
    private Artist mArtist;
    private ArtSize mArtSize = null;
    private PaletteGeneratedWatcher mPalletGeneratedWatcher;
    private boolean mIsInternetSearchEnabled = false;
    private Object mTag;
    private boolean mProvideDefaultArtwork;

    public ArtistArtLoader(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        if (!(ctx instanceof Activity)) {
            throw new IllegalArgumentException(TAG + " must be called with an activity context");
        }
        mContext = (Activity) ctx;
        mDefaultArtwork = (BitmapDrawable) mContext.getResources().getDrawable(R.drawable.default_background);
    }

    public ArtistArtLoader setTag(Object tag) {
        mTag = tag;
        return this;
    }

    public ArtistArtLoader setProvideDefaultArtwork(boolean provideDefaultArtwork) {
        mProvideDefaultArtwork = provideDefaultArtwork;
        return this;
    }

    public ArtistArtLoader setInternetSearchEnabled(boolean isInternetSearchEnabled) {
        mIsInternetSearchEnabled = isInternetSearchEnabled;
        return this;
    }

    public ArtistArtLoader setArtist(Artist artist) {
        mArtist = artist;
        return this;
    }

    public ArtistArtLoader setArtLoadedWatcher(ArtLoadedWatcher watcher) {
        mWatcher = watcher;
        return this;
    }

    public ArtistArtLoader setArtSize(ArtSize size) {
        mArtSize = size;
        return this;
    }

    public ArtistArtLoader setPaletteGeneratedWatcher(PaletteGeneratedWatcher watcher) {
        mPalletGeneratedWatcher = watcher;
        return this;
    }

    public ArtistArtLoader loadInBackground() {
        if (mArtist == null || mWatcher == null) {
            Log.d(TAG, "will not load artwork until a song and watcher have been given");
        } else {
            Log.d(TAG, "(" + mArtist + ") beginning search for artist art with sdcard");
            checkArtistAndBeginLoading();
        }
        return this;
    }

    private void checkArtistAndBeginLoading() {
        if (!Util.isArtistValid(mArtist)) {
            Log.d(TAG, "(" + mArtist + ") will not begin search with invalid artist");
        } else {
            BackgroundLoader backgroundLoader = new BackgroundLoader();
            backgroundLoader.execute(mArtist);
        }
    }

    private void saveHighAndLowQualityVersions(Bitmap bitmap) {
        if (bitmap == null) throw new AssertionError("bitmap can not be null");
        generateAndSaveLowQualityVersion(bitmap);
        generateAndSaveHighQualityVersion(bitmap);
    }

    private void generateAndSaveLowQualityVersion(Bitmap bitmap) {
        Bitmap scaledBitmap = resizeBitmap(bitmap, 150);
        saveBitmap(bitmap, ArtSize.SMALL);
    }

    private void generateAndSaveHighQualityVersion(Bitmap bitmap) {
        Display display = mContext.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        //TODO should we store by the largest dimension (for better landscape support)?

        Bitmap scaledBitmap = resizeBitmap(bitmap, width);
        saveBitmap(bitmap, ArtSize.LARGE);
    }

    /**
     * Scale the bitmap so that the largest dimension is at most maxDimension.
     *
     * @param bitmap
     * @param maxDimension
     * @return The scaled bitmap if the largest edge is larger than maxDimension, or
     * the unmodified Bitmap if both width and height are less than maxDimension
     */
    private Bitmap resizeBitmap(Bitmap bitmap, int maxDimension) {
        if (bitmap == null) throw new AssertionError("can not resize null bitmap");
        if (maxDimension <= 0) throw new AssertionError("can not resize bitmap to less than 1px");

        //resize the bitmap so that the shortest side is 150px
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        boolean resizeWidth = width >= height ? true : false;
        Integer newWidth = null;
        Integer newHeight = null;
        if (resizeWidth) {
            if (width <= maxDimension) {
                Log.d(TAG, "will not resize image, width is <= max small width");
            } else {
                float scale = (float) maxDimension / (float) width;
                newWidth = maxDimension;
                newHeight = (int) (height * scale);
            }
        } else {
            if (height <= maxDimension) {
                Log.d(TAG, "will not resize image, height is <= max small height");
            } else {
                float scale = (float) maxDimension / (float) height;
                newHeight = maxDimension;
                newWidth = (int) (width * scale);
            }
        }

        if (newWidth == null || newHeight == null) {
            Log.d(TAG, "skilling image resize");
            return bitmap;
        } else {
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            return scaledBitmap;
        }
    }

    private void saveBitmap(Bitmap bitmap, ArtSize size) {
        if (bitmap == null) throw new AssertionError("bitmap can not be null");
        if (size == null) throw new AssertionError("size can not be null");

        Log.d(TAG, "(" + mArtist + ") saving bitmap to disk");
        FileSystemWriter fileSystemWriter = new FileSystemWriter(mContext);
        boolean fileWritten = fileSystemWriter.writeArtworkToFileSystem(mArtist, new BitmapDrawable(bitmap), size); //TODO unwrap the bitmapdrawable
    }

    private void sendArtwork(final Drawable drawable) {
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWatcher.onArtLoaded(drawable, mTag);
            }
        });
        loadPaletteColors(drawable);
    }

    private void loadPaletteColors(Drawable artwork) {
        Bitmap bitmap = null;
        try {
            bitmap = ((BitmapDrawable) artwork).getBitmap();
            Palette.generateAsync(bitmap,
                    new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(final Palette palette) {
                            if (mPalletGeneratedWatcher != null) {
                                mContext.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mPalletGeneratedWatcher.onPaletteGenerated(palette);
                                    }
                                });
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "could not generate palette from artwork Drawable: " + e.getMessage());
        }
    }

    /* package private */ enum IMAGE_SOURCE {
        LOCAL,
        NETWORK;
    }

    private class BackgroundLoader extends AsyncTask<Artist, Void, BitmapDrawable> {
        @Override
        protected BitmapDrawable doInBackground(Artist... params) {
            boolean localArtworkSent = loadFromFileSystem(mArtSize);
            if (!localArtworkSent) {
                Log.d(TAG, "could not find " + mArtSize + " artwork for '" + mArtist + "'");
                //artwork not found in file system cache.  If we were trying to load the large size
                //image then try again with the small size
                if (mArtSize == ArtSize.LARGE) {
                    Log.d(TAG, "unable to load large art size from cache, looking for small " +
                            "artwork to pass back temporarily");
                    boolean foundSmallArt = loadFromFileSystem(ArtSize.SMALL);
                    if (foundSmallArt) {
                        Log.d(TAG, "sending back temporary small version of artwork for '" + mArtist + "'");
                    }
                }
            }

            //end of local artwork search.  If requested, we can send back some default artwork
            //before beginning the internet search
            if (mProvideDefaultArtwork && !localArtworkSent) {
                Drawable defaultArtwork = mContext.getResources().getDrawable(R.drawable.default_background);
                sendArtwork(defaultArtwork);
            }

            ArtworkPrefs artworkPrefs = new ArtworkPrefs(mContext);
            ArtworkDatabaseWrapper databaseWrapper = ArtworkDatabaseWrapper.getInstance(mContext);
            if (!localArtworkSent || databaseWrapper.isArtworkOutdated(mArtist)) {
                boolean foundInternetArtwork = loadFromInternet();
                if (foundInternetArtwork) {
                    boolean artworkLoaded = loadFromFileSystem(mArtSize);
                }
            }

            return null;
        }

        private boolean loadFromFileSystem(ArtSize size) {
            boolean artworkSent = false;
            BitmapDrawable d = null;
            FileSystemArtistFetcher fileSystemFetcher = new FileSystemArtistFetcher(mContext)
                    .setSize(size)
                    .setArtist(mArtist);
            try {
                d = fileSystemFetcher.loadArtwork();
                sendArtwork(d);
                artworkSent = true;
            } catch (ArtworkNotFoundException e) {

            }
            return artworkSent;
        }

        private boolean loadFromInternet() {
            boolean artworkFound = false;
            if (!mIsInternetSearchEnabled) {
                Log.d(TAG, "no or low quality local artwork, but internet search is disabled");
            } else {
                Log.d(TAG, "no or low quality local artwork, beginning internet search");
                LastFmImageArtistFetcher lastFmImageFetcher = new LastFmImageArtistFetcher(mContext)
                        .setArtist(mArtist);
                try {
                    BitmapDrawable d = lastFmImageFetcher.loadArtwork();
                    saveHighAndLowQualityVersions(d.getBitmap());
                    artworkFound = true;
                } catch (ArtworkNotFoundException e) {
                    Log.e(TAG, "artwork could not be found on last.fm");
                }
            }
            return artworkFound;
        }
    }
}









