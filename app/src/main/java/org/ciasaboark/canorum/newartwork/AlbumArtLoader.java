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

package org.ciasaboark.canorum.artwork.albumart;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;
import android.util.Log;

import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.albumart.fetcher.FileSystemFetcher;
import org.ciasaboark.canorum.artwork.albumart.fetcher.GoogleImageSearchFetcher;
import org.ciasaboark.canorum.artwork.albumart.fetcher.LastFmImageFetcher;
import org.ciasaboark.canorum.artwork.albumart.fetcher.MediaStoreFetcher;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.artwork.watcher.LoadingWatcher;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.artwork.writer.FileSystemWriter;
import org.ciasaboark.canorum.song.extended.ExtendedAlbum;

/**
 * Created by Jonathan Nelson on 1/29/15.
 */
public class AlbumArtLoader {
    private static final String TAG = "AlbumArtiLoader";
    private final BitmapDrawable mDefaultArtwork;
    private Activity mContext;
    private ArtLoadedWatcher mWatcher;
    private ExtendedAlbum mAlbum;
    private BitmapDrawable mBestArtwork = null;
    private BitmapDrawable mLastKnownArtwork = null;
    private ArtSize mArtSize = null;
    private PaletteGeneratedWatcher mPalletGeneratedWatcher;
    private boolean mIsInternetSearchEnabled = false;
    private String mTag;

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

    public AlbumArtLoader setTag(String tag) {
        mTag = tag;
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
            tryLoadingFileSystemArtwork();
        }
    }

    private void tryLoadingFileSystemArtwork() {
        FileSystemFetcher fileSystemFetcher = new FileSystemFetcher(mContext)
                .setAlbum(mAlbum)
                .setLoadingWatcher(new LoadingWatcher() {
                    @Override
                    public void onLoadFinished(BitmapDrawable artwork, String imageSource) {
                        if (artwork != null) {
                            Log.d(TAG, "(" + mAlbum + ") found album art on sd card: " + imageSource);
                            processArtwork(artwork, imageSource, IMAGE_SOURCE.LOCAL);
                        } else {
                            Log.e(TAG, "(" + mAlbum + ") could not load artork from sdcard, trying media store");
                            tryLoadingMediaStoreArtwork();
                        }
                    }
                })
                .setSize(mArtSize)
                .loadInBackground();
    }

    private void tryLoadingMediaStoreArtwork() {
        MediaStoreFetcher mediaStoreFetcher = new MediaStoreFetcher(mContext)
                .setAlbum(mAlbum)
                .setLoadingWatcher(new LoadingWatcher() {
                    @Override
                    public void onLoadFinished(BitmapDrawable artwork, String imageSource) {
                        if (artwork != null) {
                            Log.d(TAG, "found album art in system media store: " + imageSource);
                            processArtwork(artwork, imageSource, IMAGE_SOURCE.LOCAL);
                            mWatcher.onLoadProgressChanged(LoadProgress.FINISHED_LOAD);
                            boolean artworkShouldBeOverwritten = true; //TODO check that the artwork returned is actually low quality
                            if (mIsInternetSearchEnabled && artworkShouldBeOverwritten) {
                                Log.d(TAG, "beginning search for better quality artwork on internet");
                                beginInternetSearch();
                            }
                        } else {
                            Log.e(TAG, "could not find album art in system media store, " +
                                    "beginning internet search");
                            //send some default artwork before we begin the internet search
                            sendArtwork(null);
                            beginInternetSearch();
                        }
                    }
                })
                .loadInBackground();
    }

    private void tryLoadingGoogleImageSearchArtwork() {
        GoogleImageSearchFetcher googleImageSearchFetcher = new GoogleImageSearchFetcher(mContext)
                .setLoadingWatcher(new LoadingWatcher() {
                    @Override
                    public void onLoadFinished(BitmapDrawable artwork, String imageSource) {
                        if (artwork != null) {
                            Log.d(TAG, "google image search found artwork from: " + imageSource);
                            artwork = resizeArtworkIfNeeded(artwork);
                            processArtwork(artwork, imageSource, IMAGE_SOURCE.NETWORK);
                            mWatcher.onLoadProgressChanged(LoadProgress.FINISHED_LOAD);
                        } else {
                            Log.e(TAG, "could not load artwork from google image search, last provider used, no album art available");
                            mWatcher.onLoadProgressChanged(LoadProgress.FAILED_LOAD);
                        }
                    }
                })
                .setSafeSearchLevel(GoogleImageSearchFetcher.SEARCH_LEVEL.HIGH)
                .setImageSize(null)
                .setAlbum(mAlbum)
                .loadInBackground();
        mWatcher.onLoadProgressChanged(LoadProgress.STARTING_INTERNET);
    }

    private void beginInternetSearch() {
        tryLoadingLastFmImageArtwork();
    }

    private void tryLoadingLastFmImageArtwork() {
        LastFmImageFetcher lastFmImageFetcher = new LastFmImageFetcher(mContext)
                .setLoadingWatcher(new LoadingWatcher() {
                    @Override
                    public void onLoadFinished(BitmapDrawable artwork, String imageSource) {
                        if (artwork == null) {
                            Log.d(TAG, "(" + mAlbum + ") could not get artwork from last.fm, sending watcher null drawable");
                            sendArtwork(null);
                            tryLoadingGoogleImageSearchArtwork();
                        } else {
//                            artwork = resizeArtworkIfNeeded(artwork);
                            processArtwork(artwork, imageSource, IMAGE_SOURCE.NETWORK);
                        }
                    }
                })
                .setAlbum(mAlbum)
                .loadInBackground();
    }

//    private boolean isArtworkLowQuality(Drawable artwork) {
//        //the artwork is considered low quality if its width is less than the devices display width
//        boolean artworkIsLowQuality = false;
//        int width = artwork.getIntrinsicWidth();
//        if (width < getDeviceWidth()) {
//            artworkIsLowQuality = true;
//        }
//        return artworkIsLowQuality;
//    }

    private void provideDefaultArtwork() {
        Drawable defaultAlbumArt = mContext.getResources().getDrawable(R.drawable.default_album_art);
        sendArtwork(defaultAlbumArt);
    }

    private void sendArtwork(final Drawable drawable) {
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWatcher.onArtLoaded(drawable, mTag);
            }
        });
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

    private void stashArtworkIfNeeded(BitmapDrawable artwork, String imageSource, IMAGE_SOURCE source) {
        if (mBestArtwork == null || mBestArtwork == mDefaultArtwork) {
            Log.d(TAG, "(" + mAlbum + ") no previous artwork, using artwork from " + imageSource + " as best artwork");
            mBestArtwork = artwork;
            storeArtworkOnFileSystemIfNeeded(source, artwork);
        } else {
            int curDimensions = mBestArtwork.getIntrinsicHeight() * mBestArtwork.getIntrinsicWidth();
            int newDimensions = artwork.getIntrinsicHeight() * artwork.getIntrinsicWidth();
            if (newDimensions > curDimensions) {
                Log.d(TAG, "(" + mAlbum + ") using artwork from " + imageSource + " as best artwork");
                mBestArtwork = artwork;
                storeArtworkOnFileSystemIfNeeded(source, artwork);
            } else {
                Log.d(TAG, "(" + mAlbum + ") artwork from " + imageSource + " is lower resolution than current art, discarding");
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

    /* package private */ enum IMAGE_SOURCE {
        LOCAL,
        NETWORK;
    }

}









