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

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.artist.ArtistArtLoader;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.song.Artist;

/**
 * Created by Jonathan Nelson on 3/4/15.
 */
public class SimilarArtistPortrait extends RelativeLayout {
    private static final String TAG = "SimilarArtistPortrait";
    private final Context mContext;
    private final Artist mArtist;
    TextView mArtistTextView;
    RelativeLayout mArtistTextHolder;
    ImageView mArtistImageView;
    private View mRootView;

    public SimilarArtistPortrait(Context ctx, AttributeSet attrs) {
        this(ctx, attrs, null);
    }

    public SimilarArtistPortrait(Context ctx, AttributeSet attrs, Artist artist) {
        super(ctx, attrs);
        mArtist = artist;
        mContext = ctx;
        init();
    }

    private void init() {
        mRootView = (RelativeLayout) inflate(mContext, R.layout.view_similar_artist, this);
        findChildren();
        initTextView();
        initArtistArt();
    }

    private void findChildren() {
        mArtistTextView = (TextView) mRootView.findViewById(R.id.artist_name);
        mArtistTextHolder = (RelativeLayout) mRootView.findViewById(R.id.artist_text_holder);
        mArtistImageView = (ImageView) mRootView.findViewById(R.id.artist_image);
    }

    private void initTextView() {
        if (mArtist != null) {
            mArtistTextView.setText(mArtist.getArtistName());
        }
    }

    private void initArtistArt() {
        if (mArtist != null) {

            ArtistArtLoader artLoader = new ArtistArtLoader(mContext)
                    .setArtist(mArtist)
                    .setArtSize(ArtSize.SMALL)
                    .setInternetSearchEnabled(true)
                    .setProvideDefaultArtwork(false)
                    .setArtLoadedWatcher(new ArtLoadedWatcher() {
                        @Override
                        public void onArtLoaded(Drawable artwork, Object tag) {
                            if (mArtistImageView != null)
                                mArtistImageView.setImageDrawable(artwork);
                        }

                        @Override
                        public void onLoadProgressChanged(LoadProgress progress) {

                        }
                    })
                    .setPaletteGeneratedWatcher(new PaletteGeneratedWatcher() {
                        @Override
                        public void onPaletteGenerated(Palette palette) {
                            int oldColor = getTagBackgroundColor();
                            int newColor = palette.getDarkVibrantColor(
                                    palette.getDarkMutedColor(
                                            palette.getMutedColor(
                                                    palette.getVibrantColor(
                                                            oldColor
                                                    )
                                            )
                                    )
                            );

                            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), oldColor, newColor);
                            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animator) {
                                    int color = (Integer) animator.getAnimatedValue();
                                    mArtistTextHolder.setBackgroundColor(color);
                                }
                            });
                            colorAnimation.start();
                        }
                    })
                    .loadInBackground();
        }
    }

    public int getTagBackgroundColor() {
        Drawable drawable = mArtistTextHolder.getBackground();
        int backgroundColor = -1;
        if (drawable instanceof ColorDrawable) {
            backgroundColor = ((ColorDrawable) drawable).getColor();
        }

        return backgroundColor;
    }

    public void setTagBackgroundColor(int color) {
        mArtistTextHolder.setBackgroundColor(color);
    }
}
