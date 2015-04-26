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
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.album.AlbumArtLoader;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.playlist.provider.MergedProvider;
import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.song.Artist;
import org.ciasaboark.canorum.song.Song;
import org.ciasaboark.canorum.song.Track;
import org.ciasaboark.canorum.song.shadow.ShadowAlbum;
import org.ciasaboark.canorum.song.shadow.ShadowLibraryAction;
import org.ciasaboark.canorum.song.shadow.ShadowLibraryFetcher;
import org.ciasaboark.canorum.song.shadow.ShadowLibraryLoadedListener;
import org.ciasaboark.canorum.song.shadow.ShadowSong;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Jonathan Nelson on 2/6/15.
 */
public class AlbumCompactView extends LinearLayout {

    private final Context mContext;
    private final AttributeSet mAttrs;
    List<Track> mAlbumTracks;
    List<ShadowSong> mShadowSongs = new ArrayList<ShadowSong>();
    private TextView mAlbumTitle;
    private ImageSwitcher mAlbumArt;
    private LinearLayout mSongContainer;
    private View mLayout;
    private Album mAlbum;
    private TextView mAlbumYear;
    private View mAlbumHeader;
    private OnClickListener mOnClickListener;
    private OnLongClickListener mLongClickListener;
    private ImageView mMenuButton;

