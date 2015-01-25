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

import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.content.Loader;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.Song;

import java.util.Random;

/**
 * Created by Jonathan Nelson on 1/23/15.
 */
public class NowPlayingCard extends RelativeLayout implements android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor> {
    public static final String BROADCAST_COLOR_CHANGED = "color_changed";
    public static final String BROADCAST_COLOR_CHANGED_PRIMARY = "new_color";
    public static final String BROADCAST_COLOR_CHANGED_ACCENT = "accent_color";
    private static final String TAG = "NowPlayingCard";
    private static final int ALBUM_ART_LOADER = 1;
    private RelativeLayout mLayout;
    private LoaderManager mLoaderManager;
    private Context mContext;
    private View mCurPlayCard;
    private TextView mCurTitle;
    private TextView mCurArtist;
    private TextView mCurAlbum;
    private ImageView mThumbsUp;
    private ImageView mThumbsDown;
    private ImageSwitcher mImageSwitcher;
    private ImageView mCurRating;
    private RelativeLayout mCurColor;
    private View mTopWrapper;
    private View mBottomWrapper;
    private MusicControllerSingleton mMusicControllerSingleton;

    public NowPlayingCard(Context ctx) {
        this(ctx, null);
    }

    public NowPlayingCard(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        if (ctx == null) {
            throw new IllegalArgumentException("Context can not be null");
        }
        mContext = ctx;
        init();
    }

    private void init() {
        Log.d(TAG, "init()");
        mLayout = (RelativeLayout) inflate(getContext(), R.layout.now_playing_card, this);
        mImageSwitcher = (ImageSwitcher) findViewById(R.id.switcher);
        mCurPlayCard = findViewById(R.id.cur_play_card);
        mCurTitle = (TextView) findViewById(R.id.cur_play_title);
        mCurArtist = (TextView) findViewById(R.id.cur_play_artist);
        mCurAlbum = (TextView) findViewById(R.id.cur_play_album);
        mCurRating = (ImageView) findViewById(R.id.cur_play_rating);
        mCurColor = (RelativeLayout) findViewById(R.id.cur_play_color);
        mTopWrapper = findViewById(R.id.cur_play_top_wrapper);
        mBottomWrapper = findViewById(R.id.cur_play_bottom_wrapper);
//        mCurAlbumArt =  (ImageView) findViewById(R.id.cur_play_album_art);
        mThumbsUp = (ImageView) findViewById(R.id.cur_play_thumbs_up);
        mThumbsDown = (ImageView) findViewById(R.id.cur_play_thumbs_down);

        initImageSwitcher();

        //disable loading the controller and database when in the layout editor
        if (!isInEditMode()) {
            mMusicControllerSingleton = MusicControllerSingleton.getInstance(mContext);
            try {
                mLoaderManager = ((ActionBarActivity) mContext).getSupportLoaderManager();
            } catch (Exception e) {
                Log.e(TAG, "error getting support loader manager from context, probably not an " +
                        "actionbar activity");
            }
            attachOnClickListeners();
            updateCurPlayCard();
        }
    }

    private void initImageSwitcher() {
        mImageSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView iv = new ImageView(mContext);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.
                        FILL_PARENT,LayoutParams.FILL_PARENT));
                return iv;
            }
        });
        Animation in = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);
        Animation out = AnimationUtils.loadAnimation(mContext, R.anim.fade_out);
        mImageSwitcher.setInAnimation(in);
        mImageSwitcher.setOutAnimation(out);
        mImageSwitcher.setImageDrawable(getResources().getDrawable(R.drawable.default_background));
    }

    private void attachOnClickListeners() {
        Log.d(TAG, "attachOnClickListeners()");
        mThumbsUp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO
                Song curSong = mMusicControllerSingleton.getCurSong();
                mMusicControllerSingleton.likeSong(curSong);
                showRatingHeart(mMusicControllerSingleton.getSongRating(curSong), true);
//                Toast.makeText(mContext, "Rating for " + curSong + " increased", Toast.LENGTH_SHORT).show();
            }
        });
        mThumbsDown.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO
                Song curSong = mMusicControllerSingleton.getCurSong();
                mMusicControllerSingleton.dislikeSong(curSong);
                showRatingHeart(mMusicControllerSingleton.getSongRating(curSong), true);
