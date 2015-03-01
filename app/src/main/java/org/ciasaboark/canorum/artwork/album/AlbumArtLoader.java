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

package org.ciasaboark.canorum.artwork.album;

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
import android.view.WindowManager;

import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.album.fetcher.FileSystemFetcher;
import org.ciasaboark.canorum.artwork.album.fetcher.LastFmImageFetcher;
import org.ciasaboark.canorum.artwork.album.fetcher.MediaStoreFetcher;
import org.ciasaboark.canorum.artwork.exception.ArtworkNotFoundException;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.artwork.writer.FileSystemWriter;
import org.ciasaboark.canorum.prefs.ArtworkPrefs;
import org.ciasaboark.canorum.song.extended.ExtendedAlbum;


/**
 * Created by Jonathan Nelson on 1/29/15.
 */
public class AlbumArtLoader {
    private static final String TAG = "NewAlbumArtLoader";
    private final BitmapDrawable mDefaultArtwork;
    private Activity mContext;
    private ArtLoadedWatcher mWatcher;
    private ExtendedAlbum mAlbum;
    private BitmapDrawable mBestArtwork = null;
    private BitmapDrawable mLastKnownArtwork = null;
    private ArtSize mArtSize = null;
    private PaletteGeneratedWatcher mPalletGeneratedWatcher;
    private boolean mIsInternetSearchEnabled = false;
    private Object mTag;
    private boolean mProvideDefaultArtwork;

    public AlbumArtLoader(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        if (!(ctx instanceof Activity)) {
            throw new IllegalArgumentException(TAG + " must be called with an activity context");
        }
        mContext = (Activity) ctx;
        mDefaultArtwork = (BitmapDrawable) mContext.getResources().getDrawable(R.drawable.default_album_art);
        mBestArtwork = mDefaultArtwork;
    }

    public AlbumArtLoader setTag(Object tag) {
        mTag = tag;
        return this;
    }

    public AlbumArtLoader setProvideDefaultArtwork(boolean provideDefaultArtwork) {
        mProvideDefaultArtwork = provideDefaultArtwork;
        return this;
    }

    public AlbumArtLoader setInternetSearchEnabled(boolean isInternetSearchEnabled) {
        mIsInternetSearchEnabled = isInternetSearchEnabled;
        return this;
    }

    public AlbumArtLoader setAlbum(ExtendedAlbum album) {
        mAlbum = album;
        return this;
    }

    public AlbumArtLoader setArtLoadedWatcher(ArtLoadedWatcher watcher) {
        mWatcher = watcher;
        return this;
    }

    public AlbumArtLoader setArtSize(ArtSize size) {
        mArtSize = size;
        return this;
    }

    public AlbumArtLoader setPaletteGeneratedWatcher(PaletteGeneratedWatcher watcher) {
        mPalletGeneratedWatcher = watcher;
        return this;
    }

    public AlbumArtLoader loadInBackground() {
        if (mAlbum == null || mWatcher == null) {
            Log.d(TAG, "will not load artwork until a song and watcher have been given");
        } else {
            Log.d(TAG, "(" + mAlbum + ") beginning search for album art with sdcard");
            checkAlbumAndBeginLoading();
        }
        return this;
    }

    private void checkAlbumAndBeginLoading() {
        if (mAlbum.getAlbumName().equals("<unknown>") || mAlbum.getAlbumName().equals("") ||
                mAlbum.getArtistName().equals("<unknown>") || mAlbum.getArtistName().equals("")) {
            Log.d(TAG, "(" + mAlbum + ") will not search for album art for unknown album");
        } else {
            BackgroundLoader backgroundLoader = new BackgroundLoader();
            backgroundLoader.execute(mAlbum);
        }
    }

