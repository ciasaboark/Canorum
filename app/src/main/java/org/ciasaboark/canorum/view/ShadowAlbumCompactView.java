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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.album.AlbumArtLoader;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.song.Track;
import org.ciasaboark.canorum.song.shadow.ShadowAlbum;
import org.ciasaboark.canorum.song.shadow.ShadowSong;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Jonathan Nelson on 2/6/15.
 */
public class ShadowAlbumCompactView extends LinearLayout {
    private static final String TAG = "ShadowAlbumCompactView";

    private final Context mContext;
    private final AttributeSet mAttrs;
    private TextView mAlbumTitle;
    private ImageSwitcher mAlbumArt;
    private LinearLayout mSongContainer;
    private View mLayout;
    private ShadowAlbum mAlbum;
    private TextView mAlbumYear;
    private View mAlbumHeader;
    private OnClickListener mOnClickListener;
    private OnLongClickListener mLongClickListener;
    private ImageView mMenuButton;

    public ShadowAlbumCompactView(Context ctx, AttributeSet attr, ShadowAlbum album) {
        super(ctx, attr);
        mContext = ctx;
        mAttrs = attr;
        mAlbum = album;
        init();
    }

    private void init() {
        mLayout = (LinearLayout) inflate(mContext, R.layout.view_album_compact, this);
        mAlbumHeader = mLayout.findViewById(R.id.album_compact_header);
        mAlbumTitle = (TextView) mLayout.findViewById(R.id.album_compact_header_title);
        mAlbumYear = (TextView) mLayout.findViewById(R.id.album_compact_header_year);
        mAlbumArt = (ImageSwitcher) mLayout.findViewById(R.id.albumImage);
        mSongContainer = (LinearLayout) mLayout.findViewById(R.id.album_compact_song_container);
        mMenuButton = (ImageView) mLayout.findViewById(R.id.album_compat_play_button);
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
        mMenuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu menu = new PopupMenu(mContext, mMenuButton);
                menu.inflate(R.menu.menu_shop_sites);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        boolean itemHandled = false;
                        MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(mContext);
                        switch (item.getItemId()) {
                            case R.id.popup_menu_shop_amazon:
                                Toast.makeText(mContext, "Not yet implemented", Toast.LENGTH_SHORT).show();
                                itemHandled = true;
                                break;
                            case R.id.popup_menu_shop_google_play:
                                Toast.makeText(mContext, "Not yet implemented", Toast.LENGTH_SHORT).show();
                                itemHandled = true;
                                break;
                            case R.id.popup_menu_shop_youtube:
                                boolean searchLaunched = false;
                                String searchQuery = mAlbum.getArtist() + " " + mAlbum.getAlbumName();
                                try {
                                    Intent youtubeIntent = new Intent(Intent.ACTION_SEARCH);
                                    youtubeIntent.setPackage("com.google.android.youtube");
                                    youtubeIntent.putExtra("query", searchQuery);
                                    youtubeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    mContext.startActivity(youtubeIntent);
                                    searchLaunched = true;
                                } catch (ActivityNotFoundException e) {
                                    //if the youtube app is not installed then we can just launch a regular web query
                                    Intent webIntent = new Intent(Intent.ACTION_VIEW);
                                    try {
                                        String encodedQueryString = URLEncoder.encode(searchQuery, "UTF-8");
                                        String baseUrl = "https://www.youtube.com/results?search_query=";
                                        webIntent.setData(Uri.parse(baseUrl + encodedQueryString));
                                        mContext.startActivity(webIntent);
                                        searchLaunched = true;
                                    } catch (UnsupportedEncodingException ex) {
                                        Log.e(TAG, "unable to launch search query for string:'" + searchQuery + "': " + ex.getMessage());
                                    }

                                }

                                if (searchLaunched) {
                                    if (musicControllerSingleton.isPlaying())
                                        musicControllerSingleton.pause(false);
                                }
                                itemHandled = searchLaunched;
                                break;
                        }
                        return itemHandled;
                    }
                });
                menu.show();

            }
        });
        mAlbumTitle.setText(mAlbum.getAlbumName());
        int albumYear = mAlbum.getYear();
        if (albumYear != 0 && albumYear != -1) {
            mAlbumYear.setText(String.valueOf(albumYear));
            mAlbumYear.setVisibility(View.VISIBLE);
        } else {
            mAlbumYear.setVisibility(View.GONE);
        }

        //convert this shadow album to a normal extended album so we can load artwork
        String albumName = mAlbum.getAlbumName();
        int year = mAlbum.getYear();
        int numSongs = mAlbum.getSongs().size();
        String artistName = mAlbum.getArtist().getArtistName();

        Album album = Album.newSimpleAlbum(albumName, artistName);

        AlbumArtLoader albumArtLoader = new AlbumArtLoader(mContext)
                .setAlbum(album)
                .setArtSize(ArtSize.SMALL)
                .setInternetSearchEnabled(true)
                .setArtLoadedWatcher(new ArtLoadedWatcher() {
                    @Override
                    public void onArtLoaded(final Drawable artwork, Object tag) {
                        mAlbumArt.setImageDrawable(artwork);
                    }

                    @Override
                    public void onLoadProgressChanged(LoadProgress progress) {
                        //ignore
                    }
                })
                .setPaletteGeneratedWatcher(new PaletteGeneratedWatcher() {
                    @Override
                    public void onPaletteGenerated(Palette palette, Object tag) {
                        Palette.Swatch muted = palette.getMutedSwatch();
                        Palette.Swatch darkmuted = palette.getDarkMutedSwatch();
                        Palette.Swatch vibrant = palette.getVibrantSwatch();
                        Palette.Swatch darkVibrant = palette.getDarkVibrantSwatch();

                        Palette.Swatch swatch = palette.getVibrantSwatch();
                        if (swatch == null) swatch = palette.getDarkVibrantSwatch();
                        if (swatch == null) swatch = palette.getMutedSwatch();
                        if (swatch == null) swatch = palette.getDarkMutedSwatch();

                        int headerColor = palette.getDarkVibrantColor(
                                palette.getMutedColor(
                                        palette.getDarkMutedColor(-1)
                                )
                        );

                        int bodyColor = palette.getVibrantColor(-1);


                        if (headerColor != -1) {
                            int oldColor;
                            Drawable d = mAlbumHeader.getBackground();
                            if (d != null && d instanceof ColorDrawable) {
                                oldColor = ((ColorDrawable) d).getColor();
                                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), oldColor, headerColor);
                                colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animator) {
                                        int color = (Integer) animator.getAnimatedValue();
                                        mAlbumHeader.setBackgroundColor(color);
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
        List<ShadowSong> albumTracks = mAlbum.getSongs();
        Collections.sort(albumTracks, new Comparator<ShadowSong>() {
            @Override
            public int compare(ShadowSong lhs, ShadowSong rhs) {
                Integer leftSongNum = lhs.getRawTrackNum();
                Integer rightSongNum = rhs.getRawTrackNum();
                int comp = leftSongNum.compareTo(rightSongNum);
                return comp;
            }
        });

        for (ShadowSong shadowSong : albumTracks) {
            //convert shadow song into regular song
            ShadowSongView songView = new ShadowSongView(mContext, null, shadowSong, true);
            songView.setArtist(mAlbum.getArtist());
            mSongContainer.addView(songView);
        }
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    private int getAlbumCountForTracks(List<Track> tracks) {
        List<Integer> knownDiscs = new ArrayList<Integer>();
        int discCount = 0;
        for (Track track : tracks) {
            int discNum = track.getSong().getDiskNum();
            if (!knownDiscs.contains(discNum)) {
                knownDiscs.add(discNum);
                discCount++;
            }
        }
        return discCount;
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        mOnClickListener = listener;
        mAlbumHeader.setOnClickListener(mOnClickListener);
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener listener) {
        mLongClickListener = listener;
        mAlbumHeader.setOnLongClickListener(mLongClickListener);
    }

    public ImageSwitcher getAlbumArt() {
        return mAlbumArt;
    }
}
