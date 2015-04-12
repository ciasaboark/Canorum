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

package org.ciasaboark.canorum.view;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.album.AlbumArtLoader;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.song.Track;

import java.util.Formatter;
import java.util.Locale;

/**
 * Created by Jonathan Nelson on 2/13/15.
 */

public class MiniControllerView extends RelativeLayout {
    private static final String TAG = "MusicController";
    private static final int SHOW_PROGRESS = 1;
    private Context mContext;
    private MusicControllerView.SimpleMediaPlayerControl mMediaPlayerController;
    private RelativeLayout mLayout;
    private View mMediaControls;
    private ImageView mPrevButton;
    private ImageView mNextButton;
    private ImageView mPlayButton;
    private ImageView mAlbumImageView;
    private TextView mTrackTitle;
    private TextView mTrackArtist;
    private boolean mIsEnabled = true;
    private LocalBroadcastManager mBroadcastManager;
    private BroadcastReceiver mReceiver;
    private int mPrevAccentColor = -1;
    private OnClickListener mPrevListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mMediaPlayerController.playPrev();
            updatePlayPause();
            updatePrevNext();
        }
    };
    private OnClickListener mNextListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mMediaPlayerController.playNext();
            updatePlayPause();
            updatePrevNext();
        }
    };

    private OnClickListener mPlayListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mMediaPlayerController.play();
            updatePlayPause();
        }
    };
    private OnClickListener mPauseListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mMediaPlayerController.pause();
            updatePlayPause();
        }
    };


    public MiniControllerView(Context context) {
        this(context, null);
    }

    public MiniControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public void setMediaPlayerController(MusicControllerView.SimpleMediaPlayerControl mpc) {
        if (mpc == null) {
            throw new IllegalArgumentException("Media Player Control can not be null");
        }
        mMediaPlayerController = mpc;
        updatePlayPause();
        updatePrevNext();
    }

    private void updatePlayPause() {
        if (mMediaPlayerController == null || mMediaPlayerController.isEmpty()) {
            //no media controller has been specified yet, disable this button
            Drawable d = getResources().getDrawable(R.drawable.controls_play);
            d.mutate().setColorFilter(getResources().getColor(R.color.controls_disabled), PorterDuff.Mode.MULTIPLY);
            mPlayButton.setImageDrawable(d);
            mPlayButton.setEnabled(false);
            mPlayButton.setOnClickListener(null);
        } else if (mMediaPlayerController.isPlaying()) {
            mPlayButton.setImageDrawable(getResources().getDrawable(R.drawable.controls_pause));
            mPlayButton.setOnClickListener(mPauseListener);
            mPlayButton.setEnabled(true);
        } else {
            mPlayButton.setImageDrawable(getResources().getDrawable(R.drawable.controls_play));
            mPlayButton.setOnClickListener(mPlayListener);
            mPlayButton.setEnabled(true);
        }
    }

    private void updatePrevNext() {
        updatePrev();
        updateNext();
    }

    private void updatePrev() {
        if (mMediaPlayerController == null || !mMediaPlayerController.hasPrev()) {
            Drawable d = getResources().getDrawable(R.drawable.controls_prev);
            d.mutate().setColorFilter(getResources().getColor(R.color.controls_disabled), PorterDuff.Mode.MULTIPLY);
            mPrevButton.setImageDrawable(d);
            mPrevButton.setEnabled(false);
            mPrevButton.setOnClickListener(null);
        } else {
            mPrevButton.setImageDrawable(getResources().getDrawable(R.drawable.controls_prev));
            mPrevButton.setEnabled(true);
            mPrevButton.setOnClickListener(mPrevListener);
        }
    }

    private void updateNext() {
        if (mMediaPlayerController == null || !mMediaPlayerController.hasNext()) {
            Drawable d = getResources().getDrawable(R.drawable.controls_next);
            d.mutate().setColorFilter(getResources().getColor(R.color.controls_disabled), PorterDuff.Mode.MULTIPLY);
            mNextButton.setImageDrawable(d);
            mNextButton.setEnabled(false);
            mNextButton.setOnClickListener(null);
        } else {
            mNextButton.setImageDrawable(getResources().getDrawable(R.drawable.controls_next));
            mNextButton.setEnabled(true);
            mNextButton.setOnClickListener(mNextListener);
        }
    }

    private String getFormattedTime(int durationMs) {
        int totalSeconds = durationMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        Formatter formatter = new Formatter(sb, Locale.getDefault());
        if (hours > 0) {
            return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return formatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private void init() {
        mLayout = (RelativeLayout) inflate(getContext(), R.layout.view_mini_controls, this);
        if (!isInEditMode()) {
            mMediaControls = mLayout.findViewById(R.id.mini_controller_root);
            mAlbumImageView = (ImageView) mLayout.findViewById(R.id.albumImage);
            mTrackTitle = (TextView) mLayout.findViewById(R.id.mini_song_title);
            mTrackArtist = (TextView) mLayout.findViewById(R.id.mini_song_artist);

            mPrevButton = (ImageView) mLayout.findViewById(R.id.controls_button_media_prev);
            mNextButton = (ImageView) mLayout.findViewById(R.id.controls_button_media_next);
            mPlayButton = (ImageView) mLayout.findViewById(R.id.controls_button_media_play);

            attachStaticListeners();
            updateWidgets();
            clipToOutlines();
            initBroadcastReceivers();
        }
    }

    private void clipToOutlines() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewOutlineProvider viewOutlineProvider = new ViewOutlineProvider() {
                @Override
                @TargetApi(21)
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, 64, 64);
                }
            };

            mPrevButton.setOutlineProvider(viewOutlineProvider);
            mPlayButton.setOutlineProvider(viewOutlineProvider);
            mNextButton.setOutlineProvider(viewOutlineProvider);
        }
    }

    private void initBroadcastReceivers() {
        //Got a notification that music has began playing
        LocalBroadcastManager.getInstance(mContext).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateWidgets();
            }
        }, new IntentFilter(MusicControllerSingleton.ACTION_PLAY));

        //Got a notification that the seek progress has been changed
        LocalBroadcastManager.getInstance(mContext).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            }
        }, new IntentFilter(MusicControllerSingleton.ACTION_SEEK));

        //Got a notification that the music has been paused
        LocalBroadcastManager.getInstance(mContext).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateWidgets();
            }
        }, new IntentFilter(MusicControllerSingleton.ACTION_PAUSE));

        //Got a notification that the next track is playing
        LocalBroadcastManager.getInstance(mContext).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateWidgets();
            }
        }, new IntentFilter(MusicControllerSingleton.ACTION_NEXT));

        //Got a notification that the prev track is playing
        LocalBroadcastManager.getInstance(mContext).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateWidgets();
            }
        }, new IntentFilter(MusicControllerSingleton.ACTION_PREV));
    }

    public void updateWidgets() {
        updatePlayPause();
        updatePlayPause();
        updatePrevNext();
        updateTrackText();
        updateAlbumView();
    }

    private void updateAlbumView() {
        Track curTrack = MusicControllerSingleton.getInstance(mContext).getCurTrack();
        if (curTrack == null) {
            mAlbumImageView.setImageDrawable(getResources().getDrawable(R.drawable.default_album_art));
        } else {
            Album album = curTrack.getSong().getAlbum();
            AlbumArtLoader albumArtLoader = new AlbumArtLoader(mContext)
                    .setInternetSearchEnabled(true)
                    .setAlbum(album)
                    .setArtSize(ArtSize.SMALL)
                    .setArtLoadedWatcher(new ArtLoadedWatcher() {
                        @Override
                        public void onArtLoaded(final Drawable artwork, Object tag) {
                            if (artwork == null) {
                                mAlbumImageView.setImageDrawable(getResources().getDrawable(R.drawable.default_album_art));
                            } else {
                                mAlbumImageView.setImageDrawable(artwork);
                            }
                        }

                        @Override
                        public void onLoadProgressChanged(LoadProgress progress) {

                        }
                    })
                    .setPaletteGeneratedWatcher(new PaletteGeneratedWatcher() {
                        @Override
                        public void onPaletteGenerated(Palette palette, Object tag) {
                            //TODO
                        }
                    })
                    .loadInBackground();
        }
    }

    private void updateTrackText() {
        Track curTrack = MusicControllerSingleton.getInstance(mContext).getCurTrack();
        String title = "";
        String artist = "";
        if (curTrack != null) {
            title = curTrack.getSong().getTitle();
            artist = curTrack.getSong().getAlbum().getArtist().getArtistName();
        }
        mTrackTitle.setText(title);
        mTrackArtist.setText(artist);
    }

    public BitmapDrawable getAlbumArtwork() {
        BitmapDrawable albumArt = null;
        Drawable d = mAlbumImageView.getDrawable();
        if (d instanceof BitmapDrawable) {
            albumArt = (BitmapDrawable) d;
        }
        return albumArt;
    }

    private void attachStaticListeners() {
        mPrevButton.setOnClickListener(mPrevListener);
        mNextButton.setOnClickListener(mNextListener);
    }

    public ImageView getAlbumImageView() {
        return mAlbumImageView;
    }

    @Override
    public Drawable getBackground() {
        if (isInEditMode()) {
            return null;
        } else {
            return mMediaControls.getBackground();
        }
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        mMediaControls.setOnClickListener(listener);
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        mIsEnabled = isEnabled;
    }

    @Override
    public void setBackgroundColor(int color) {
        mMediaControls.setBackgroundColor(color);
    }

    /**
     * Set the OnClick listener for the previous and next buttons.  Passing null will unset any
     * previously attached listeners
     *
     * @param prevListener
     * @param nextListener
     */
    public void setPrevNextListeners(OnClickListener prevListener, OnClickListener nextListener) {
        mPrevListener = prevListener;
        mNextListener = nextListener;
        mPrevButton.setOnClickListener(prevListener);
        mNextButton.setOnClickListener(nextListener);
    }

    public enum RepeatMode {
        NONE,
        ALL,
        SINGLE
    }

    public enum ShuffleMode {
        OFF,
        SIMPLE
    }

    @Override
    public void setBackground(Drawable background) {
        mMediaControls.setBackground(background);
    }


}