    public void processArtwork(BitmapDrawable artwork, String imageSource, IMAGE_SOURCE source) {
        //TODO stash awtwork so we can see if the internet loader got a better quality version later
        Log.d(TAG, "(" + mAlbum + ") processArtwork()");
        if (artwork != null) {
            if (source == IMAGE_SOURCE.NETWORK) {
                artwork = resizeArtworkIfNeeded(artwork);
            }
            stashArtworkIfNeeded(artwork, imageSource, source);
        }

        //if the artwork isn't null and has changed from the last time
        //then notify the watcher, then begin pulling palette colors
        if (mBestArtwork != null && mBestArtwork != mLastKnownArtwork) {
            mLastKnownArtwork = mBestArtwork;
            mBestArtwork = artwork;
            sendArtwork(mBestArtwork);
            if (mPalletGeneratedWatcher != null) {
                loadPaletteColors(mBestArtwork);
            }
        }
    }

    private BitmapDrawable resizeArtworkIfNeeded(BitmapDrawable artwork) {
        if (artwork == null) {
            Log.d(TAG, "(" + mAlbum + ") artwork is null, skipping resize");
            return artwork;
        } else {
            ArtSize size = mArtSize;
            if (size == null) {
                Log.d(TAG, "no art size given, assuming LARGE");
                size = ArtSize.LARGE;
            }

            if (size == ArtSize.LARGE) {
                //TODO no resizing for now, just return the large format art
                Log.d(TAG, "(" + mAlbum + ") not resizing LARGE artwork");
                return artwork;
            } else {
                Bitmap bitmap = artwork.getBitmap();
                if (bitmap == null) {
                    Log.e(TAG, "could not get Bitmap from BitmapDrawable");
                    return artwork;
                } else {
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    int maxWidth = 400;
                    int maxHeight = 400;
                    if (width <= maxWidth) {
                        Log.d(TAG, "(" + mAlbum + ") artwork width (" + width + "x" + height + ") is smaller than max width, will not scale");
                        //if the image width is already smaller than the screen width, then just return the image
                        return artwork;
                    } else {
                        Log.d(TAG, "(" + mAlbum + ") artwork width (" + width + "x" + height + ") is larger than max width (400x400), scaling down");
                        float scale = (float) maxWidth / (float) width;
                        int newWidth = maxWidth;
                        int newHeight = (int) (height * scale);
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                        return new BitmapDrawable(scaledBitmap);
                    }
                }
            }
        }
    }

    private void stashArtworkIfNeeded(BitmapDrawable artwork, String imageSource, IMAGE_SOURCE source) {
        if (mBestArtwork == null || mBestArtwork == mDefaultArtwork) {
            Log.d(TAG, "(" + mAlbum + ") no previous artwork, using artwork from " + imageSource + " as best artwork");
            mBestArtwork = artwork;
            storeArtworkOnFileSystemIfNeeded(source, artwork);
        } else {
            int curDimensions = mBestArtwork.getIntrinsicHeight() * mBestArtwork.getIntrinsicWidth();
            int newDimensions = artwork.getIntrinsicHeight() * artwork.getIntrinsicWidth();
            if (newDimensions >= curDimensions) {
                Log.d(TAG, "(" + mAlbum + ") using artwork from " + imageSource + " as best artwork");
                mBestArtwork = artwork;
                storeArtworkOnFileSystemIfNeeded(source, artwork);
            } else {
                Log.d(TAG, "(" + mAlbum + ") artwork from " + imageSource + " is lower resolution than current art, discarding");
            }
        }
    }

