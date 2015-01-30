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

package org.ciasaboark.canorum.artwork;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.Song;
import org.ciasaboark.canorum.artwork.fetcher.FileSystemFetcher;
import org.ciasaboark.canorum.artwork.fetcher.GoogleImageSearchFetcher;
import org.ciasaboark.canorum.artwork.fetcher.LoadingWatcher;
import org.ciasaboark.canorum.artwork.fetcher.SystemArtFetcher;
import org.ciasaboark.canorum.artwork.writer.FileSystemWriter;
import org.ciasaboark.canorum.prefs.ArtworkPrefs;

import java.util.Random;

/**
 * Created by Jonathan Nelson on 1/29/15.
 */
public class AlbumArtLoader {
    public static final String BROADCAST_COLOR_CHANGED = "color_changed";
    public static final String BROADCAST_COLOR_CHANGED_PRIMARY = "new_color";
    public static final String BROADCAST_COLOR_CHANGED_ACCENT = "accent_color";
    private static final String TAG = "AlbumArtLoader";
    private final ArtworkPrefs mArtworkPrefs;
    private final BitmapDrawable mDefaultArtwork;
    private Context mContext;
    private AlbumArtWatcher mWatcher;
    private Song mSong;
    private boolean mEnableInternetSearch = false;
    private BitmapDrawable mBestArtwork = null;   //TODO stash artwork here before trying intetnet search
    private BitmapDrawable mLastKnownArtwork = null;

    public AlbumArtLoader(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
        mDefaultArtwork = (BitmapDrawable) mContext.getResources().getDrawable(R.drawable.default_album_art);
        mBestArtwork = mDefaultArtwork;
        mArtworkPrefs = new ArtworkPrefs(mContext);
    }

    public AlbumArtLoader setEnableInternetSearch(boolean enableInternetSearch) {
        mEnableInternetSearch = enableInternetSearch;
        return this;
    }

    public AlbumArtLoader setSong(Song song) {
        mSong = song;
        return this;
    }

    public AlbumArtLoader setAlbumArtWatcher(AlbumArtWatcher watcher) {
        mWatcher = watcher;
        return this;
    }

    public void loadInBackground() {
        if (mSong == null || mWatcher == null) {
            Log.d(TAG, "will not load artwork until a song and watcher have been given");
        } else {
            Log.d(TAG, "beginning search for album art with sdcard");
            beginLocalSearch();
        }
    }

    private void tryLoadingFileSystemArtwork() {
        FileSystemFetcher fileSystemFetcher = new FileSystemFetcher(mContext)
                .setSong(mSong)
                .setLoadingWatcher(new LoadingWatcher() {
                    @Override
                    public void onLoadFinished(BitmapDrawable artwork, String imageSource) {
                        if (artwork != null) {
                            Log.d(TAG, "found album art on sd card: " + imageSource);
                            processArtwork(artwork, imageSource, IMAGE_SOURCE.LOCAL);
                        } else {
                            Log.e(TAG, "could not load artork from sdcard, trying media storage");
                            tryLoadingMediaStoreArtwork();
                        }
                    }
                })
                .loadInBackground();
    }

    private void beginLocalSearch() {
        tryLoadingFileSystemArtwork();
    }

