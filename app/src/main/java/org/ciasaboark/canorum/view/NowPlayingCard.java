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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.Song;
import org.ciasaboark.canorum.artwork.albumart.AlbumArtLoader;
import org.ciasaboark.canorum.artwork.albumart.writer.FileSystemWriter;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.prefs.RatingsPrefs;

/**
 * Created by Jonathan Nelson on 1/23/15.
 */
public class NowPlayingCard extends RelativeLayout implements ArtLoadedWatcher {
    private static final String TAG = "NowPlayingCard";

    private RelativeLayout mLayout;

    private Context mContext;
    private View mCurPlayCard;
    private TextView mCurTitle;
    private TextView mCurArtist;
    private TextView mCurAlbum;
    private ImageView mThumbsUp;
    private ImageView mThumbsDown;
    private ImageSwitcher mImageSwitcher;
    private ImageView mCurRating;
    private RelativeLayout mCurSearching;
    private TextView mCurSearchText;
    private View mTopWrapper;
    private View mBottomWrapper;
    private ImageView mSavedIcon;
    private MusicControllerSingleton mMusicControllerSingleton;
    private PaletteGeneratedWatcher mWatcher;

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

    public void setPaletteGenerateListener(PaletteGeneratedWatcher watcher) {
        mWatcher = watcher;
    }

    private void init() {
        mLayout = (RelativeLayout) inflate(getContext(), R.layout.now_playing_card, this);
        mImageSwitcher = (ImageSwitcher) findViewById(R.id.switcher);
        mCurPlayCard = findViewById(R.id.cur_play_card);
        mCurTitle = (TextView) findViewById(R.id.cur_play_title);
        mCurArtist = (TextView) findViewById(R.id.cur_play_artist);
        mCurAlbum = (TextView) findViewById(R.id.cur_play_album);
        mCurRating = (ImageView) findViewById(R.id.cur_play_rating);
        mCurSearching = (RelativeLayout) findViewById(R.id.cur_play_search);
        mCurSearchText = (TextView) findViewById(R.id.cur_play_search_text);
        mTopWrapper = findViewById(R.id.cur_play_top_wrapper);
        mBottomWrapper = findViewById(R.id.cur_play_bottom_wrapper);
        mThumbsUp = (ImageView) findViewById(R.id.cur_play_thumbs_up);
        mThumbsDown = (ImageView) findViewById(R.id.cur_play_thumbs_down);
        mSavedIcon = (ImageView) findViewById(R.id.cur_play_save);

        initImageSwitcher();
        initBroadcastReceivers();


        //disable loading the controller and database when in the layout editor
        if (!isInEditMode()) {
            mMusicControllerSingleton = MusicControllerSingleton.getInstance(mContext);

            attachOnClickListeners();
            updateCurPlayCard();
        }
    }