    public AlbumCompactView(Context ctx, AttributeSet attr, Album album) {
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
        initAlbumList();
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
                MergedProvider provider = MergedProvider.getInstance(mContext);
                final List<Track> albumTracks = provider.getTracksForAlbum(mAlbum.getArtist().getArtistName(), mAlbum);
                if (!albumTracks.isEmpty()) {
                    PopupMenu menu = new PopupMenu(mContext, v);
                    menu.inflate(R.menu.menu_album);
                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            boolean itemHandled = false;
                            MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(mContext);
                            switch (item.getItemId()) {
                                case R.id.popup_menu_library_play_now:
                                    musicControllerSingleton.addTracksToQueueHead(albumTracks);
                                    musicControllerSingleton.playNext();
                                    itemHandled = true;
                                    break;
                                case R.id.popup_menu_library_play_next:
                                    musicControllerSingleton.addTracksToQueueHead(albumTracks);
                                    itemHandled = true;
                                    break;
                                case R.id.popup_menu_library_add_queue:
                                    musicControllerSingleton.addTracksToQueue(albumTracks);
                                    itemHandled = true;
                                    break;
                                case R.id.popup_menu_library_show_missing:
                                    findMissingTracks();
                                    itemHandled = true;
                                    break;
                            }
                            return itemHandled;
                        }
                    });
                    menu.show();
                }

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

        AlbumArtLoader albumArtLoader = new AlbumArtLoader(mContext)
                .setAlbum(mAlbum)
                .setArtSize(ArtSize.SMALL)
                .setInternetSearchEnabled(true)
                .setArtLoadedListener(new ArtLoadedWatcher() {
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

    private void initAlbumList() {
        MergedProvider provider = MergedProvider.getInstance(mContext);
        mAlbumTracks = provider.getTracksForAlbum(mAlbum.getArtist().getArtistName(), mAlbum);
        Collections.sort(mAlbumTracks, new Comparator<Track>() {
            @Override
            public int compare(Track lhs, Track rhs) {
                Integer leftSongNum = lhs.getSong().getRawTrackNum();
                Integer rightSongNum = rhs.getSong().getRawTrackNum();
                int comp = leftSongNum.compareTo(rightSongNum);
                return comp;
            }
        });
    }

    private void findMissingTracks() {
        final ProgressBar progressBar = (ProgressBar) mLayout.findViewById(R.id.album_image_progressbar);
        progressBar.setVisibility(View.VISIBLE);

        Artist artist = new Artist(-1, mAlbum.getArtist().getArtistName());
        ShadowLibraryFetcher libraryFetcher = new ShadowLibraryFetcher((Activity) mContext)
                .setArtist(artist)
                .setShadowLibraryListener(new ShadowLibraryLoadedListener() {
                    @Override
                    public void onShadowLibraryLoaded(List<ShadowAlbum> shadowLibrary) {
                        //nothing to do here
                    }

                    @Override
                    public void onShadowAlbumLoaded(ShadowAlbum shadowAlbum) {
                        if (shadowAlbum != null) {
                            for (ShadowSong song : shadowAlbum.getSongs()) {
                                addShadowSong(song);
                            }
                        }
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onShadowLibraryUpdate(ShadowLibraryAction action, String message) {
                        //nothing to do here
                    }

                    @Override
                    public void onAlbumTitlesLoaded(List<String> albumTitles) {
                        //nothing to do here
                    }
                })
                .loadAlbumsInBackground(new String[]{mAlbum.getAlbumName()});
    }

    private void fillSongContainer() {
        mSongContainer.removeAllViews();
        int albumCount = getAlbumCountForTracks(mAlbumTracks);
        List<Integer> albumHeaders = new ArrayList<Integer>();

        //the merged list needs to be Track based instead of song based, so that onclicks
        // will work properly
        List<Track> mergedSongList = new ArrayList<Track>();
        for (Track track : mAlbumTracks) {
            mergedSongList.add(track);
        }

        Artist fakeArtist = new Artist(-1, mAlbum.getArtist().getArtistName());
        Album fakeAlbum = mAlbum;
        for (Song shadowSong : mShadowSongs) {
            Track fakeTrack = new Track(shadowSong, null, null);
            mergedSongList.add(fakeTrack);
        }
        Collections.sort(mergedSongList, new Comparator<Track>() {
            @Override
            public int compare(Track lhs, Track rhs) {
                Integer leftTrackNum = lhs.getSong().getRawTrackNum();
                Integer rightTrackNum = rhs.getSong().getRawTrackNum();
                return leftTrackNum.compareTo(rightTrackNum);
            }
        });

        for (Track track : mergedSongList) {
            int discNum = track.getSong().getDiskNum();
            if (albumCount > 1 && !albumHeaders.contains(discNum)) {
                albumHeaders.add(discNum);
                TextView discHeader = new TextView(mContext);
                discHeader.setText("Disc " + String.valueOf(discNum));
                mSongContainer.addView(discHeader);
            }
            if (track.getSong() instanceof ShadowSong) {
                ShadowSongView shadowSongView = new ShadowSongView(mContext, null, (ShadowSong) track.getSong(), true);
                shadowSongView.setArtist(fakeArtist);
                mSongContainer.addView(shadowSongView);
            } else {
                SongView songView = new SongView(mContext, null, track, true);
                mSongContainer.addView(songView);
            }
        }

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

    public Album getAlbum() {
        return mAlbum;
    }

    public void addShadowSong(ShadowSong shadowSong) {
        List<ShadowSong> singleSongList = new ArrayList<ShadowSong>();
        singleSongList.add(shadowSong);
        addShadowSongs(singleSongList);
    }

    public void addShadowSongs(List<ShadowSong> shadowSongs) {
        boolean songAdded = false;
        for (ShadowSong shadowSong : shadowSongs) {
            if (shadowSong == null) {
                throw new IllegalArgumentException("shadowSong can not be null");
            }

            //skip adding the shadow song if a similar song already exists
            //since its possible that the track titles are similar but not exactly the same
            //we will also filter by the track number
            boolean addSong = true;
            for (Track track : mAlbumTracks) {
                String songName = track.getSong().getTitle();
                String shadowSongName = shadowSong.getTitle();
                Integer songTrackNum = track.getSong().getFormattedTrackNum();
                Integer shadowTrackNum = shadowSong.getFormattedTrackNum();
                if (songName.equals(shadowSongName)) {
                    addSong = false;
                } else if (songTrackNum.equals(shadowTrackNum)) {
                    addSong = false;
                }
            }

            if (addSong) {
                mShadowSongs.add(shadowSong);
                songAdded = true;
            }
        }
        if (songAdded) {
            fillSongContainer();
        }

    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
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
