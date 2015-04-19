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

package org.ciasaboark.canorum.artwork.cache;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.album.AlbumArtLoader;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.song.Track;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonathan Nelson on 4/17/15.
 * Holds low and high quality Bitmaps for the currently playing track
 */
public class CurPlayArtworkCache {
    private static final String TAG = "CurPlayArtworkCache";
    private static CurPlayArtworkCache sInstance;
    private static Context sContext;
    private Track mCurTrack;
    private Drawable mLowQualityDrawable;
    private Drawable mHighQualityDrawable;
    private AlbumArtLoader mHighLoader;
    private AlbumArtLoader mLowLoader;
    private BroadcastReceiver mBroadcastReceiver;
    private List<WeakReference<ArtLoadedWatcher>> mHighQualityListeners;
    private List<WeakReference<ArtLoadedWatcher>> mLowQualityListeners;
    private List<WeakReference<ArtLoadedWatcher>> mAllQualityListeners;

    public static CurPlayArtworkCache getsInstance(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context can not be null");
        }

        if (sInstance == null) {
            sContext = context;
            sInstance = new CurPlayArtworkCache();
        }
        return sInstance;
    }

    private CurPlayArtworkCache() {
        initLoaders();
        initBroadcastReceivers();
        mHighQualityListeners = new ArrayList<>();
        mLowQualityListeners = new ArrayList<>();
        mAllQualityListeners = new ArrayList<>();
    }

    private void initLoaders() {
        mHighLoader = new AlbumArtLoader(sContext)
                .setTag("HIGH")
                .setArtSize(ArtSize.LARGE)
                .setProvideDefaultArtwork(true);

        mLowLoader = new AlbumArtLoader(sContext)
                .setTag("LOW")
                .setArtSize(ArtSize.SMALL)
                .setProvideDefaultArtwork(true);
    }

    private void initBroadcastReceivers() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case MusicControllerSingleton.ACTION_PLAY:
                        mCurTrack = null;
                        mLowQualityDrawable = null;
                        mHighQualityDrawable = null;

                        Track curSong = (Track) intent.getSerializableExtra("curSong");
                        if (curSong == null) {
                            Log.w(TAG, "got broadcast notification that a song has began playing, but could " +
                                    "not get song from intent");
                        } else {
                            mCurTrack = curSong;
                            mHighLoader.setAlbum(mCurTrack.getSong().getAlbum())
                                    .setTag(mCurTrack)
                                    .setArtLoadedWatcher(new ArtLoadedWatcher() {
                                        @Override
                                        public void onArtLoaded(Drawable artwork, Object tag) {
                                            if (mCurTrack.equals(tag)) {
                                                mHighQualityDrawable = artwork;
                                                notifyHighQualityWatchers(artwork, tag);
                                                notifyAllQualityListeners(mLowQualityDrawable, tag);
                                            }
                                        }

                                        @Override
                                        public void onLoadProgressChanged(LoadProgress progress) {

                                        }
                                    })
                                    .loadInBackground();

                            mLowLoader.setAlbum(mCurTrack.getSong().getAlbum())
                                    .setTag(mCurTrack)
                                    .setArtLoadedWatcher(new ArtLoadedWatcher() {
                                        @Override
                                        public void onArtLoaded(Drawable artwork, Object tag) {
                                            if (mCurTrack.equals(tag)) {
                                                mLowQualityDrawable = artwork;
                                                notifyLowQualityListeners(mLowQualityDrawable, tag);
                                                notifyAllQualityListeners(mLowQualityDrawable, tag);
                                            }
                                        }

                                        @Override
                                        public void onLoadProgressChanged(LoadProgress progress) {

                                        }
                                    })
                                    .loadInBackground();
                        }

                }
            }
        };
        LocalBroadcastManager.getInstance(sContext).registerReceiver(mBroadcastReceiver, new IntentFilter(MusicControllerSingleton.ACTION_PLAY));
    }

    private void notifyHighQualityWatchers(Drawable artwork, Object tag) {
        notifyWatchersInList(mHighQualityListeners, artwork, tag);
    }

    private void notifyLowQualityListeners(Drawable artwork, Object tag) {
        notifyWatchersInList(mLowQualityListeners, artwork, tag);
    }

    private void notifyAllQualityListeners(Drawable artwork, Object tag) {
        notifyWatchersInList(mAllQualityListeners, artwork, tag);
    }

    private void notifyWatchersInList(List<WeakReference<ArtLoadedWatcher>> list, Drawable artwork, Object tag) {
        List<WeakReference<ArtLoadedWatcher>> cleanupRefs = new ArrayList<>();
        for (WeakReference<ArtLoadedWatcher> reference : list) {
            ArtLoadedWatcher watcher = reference.get();
            if (watcher == null) {
                cleanupRefs.add(reference);
            } else {
                watcher.onArtLoaded(artwork, tag);
            }
        }

        if (!cleanupRefs.isEmpty()) {
            removeRefsFromList(list, cleanupRefs);
        }
    }

    private void removeRefsFromList(List<WeakReference<ArtLoadedWatcher>> list, List<WeakReference<ArtLoadedWatcher>> refsToRemove) {
        //remove any dangling references
        for (WeakReference<ArtLoadedWatcher> ref : refsToRemove) {
            list.remove(ref);
        }
    }


    public void registerHighQualityListener(ArtLoadedWatcher watcher) {
        mHighQualityListeners.add(new WeakReference<ArtLoadedWatcher>(watcher));
    }

    public void registerLowQualityListener(ArtLoadedWatcher watcher) {
        mLowQualityListeners.add(new WeakReference<ArtLoadedWatcher>(watcher));
    }

    public void registerAllQualityListener(ArtLoadedWatcher watcher) {
        mAllQualityListeners.add(new WeakReference<ArtLoadedWatcher>(watcher));
    }

    public void unregisterWatcher(ArtLoadedWatcher watcher) {
        unregisterWatcherFromList(mHighQualityListeners, watcher);
        unregisterWatcherFromList(mLowQualityListeners, watcher);
        unregisterWatcherFromList(mAllQualityListeners, watcher);
    }

    private void unregisterWatcherFromList(List<WeakReference<ArtLoadedWatcher>> list, ArtLoadedWatcher watcher) {
        for (WeakReference<ArtLoadedWatcher> reference : list) {
            ArtLoadedWatcher w = reference.get();
            if (watcher == w) {
                mHighQualityListeners.remove(reference);
            }
        }
    }

    public Drawable getHighQualityDrawable() {
        return mHighQualityDrawable;
    }

    public Drawable getLowQualityDrawable() {
        return mLowQualityDrawable;
    }
}