    private void initBroadcastReceivers() {
        //Got a notification that music has began playing
        LocalBroadcastManager.getInstance(mContext).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String searchLocation = intent
                        .getStringExtra(AlbumArtLoader.BROADCAST_ACTION_SEARCHING_BEGINS_KEY);
                if (searchLocation != null) {
                    showSearchProgress(searchLocation);
                }
            }
        }, new IntentFilter(AlbumArtLoader.BROADCAST_ACTION_SEARCHING_BEGINS));

        LocalBroadcastManager.getInstance(mContext).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                hideSearchProgress();
            }
        }, new IntentFilter(AlbumArtLoader.BROADCAST_ACTION_SEARCHING_ENDS));

        LocalBroadcastManager.getInstance(mContext).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCurSearching.setVisibility(View.INVISIBLE);
                Animation fadeIn = AnimationUtils.loadAnimation(mContext, R.anim.fade_in_slow);
                final Animation fadeOut = AnimationUtils.loadAnimation(mContext, R.anim.fade_out_slow);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mSavedIcon.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                fadeIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        //nothing to do here
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mSavedIcon.startAnimation(fadeOut);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                        //nothing to do here
                    }
                });
                mSavedIcon.startAnimation(fadeIn);
                mSavedIcon.setVisibility(View.VISIBLE);

            }
        }, new IntentFilter(AlbumArtLoader.BROADCAST_ACTION_ARTWORK_SAVED));
    }

    private void showSearchProgress(String searchLocation) {
        mCurSearchText.setText("Searching " + searchLocation + "...");
        mCurSearching.setVisibility(View.VISIBLE);
    }

    private void hideSearchProgress() {
        mCurSearchText.setText("");
        mCurSearching.setVisibility(View.INVISIBLE);
    }

    private void initImageSwitcher() {
        mImageSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView iv = new ImageView(mContext);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.
                        FILL_PARENT, LayoutParams.FILL_PARENT));
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
        mImageSwitcher.setDrawingCacheEnabled(true);
        mImageSwitcher.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ImageView curImageView = (ImageView) mImageSwitcher.getCurrentView();
                Drawable d = curImageView.getDrawable();
                if (d instanceof BitmapDrawable) {
                    FileSystemWriter fileSystemWriter = new FileSystemWriter(mContext);
                    fileSystemWriter.writeArtworkToFilesystem(mMusicControllerSingleton.getCurSong(), (BitmapDrawable) d);
                }
                return true;
            }
        });

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

        mCurRating.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu ratingPopupMenu = new PopupMenu(mContext, mCurRating);
                ratingPopupMenu.inflate(R.menu.popup_ratings);
                ratingPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        RatingsPrefs ratingsPrefs = new RatingsPrefs(mContext);
                        boolean itemHandled = false;
                        switch (item.getItemId()) {
                            case R.id.popup_menu_rating_optimistic:
                                Toast.makeText(mContext, "selected optimistic rater", Toast.LENGTH_SHORT).show();
                                ratingsPrefs.setRatingAlgoritm(RatingsPrefs.Mode.OPTIMISTIC);
                                itemHandled = true;
                                break;
                            case R.id.popup_menu_rating_linear:
                                Toast.makeText(mContext, "selected linear rater", Toast.LENGTH_SHORT).show();
                                ratingsPrefs.setRatingAlgoritm(RatingsPrefs.Mode.LINEAR);
                                itemHandled = true;
                                break;
                            case R.id.popup_menu_rating_full_playthrough:
                                Toast.makeText(mContext, "selected full playthrough rater", Toast.LENGTH_SHORT).show();
                                ratingsPrefs.setRatingAlgoritm(RatingsPrefs.Mode.PREFER_FULL);
                                itemHandled = true;
                                break;
                            default: //STANDARD
                                Toast.makeText(mContext, "selected standard rater", Toast.LENGTH_SHORT).show();
                                ratingsPrefs.setRatingAlgoritm(RatingsPrefs.Mode.STANDARD);
                                itemHandled = true;
                                break;
                        }
                        return itemHandled;
                    }
                });
                ratingPopupMenu.show();
            }
        });
    }

    public void updateWidgets() {
        updateCurPlayCard();
    }

    private void updateCurPlayCard() {
        if (mMusicControllerSingleton == null || mMusicControllerSingleton.isEmpty()) {
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
        mTopWrapper.setVisibility(View.GONE);
        mBottomWrapper.setVisibility(View.GONE);
    }

    private void showSongCard(Song curSong) {
        mCurTitle.setText(curSong.getTitle());
        mCurArtist.setText(curSong.getArtist());
        mCurAlbum.setText(curSong.getAlbum());
        showRatingHeart(mMusicControllerSingleton.getSongRating(curSong), false);
        mTopWrapper.setVisibility(View.VISIBLE);
        mBottomWrapper.setVisibility(View.VISIBLE);
        AlbumArtLoader albumArtLoader = new AlbumArtLoader(mContext)
                .setArtLoadedWatcher(this)
                .setSong(curSong)
                .setEnableInternetSearch(true)
                .setPaletteGeneratedWatcher(new PaletteGeneratedWatcher() {
                    @Override
                    public void onPaletteGenerated(Palette palette) {
                        if (mWatcher != null) {
                            //pass the generated palette back to the activity/fragment etc...
                            mWatcher.onPaletteGenerated(palette);
                        }
                    }
                })
                .loadInBackground();

    }

    private void showRatingHeart(final int rating, boolean animate) {
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
        mCurRating.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(mContext, rating + "/100", Toast.LENGTH_SHORT).show();
                return false;
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
    public void onArtLoaded(Drawable artwork) {
        mImageSwitcher.setImageDrawable(artwork);
    }

    @Override
    public void onLoadProgressChanged(LoadProgress progress) {
        //TODO
    }
}
