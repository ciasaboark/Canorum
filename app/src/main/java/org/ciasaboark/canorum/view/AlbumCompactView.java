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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.ciasaboark.canorum.Album;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.Song;
import org.ciasaboark.canorum.artwork.albumart.AlbumArtLoader;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.playlist.SystemLibrary;

import java.util.List;

/**
 * Created by Jonathan Nelson on 2/6/15.
 */
public class AlbumCompactView extends LinearLayout {

    private final Context mContext;
    private final AttributeSet mAttrs;
    private TextView mAlbumTitle;
    private ImageSwitcher mAlbumArt;
    private LinearLayout mSongContainer;
    private View mLayout;
    private Album mAlbum;
    private TextView mAlbumYear;

    public AlbumCompactView(Context ctx, AttributeSet attr, Album album) {
        super(ctx, attr);
        mContext = ctx;
        mAttrs = attr;
        mAlbum = album;
        init();
    }

    private void init() {
        mLayout = (LinearLayout) inflate(mContext, R.layout.view_album_compact, this);
        mAlbumTitle = (TextView) mLayout.findViewById(R.id.album_compact_header_title);
        mAlbumYear = (TextView) mLayout.findViewById(R.id.album_compact_header_year);
        mAlbumArt = (ImageSwitcher) mLayout.findViewById(R.id.album_compact_header_image);
        mSongContainer = (LinearLayout) mLayout.findViewById(R.id.album_compact_song_container);
        initImageSwitcher();
        drawHeader();
        fillSongContainer();
    }

    private void initImageSwitcher() {
        mAlbumArt.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView iv = new ImageView(mContext);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setLayoutParams(new ImageSwitcher.LayoutParams(RelativeLayout.LayoutParams.
                        FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT));
                return iv;
            }
        });
        Animation in = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);
        Animation out = AnimationUtils.loadAnimation(mContext, R.anim.fade_out);
        mAlbumArt.setInAnimation(in);
        mAlbumArt.setOutAnimation(out);
        mAlbumArt.setImageDrawable(getResources().getDrawable(R.drawable.default_background));
    }

    private void drawHeader() {
        mAlbumTitle.setText(mAlbum.getAlbumName());
        mAlbumYear.setText(mAlbum.getYear());
        AlbumArtLoader albumArtLoader = new AlbumArtLoader(mContext)
                .setAlbum(mAlbum)
                .setInternetSearchEnabled(true)
                .setArtLoadedWatcher(new ArtLoadedWatcher() {
                    @Override
                    public void onArtLoaded(Drawable artwork) {
                        mAlbumArt.setImageDrawable(artwork);
                    }

                    @Override
                    public void onLoadProgressChanged(LoadProgress progress) {
                        //ignore
                    }
                })
                .setPaletteGeneratedWatcher(new PaletteGeneratedWatcher() {
                    @Override
                    public void onPaletteGenerated(Palette palette) {
                        Palette.Swatch muted = palette.getMutedSwatch();
                        Palette.Swatch darkmuted = palette.getDarkMutedSwatch();
                        Palette.Swatch vibrant = palette.getVibrantSwatch();
                        Palette.Swatch darkVibrant = palette.getDarkVibrantSwatch();
                        int color;

                        if (darkVibrant != null) {
                            color = darkVibrant.getRgb();
                        } else if (darkmuted != null) {
                            color = darkmuted.getRgb();
                        } else if (muted != null) {
                            color = muted.getRgb();
                        } else if (vibrant != null) {
                            color = vibrant.getRgb();
                        } else {
                            color = -1;
                        }

                        if (color != -1) {
                            final RelativeLayout container = (RelativeLayout) mLayout.findViewById(R.id.album_compact_header);
                            int oldColor;
                            Drawable d = container.getBackground();
                            if (d != null && d instanceof ColorDrawable) {
                                oldColor = ((ColorDrawable) d).getColor();
                                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), oldColor, color);
                                colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animator) {
                                        int color = (Integer) animator.getAnimatedValue();
                                        container.setBackgroundColor(color);
                                    }

                                });
                                colorAnimation.start();
                            }
                        }
                    }
                })
                .loadInBackground();
    }

    private void fillSongContainer() {
        mSongContainer.removeAllViews();
        SystemLibrary systemLibrary = new SystemLibrary(mContext);
        List<Song> albumSongs = systemLibrary.getSongsForAlbum(mAlbum);
        for (Song song : albumSongs) {
            SongView songView = new SongView(mContext, null, song);
            mSongContainer.addView(songView);
        }
        //TODO
    }
}