    private void sendArtwork(final Drawable drawable) {
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWatcher.onArtLoaded(drawable, mTag);
            }
        });
    }

    private void loadPaletteColors(Drawable artwork) {
        Bitmap bitmap = null;
        try {
            bitmap = ((BitmapDrawable) artwork).getBitmap();
            Palette.generateAsync(bitmap,
                    new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            if (mPalletGeneratedWatcher != null) {
                                mPalletGeneratedWatcher.onPaletteGenerated(palette);
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "could not generate palette from artwork Drawable: " + e.getMessage());
        }
    }

    private void storeArtworkOnFileSystemIfNeeded(IMAGE_SOURCE source, BitmapDrawable artwork) {
        if (artwork == null) {
            Log.d(TAG, "(" + mAlbum + ") will not save null image to sdcard");
        } else {
            if (source != IMAGE_SOURCE.NETWORK) {
                Log.d(TAG, "(" + mAlbum + ") image not sourced from internet, will not write to sdcard");
            } else {
                Log.d(TAG, "(" + mAlbum + ") saving results of internet search to sd card");
                FileSystemWriter fileSystemWriter = new FileSystemWriter(mContext);
                fileSystemWriter.writeArtworkToFileSystem(mAlbum, mBestArtwork, mArtSize);
            }
        }
    }

    private boolean isArtworkLowQuality(BitmapDrawable d) {
        boolean artworkIsLowQuality = false;
        if (d == null) {
            artworkIsLowQuality = true;
        } else if (mArtSize != ArtSize.SMALL) {
            int imageWidth = d.getBitmap().getWidth();

            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int screenWidth = size.x;

            artworkIsLowQuality = imageWidth < screenWidth;
        }
        return artworkIsLowQuality;
    }

    /* package private */ enum IMAGE_SOURCE {
        LOCAL,
        NETWORK;
    }

    private class BackgroundLoader extends AsyncTask<ExtendedAlbum, Void, BitmapDrawable> {

        @Override
        protected BitmapDrawable doInBackground(ExtendedAlbum... params) {
            BitmapDrawable d = null;
            ExtendedAlbum album = params[0];
            FileSystemFetcher fileSystemFetcher = new FileSystemFetcher(mContext)
                    .setSize(mArtSize)
                    .setAlbum(mAlbum);
            try {
                d = fileSystemFetcher.loadArtwork();
                processArtwork(d, "cached", IMAGE_SOURCE.LOCAL);
            } catch (ArtworkNotFoundException e) {
                //artwork not found in file system cache.  If we were trying to load the large size
                //image then try again with the small size
                if (mArtSize == ArtSize.LARGE) {
                    Log.d(TAG, "unable to load large art size from cache, looking for small " +
                            "artwork to pass back temporarily");
                    FileSystemFetcher smallFileSystemFetcher = new FileSystemFetcher(mContext)
                            .setSize(ArtSize.SMALL)
                            .setAlbum(mAlbum);
                    try {
                        BitmapDrawable smallAlbumArt = smallFileSystemFetcher.loadArtwork();
                        sendArtwork(smallAlbumArt);
                        Log.d(TAG, "found small artwork");
                    } catch (ArtworkNotFoundException e3) {
                        //if the small artwork didn't load its not a problem, we will be trying
                        // media store anyway
                        Log.d(TAG, "could not find small artwork when asked for large, this is fine");
                    }
                }
                Log.d(TAG, "unable to load artwork from file system cache, trying media store");
                MediaStoreFetcher mediaStoreFetcher = new MediaStoreFetcher(mContext)
                        .setAlbum(mAlbum);
                try {
                    d = mediaStoreFetcher.loadArtwork();
                    processArtwork(d, "media store", IMAGE_SOURCE.LOCAL);
                } catch (ArtworkNotFoundException e2) {
                    Log.d(TAG, "unable to load artwork from media store");
                }
            }

            //end of local artwork search.  If requested, we can send back some default artwork
            //before beginning the internet search
            if (mProvideDefaultArtwork) {
                Drawable defaultArtwork = mContext.getResources().getDrawable(R.drawable.default_album_art);
                sendArtwork(defaultArtwork);
            }

            ArtworkPrefs artworkPrefs = new ArtworkPrefs(mContext);
            if (d == null || isArtworkLowQuality(d)) {
                if (!mIsInternetSearchEnabled) {
                    Log.d(TAG, "no or low quality local artwork, but internet search is disabled");
                } else {
                    Log.d(TAG, "no or low quality local artwork, beginning internet search");
                    LastFmImageFetcher lastFmImageFetcher = new LastFmImageFetcher(mContext)
                            .setAlbum(mAlbum);
                    try {
                        d = lastFmImageFetcher.loadArtwork();
                        processArtwork(d, "last.fm", IMAGE_SOURCE.NETWORK);
                    } catch (ArtworkNotFoundException e) {
                        Log.e(TAG, "artwork could not be found on last.fm");
                    }
                }
            }

            return d;
        }
    }

}









