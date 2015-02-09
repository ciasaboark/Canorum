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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;
import android.util.Log;

import org.ciasaboark.canorum.Artist;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.artist.fetcher.FileSystemFetcher;
import org.ciasaboark.canorum.artwork.artist.fetcher.LastFmImageFetcher;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.artwork.watcher.LoadingWatcher;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.artwork.writer.FileSystemWriter;

/**
 * Created by Jonathan Nelson on 1/29/15.
 */
public class ArtistArtLoader {
    private static final String TAG = "ArtistArtLoader";
    private final BitmapDrawable mDefaultArtwork;
    private Context mContext;
    private ArtLoadedWatcher mWatcher;
    private Artist mArtist;
    private BitmapDrawable mBestArtwork = null;
    private BitmapDrawable mLastKnownArtwork = null;
    private ArtSize mArtSize = null;
    private PaletteGeneratedWatcher mPalletGeneratedWatcher;

    public ArtistArtLoader(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
        mDefaultArtwork = (BitmapDrawable) mContext.getResources().getDrawable(R.drawable.default_album_art);
        mBestArtwork = mDefaultArtwork;
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
        if (mArtist.getArtistName().equals("<unknown>") || mArtist.getArtistName().equals("")) {
            Log.d(TAG, "(" + mArtist + ") will not search for artist art for unknown artist");
        } else {
            tryLoadingFileSystemArtwork();
        }
    }

    private void tryLoadingFileSystemArtwork() {
        FileSystemFetcher fileSystemFetcher = new FileSystemFetcher(mContext)
                .setArtist(mArtist)
                .setArtLoadedWatcher(new LoadingWatcher() {
                    @Override
                    public void onLoadFinished(BitmapDrawable artwork, String imageSource) {
                        if (artwork != null) {
                            Log.d(TAG, "(" + mArtist + ") found artist art on sd card: " + imageSource);
                            processArtwork(artwork, imageSource, IMAGE_SOURCE.LOCAL);
                        } else {
                            Log.e(TAG, "(" + mArtist + ") could not load artork from sdcard, trying last.fm");
                            tryLoadingLastFmImageArtwork();
                        }
                    }
                })
                .setArtSize(mArtSize)
                .loadInBackground();
    }

    private void tryLoadingLastFmImageArtwork() {
        LastFmImageFetcher lastFmImageFetcher = new LastFmImageFetcher(mContext)
                .setLoadingWatcher(new LoadingWatcher() {
                    @Override
                    public void onLoadFinished(BitmapDrawable artwork, String imageSource) {
                        if (artwork == null) {
                            Log.d(TAG, "(" + mArtist + ") could not get artwork from last.fm, sending watcher null drawable");
                            mWatcher.onArtLoaded(null);
                        } else {
//                            artwork = resizeArtworkIfNeeded(artwork);
                            processArtwork(artwork, imageSource, IMAGE_SOURCE.NETWORK);
                        }
                    }
                })
                .setArtist(mArtist)
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
        Drawable defaultArtistArt = mContext.getResources().getDrawable(R.drawable.default_album_art);
        mWatcher.onArtLoaded(defaultArtistArt);
    }

    private void storeArtworkOnFileSystemIfNeeded(IMAGE_SOURCE source, BitmapDrawable artwork) {
        if (artwork == null) {
            Log.d(TAG, "(" + mArtist + ") will not save null image to sdcard");
        } else {
            if (source != IMAGE_SOURCE.NETWORK) {
                Log.d(TAG, "(" + mArtist + ") image not sourced from internet, will not write to sdcard");
            } else {
                Log.d(TAG, "(" + mArtist + ") saving results of internet search to sd card");
                FileSystemWriter fileSystemWriter = new FileSystemWriter(mContext);
                if (fileSystemWriter.writeArtworkToFileSystem(mArtist, mBestArtwork, mArtSize)) {
                    mWatcher.onLoadProgressChanged(LoadProgress.FINISHED_LOAD);
                }
            }
        }
    }

    private void stashArtworkIfNeeded(BitmapDrawable artwork, String imageSource, IMAGE_SOURCE source) {
        if (mBestArtwork == null || mBestArtwork == mDefaultArtwork) {
            Log.d(TAG, "(" + mArtist + ") no previous artwork, using artwork from " + imageSource + " as best artwork");
            mBestArtwork = artwork;
            storeArtworkOnFileSystemIfNeeded(source, artwork);
        } else {
            int curDimensions = mBestArtwork.getIntrinsicHeight() * mBestArtwork.getIntrinsicWidth();
            int newDimensions = artwork.getIntrinsicHeight() * artwork.getIntrinsicWidth();
            if (newDimensions > curDimensions) {
                Log.d(TAG, "(" + mArtist + ") using artwork from " + imageSource + " as best artwork");
                mBestArtwork = artwork;
                storeArtworkOnFileSystemIfNeeded(source, artwork);
            } else {
                Log.d(TAG, "(" + mArtist + ") artwork from " + imageSource + " is lower resolution than current art, discarding");
            }
        }
    }

    private BitmapDrawable resizeArtworkIfNeeded(BitmapDrawable artwork) {
        if (artwork == null) {
            Log.d(TAG, "(" + mArtist + ") artwork is null, skipping resize");
            return artwork;
        } else {
            ArtSize size = mArtSize;
            if (size == null) {
                Log.d(TAG, "no art size given, assuming LARGE");
                size = ArtSize.LARGE;
            }

            if (size == ArtSize.LARGE) {
                //TODO no resizing for now, just return the large format art
                Log.d(TAG, "(" + mArtist + ") not resizing LARGE artwork");
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
                        Log.d(TAG, "(" + mArtist + ") artwork width (" + width + "x" + height + ") is smaller than max width, will not scale");
                        //if the image width is already smaller than the screen width, then just return the image
                        return artwork;
                    } else {
                        Log.d(TAG, "(" + mArtist + ") artwork width (" + width + "x" + height + ") is larger than max width (400x400), scaling down");
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
        Log.d(TAG, "(" + mArtist + ") processArtwork()");
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
            mWatcher.onArtLoaded(mBestArtwork);
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