//                Toast.makeText(mContext, "Rating for " + curSong + " decreased", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void updateWidgets() {
        Log.d(TAG, "updateWidgets()");
        updateCurPlayCard();
    }

    private void updateCurPlayCard() {
        Log.d(TAG, "updateCurPlayCard()");
        if (mMusicControllerSingleton == null || mMusicControllerSingleton.getSongListSize() == 0) {
            showBlankCard();
        } else {
            //if we weren't explicitly given the current song then we can try to get it from the
            //service
            Song curSong = mMusicControllerSingleton.getCurSong();

            if (curSong == null) {
                Log.e(TAG, "error getting current song");
                showBlankCard();
            } else {
                showSongCard(curSong);
            }
        }
    }

    private void showBlankCard() {
        Log.d(TAG, "showBlankCard()");
        mTopWrapper.setVisibility(View.GONE);
        mBottomWrapper.setVisibility(View.GONE);
    }

    private void showSongCard(Song curSong) {
        Log.d(TAG, "showSongCard()");
        mCurTitle.setText(curSong.getTitle());
        mCurArtist.setText(curSong.getArtist());
        mCurAlbum.setText(curSong.getmAlbum());
        showRatingHeart(mMusicControllerSingleton.getSongRating(curSong), false);
        mTopWrapper.setVisibility(View.VISIBLE);
        mBottomWrapper.setVisibility(View.VISIBLE);

        //TODO set background album art
        Bundle bundle = new Bundle();
        bundle.putLong("albumId", curSong.getmAlbumId());
        try {
            if (mLoaderManager != null) {
                mLoaderManager.destroyLoader(ALBUM_ART_LOADER);
                mLoaderManager.initLoader(ALBUM_ART_LOADER, bundle, this);
            }

        } catch (Exception e) {
            Log.e(TAG, "error getting cursor loader manager from context, probably not an Activity");
        }
    }

    private void showRatingHeart(final int rating, boolean animate) {
        Log.d(TAG, "showRatingHeart()");
        Resources res = getResources();
        Drawable d = res.getDrawable(R.drawable.cur_heart_0);

        if (isBetweenInclusive(rating, 0, 10)) {
            d = res.getDrawable(R.drawable.cur_heart_0);
        } else if (isBetweenInclusive(rating, 11, 20)) {
            d = res.getDrawable(R.drawable.cur_heart_10);
        } else if (isBetweenInclusive(rating, 21, 40)) {
            d = res.getDrawable(R.drawable.cur_heart_25);
        } else if (isBetweenInclusive(rating, 41, 60)) {
            d = res.getDrawable(R.drawable.cur_heart_50);
        } else if (isBetweenInclusive(rating, 61, 85)) {
            d = res.getDrawable(R.drawable.cur_heart_75);
        } else if (isBetweenInclusive(rating, 86, 100)) {
            d = res.getDrawable(R.drawable.cur_heart_100);
        }
        d.mutate().setColorFilter(res.getColor(R.color.cur_heart), PorterDuff.Mode.MULTIPLY);
        mCurRating.setImageDrawable(d);
        mCurRating.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, rating + "/100", Toast.LENGTH_SHORT).show();
            }
        });
        if (animate) {
            Animation pulse = AnimationUtils.loadAnimation(mContext, R.anim.pulse);
            mCurRating.startAnimation(pulse);
        }
    }

    private boolean isBetweenInclusive(int val, int min, int max) {
        boolean isBetween = true;
        if (val < min) {
            isBetween = false;
        } else if (val > max) {
            isBetween = false;
        }
        return isBetween;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "onCreateLoader()");
        CursorLoader cursorLoader = null;
        switch (id) {
            case ALBUM_ART_LOADER:
                if (args == null) {
                    Log.d(TAG, "onCreateLoader() can not fetch album art without album id");
                } else {
                    long albumId = args.getLong("albumId", -1);
                    cursorLoader = new CursorLoader(
                            mContext,
                            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                            new String[]{
                                    MediaStore.Audio.Albums._ID,
                                    MediaStore.Audio.Albums.ALBUM_ART,
                                    MediaStore.Audio.Albums.ARTIST
                            },
                            MediaStore.Audio.Albums._ID + "=?",
                            new String[]{String.valueOf(albumId)},
                            null);
                }
                break;
        }
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(TAG, "onLoadFinished()");
        if (cursor != null && cursor.moveToFirst()) {
            try {
                String albumArtPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                String albumArtist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                long albumID = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));

                Bitmap bitmap = BitmapFactory.decodeFile(albumArtPath);

                Drawable albumArt;
                //its possible that creating the drawable failed, so use the default album art
                Random rand = new Random();
                int red = rand.nextInt();
                int blue = rand.nextInt();
                int green = rand.nextInt();
                final int color = Color.rgb(red, green, blue);
                if (bitmap == null) {
                    albumArt = getResources().getDrawable(R.drawable.default_album_art);
                    albumArt.mutate().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
                } else {
                   albumArt = new BitmapDrawable(getResources(), bitmap);
                }

                mImageSwitcher.setImageDrawable(albumArt);
                bitmap = ((BitmapDrawable)albumArt).getBitmap();
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
                                        primaryColor = getResources().getColor(R.color.color_primary);
                                    }

                                    //if we got a vibrant color then we can use that for the accent color
                                    if (vibrant != null) {
                                         accentColor = vibrant.getRgb();
                                    } else {
                                        accentColor = getResources().getColor(R.color.color_accent);
                                    }

                                    Intent i = new Intent(BROADCAST_COLOR_CHANGED);
                                    i.putExtra(BROADCAST_COLOR_CHANGED_PRIMARY, primaryColor);
                                    i.putExtra(BROADCAST_COLOR_CHANGED_ACCENT, accentColor);

                                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(i);
                                }
                            });
                }
            } catch (Exception e) {
                Log.e(TAG, "error getting album art uri from cursor: " + e.getMessage());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset()");
    }
}