    private void beginInternetSearch() {
        tryLoadingGoogleImageSearchArtwork();
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
                        } else {
                            Log.e(TAG, "could not load artwork from google image search");
                        }
                    }
                })
                .setSafeSearchLevel(null)
                .setImageSize(null)
                .setSong(mSong)
                .loadInBackground();
    }

    private void tryLoadingMediaStoreArtwork() {
        SystemArtFetcher systemArtFetcher = new SystemArtFetcher(mContext)
                .setSong(mSong)
                .setLoadingWatcher(new LoadingWatcher() {
                    @Override
                    public void onLoadFinished(BitmapDrawable artwork, String imageSource) {
                        if (artwork != null) {
                            Log.d(TAG, "found album art in system media store: " + imageSource);
                            processArtwork(artwork, imageSource, IMAGE_SOURCE.LOCAL);
                            boolean artworkShouldBeOverwritten = isArtworkLowQuality(artwork) && mArtworkPrefs.isOverwriteLowQualityArtwork();
                            if (mArtworkPrefs.isInternetSearchEnabled() && artworkShouldBeOverwritten) {
                                Log.d(TAG, "beginning search for better quality artwork on internet");
                                beginInternetSearch();
                            }
                        } else {
                            Log.e(TAG, "could not find album art in system media store, " +
                                    "beginning internet search");
                            //send some default artwork before we begin the internet search
                            provideDefaultArtwork();
                            beginInternetSearch();
                        }
                    }
                })
                .loadInBackground();
    }

    private boolean isArtworkLowQuality(Drawable artwork) {
        //the artwork is considered low quality if its width is less than the devices display width
        boolean artworkIsLowQuality = false;
        int width = artwork.getIntrinsicWidth();
        if (width < getDeviceWidth()) {
            artworkIsLowQuality = true;
        }
        return artworkIsLowQuality;
    }

    private void provideDefaultArtwork() {
        Drawable defaultAlbumArt = mContext.getResources().getDrawable(R.drawable.default_album_art);
        mWatcher.onAlbumArtLoaded(defaultAlbumArt);
        loadPaletteColors(defaultAlbumArt);
    }

    private void storeArtworkOnFileSystemIfNeeded(IMAGE_SOURCE source, BitmapDrawable artwork) {
        if (artwork == null) {
            Log.d(TAG, "will not save null image to sdcard");
        } else {
            if (source != IMAGE_SOURCE.NETWORK) {
                Log.d(TAG, "image not sourced from internet, will not write to sdcard");
            } else if (!mArtworkPrefs.isAutoSaveInternetResults()) {
                Log.d(TAG, "album art sourced from internet source will not be cached on sdcard, turned of in settings");
            } else {
                Log.d(TAG, "saving results of internet search to sd card");
                FileSystemWriter fileSystemWriter = new FileSystemWriter(mContext);
                fileSystemWriter.writeArtworkToFilesystem(mSong, mBestArtwork);
            }
        }
    }

    private void stashArtworkIfNeeded(BitmapDrawable artwork, String imageSource, IMAGE_SOURCE source) {

        if (mBestArtwork == null || mBestArtwork == mDefaultArtwork) {
            Log.d(TAG, "saving artwork from " + imageSource + " as best artwork");
            mBestArtwork = artwork;
            storeArtworkOnFileSystemIfNeeded(source, artwork);
        } else {
            int curDimensions = mBestArtwork.getIntrinsicHeight() * mBestArtwork.getIntrinsicWidth();
            int newDimensions = artwork.getIntrinsicHeight() * artwork.getIntrinsicWidth();
            if (newDimensions > curDimensions) {
                mBestArtwork = artwork;
                storeArtworkOnFileSystemIfNeeded(source, artwork);
                Log.d(TAG, "saving artwork from " + imageSource + " as best artwork");
            } else {
                Log.d(TAG, "artwork from " + imageSource + " is lower resolution than current art, discarding");
            }
        }
    }

    private void loadPaletteColors() {
        //load palette colors using current best artwork
        loadPaletteColors(mBestArtwork);
    }

    private void loadPaletteColors(Drawable artwork) {
        Bitmap bitmap = null;
        try {
            bitmap = ((BitmapDrawable) artwork).getBitmap();

            //its possible that creating the drawable failed, so use the default album art
            Random rand = new Random();
            int red = rand.nextInt();
            int green = rand.nextInt();
            int blue = rand.nextInt();
            final int color = Color.rgb(red, green, blue);

            if (bitmap == null) {
                Intent i = new Intent(BROADCAST_COLOR_CHANGED);
                i.putExtra(BROADCAST_COLOR_CHANGED_PRIMARY, color);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(i);
            } else {
                Palette.generateAsync(bitmap,
                        new Palette.PaletteAsyncListener() {
                            @Override
                            public void onGenerated(Palette palette) {
                                //There is no guarantee that we can get any particular swatch
                                //from the image (or one at all), so we can try a few different
                                //ones.  Since the toolbar and media controls are in white we
                                //will prefer the darker colors
                                Palette.Swatch vibrant = palette.getVibrantSwatch();
                                Palette.Swatch darkVibrant = palette.getDarkVibrantSwatch();
                                Palette.Swatch muted = palette.getMutedSwatch();
                                Palette.Swatch darkmuted = palette.getDarkMutedSwatch();
                                int primaryColor;
                                int accentColor;

                                if (darkmuted != null) {
                                    primaryColor = darkmuted.getRgb();
                                } else if (darkVibrant != null) {
                                    primaryColor = darkVibrant.getRgb();
                                } else if (muted != null) {
                                    primaryColor = muted.getRgb();
                                } else {
                                    primaryColor = mContext.getResources().getColor(R.color.color_primary);
                                }

                                //if we got a vibrant color then we can use that for the accent color
                                if (vibrant != null) {
                                    accentColor = vibrant.getRgb();
                                } else {
                                    accentColor = mContext.getResources().getColor(R.color.color_accent);
                                }

                                Intent i = new Intent(BROADCAST_COLOR_CHANGED);
                                i.putExtra(BROADCAST_COLOR_CHANGED_PRIMARY, primaryColor);
                                i.putExtra(BROADCAST_COLOR_CHANGED_ACCENT, accentColor);

                                LocalBroadcastManager.getInstance(mContext).sendBroadcast(i);
                            }
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "could not get bitmap from artwork Drawable: " + e.getMessage());
        }
    }

    private BitmapDrawable resizeArtworkIfNeeded(BitmapDrawable artwork) {
        if (artwork == null) {
            Log.d(TAG, "artwork is null, skipping resize");
            return artwork;
        } else {
            Bitmap bitmap = artwork.getBitmap();
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int deviceWidth = getDeviceWidth();
            if (width <= deviceWidth) {
                Log.d(TAG, "artwork width is smaller than device width, will not scale");
                //if the image width is already smaller than the screen width, then just return the image
                return artwork;
            } else {
                Log.d(TAG, "artwork width is larger than device width, scaling down");
                float scale = (float)deviceWidth / (float)width;
                int newWidth = deviceWidth;
                int newHeight = (int) (height * scale);
                Matrix matrix = new Matrix();
                matrix.postScale(scale, scale);
                Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
                return new BitmapDrawable(resizedBitmap);
            }
        }
    }

    private int getDeviceWidth() {
        WindowManager windowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        return width;
    }

    public void processArtwork(BitmapDrawable artwork, String imageSource, IMAGE_SOURCE source) {
        //TODO stash awtwork so we can see if the internet loader got a better quality version later
        Log.d(TAG, "processArtwork()");
        if (artwork != null) {
            stashArtworkIfNeeded(artwork, imageSource, source);
        }

        //if the artwork isn't null and has changed from the last time
        //then notify the watcher, then begin pulling palette colors
        if (mBestArtwork != null && mBestArtwork != mLastKnownArtwork) {
            mLastKnownArtwork = mBestArtwork;
            mWatcher.onAlbumArtLoaded(mBestArtwork);
            loadPaletteColors();
        }
    }

    public enum IMAGE_SOURCE {
        LOCAL,
        NETWORK;
    }

    public interface AlbumArtWatcher {
        public void onAlbumArtLoaded(Drawable albumart);
    }
}




