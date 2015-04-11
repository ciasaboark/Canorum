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

package org.ciasaboark.canorum.fragment;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.melnykov.fab.FloatingActionButton;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.album.AlbumArtLoader;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.details.DetailsFetcher;
import org.ciasaboark.canorum.details.DetailsLoadedWatcher;
import org.ciasaboark.canorum.details.article.Article;
import org.ciasaboark.canorum.details.types.Details;
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
import org.ciasaboark.canorum.view.HidingToolbar;
import org.ciasaboark.canorum.view.ShadowSongView;
import org.ciasaboark.canorum.view.SongView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ArtistDetailFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ArtistDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AlbumDetailFragment extends Fragment {
    private static final String TAG = "AlbumDetailFragment";
    private static final String KEY_ALBUM = "param1";
    private static final String KEY_FILL_SHADOW_SONGS = "fill_shadow";
    private static Drawable sInitialArt;
    FloatingActionButton mFab;
    private View mView;
    private Album mAlbum;
    private boolean mIsTextExpanded = false;
    private boolean mHidden = true;
    private boolean mFillShadowSongs = false;
    private List<Track> mAlbumTracks;
    private List<ShadowSong> mShadowSongs;

    private OnFragmentInteractionListener mListener;

    private TextView mAlbumTitle;
    private ImageSwitcher mAlbumImage;
    private View mTextBox;
    private TextView mWikiText;
    private RelativeLayout mLinkBox;
    private TextView mLinkText;
    private LinearLayout mSongsContainer;
    private HidingToolbar mToolbar;
    private ScrollView mScrollview;

    public AlbumDetailFragment() {
        // Required empty public constructor
    }

    public static AlbumDetailFragment newInstance(Album album) {
        return newInstance(album, null);
    }

    public static AlbumDetailFragment newInstance(Album album, Drawable albumArt) {
        AlbumDetailFragment fragment = new AlbumDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ALBUM, album);
        sInitialArt = albumArt;
        fragment.setArguments(args);
        return fragment;
    }

    public static AlbumDetailFragment newInstance(ShadowAlbum album) {
        return newInstance(album, null);
    }

    public static AlbumDetailFragment newInstance(ShadowAlbum album, Drawable albumArt) {
        Album fakeAlbum = new Album(-1, album.getAlbumName(), album.getYear(),
                album.getSongs().size(), album.getArtist());
        AlbumDetailFragment fragment = new AlbumDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ALBUM, fakeAlbum);
        args.putBoolean(KEY_FILL_SHADOW_SONGS, true);
        sInitialArt = albumArt;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        Animation anim;
        if (enter) {
            anim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
        } else {
            anim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
        }

        anim.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                Log.d(TAG, "fragment animation completed");

                if (!mAlbumTracks.isEmpty()) {
                    showFloatingActionButton();
                }

                AlbumArtLoader artLoader = new AlbumArtLoader(getActivity())
                        .setAlbum(mAlbum)
                        .setArtSize(ArtSize.LARGE)
                        .setInternetSearchEnabled(true)
                        .setArtLoadedWatcher(new ArtLoadedWatcher() {
                            @Override
                            public void onArtLoaded(final Drawable artwork, Object tag) {

                                if (mAlbumImage != null) {
                                    if (artwork == null) {
                                        //apply the default image if one has not already been applied
                                        int curChild = mAlbumImage.getDisplayedChild();
                                        ImageView curView = (ImageView) mAlbumImage.getChildAt(curChild);
                                        if (curView.getDrawable() == null) {
                                            mAlbumImage.setImageDrawable(getResources().getDrawable(R.drawable.default_album_art));
                                        }
                                    } else {
                                        mAlbumImage.setImageDrawable(artwork);
                                    }
                                }
                            }

                            @Override
                            public void onLoadProgressChanged(LoadProgress progress) {
                                //ignore
                            }
                        })
                        .setPaletteGeneratedWatcher(new PaletteGeneratedWatcher() {
                            @Override
                            public void onPaletteGenerated(Palette palette) {
                                applyPalette(palette);
                            }
                        })
                        .loadInBackground();
            }

            public void onAnimationRepeat(Animation animation) {
            }
        });

        return anim;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAlbum = (Album) getArguments().getSerializable(KEY_ALBUM);
            mFillShadowSongs = getArguments().getBoolean(KEY_FILL_SHADOW_SONGS, mFillShadowSongs);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_album_detail, container, false);
        findChildren();
        initToolbar();
        initAlbumDetails();
        buildAlbumTrackList();
        fillAlbumList();
        fillShadowSongsIfNeeded();
        initFab();
        return mView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        if (mToolbar != null) {
            mToolbar.detatchScrollView();
        }
    }

    public void showFloatingActionButton() {
        if (mHidden) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(mFab, "scaleX", 0, 1);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(mFab, "scaleY", 0, 1);
            AnimatorSet animSetXY = new AnimatorSet();
            animSetXY.playTogether(scaleX, scaleY);
            animSetXY.setInterpolator(new AccelerateInterpolator());
            animSetXY.setDuration(300);
            mFab.setVisibility(View.VISIBLE);
            animSetXY.start();
            mHidden = false;
        }
    }

    private void applyPalette(Palette palette) {
        Palette.Swatch darkVibrant = palette.getDarkVibrantSwatch();
        Palette.Swatch muted = palette.getMutedSwatch();
        Palette.Swatch darkmuted = palette.getDarkMutedSwatch();

        int color = palette.getDarkVibrantColor(
                palette.getMutedColor(
                        palette.getDarkMutedColor(-1)
                )
        );

        if (color == -1) {
//                            mListener.setToolbarTransparent();
        } else {
//                            mListener.setToolbarColor(color);
            final RelativeLayout wikiTextBox = (RelativeLayout) mView.findViewById(R.id.album_detail_text_box);
            Drawable d = wikiTextBox.getBackground();
            if (d != null && d instanceof ColorDrawable) {
                int oldColor = ((ColorDrawable) d).getColor();
                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), oldColor, color);
                colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator animator) {
                        int color = (Integer) animator.getAnimatedValue();
                        wikiTextBox.setBackgroundColor(color);
                    }
                });
                colorAnimation.start();
            }
        }

        int vibrantColor = palette.getVibrantColor(-1);
        if (vibrantColor != -1) {
            int oldColor = mFab.getColorNormal();
            final ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), oldColor, vibrantColor);
            colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int color = (Integer) colorAnimator.getAnimatedValue();
                    mFab.setColorNormal(color);
                    mFab.setColorPressed(color);
                    mToolbar.setFadeInBackground(new ColorDrawable(color));
                }
            });
            colorAnimator.start();
        }
    }

    private void findChildren() {
        mScrollview = (ScrollView) mView.findViewById(R.id.scrollview);
        mSongsContainer = (LinearLayout) mView.findViewById(R.id.album_detail_songs_container);
        mAlbumTitle = (TextView) mView.findViewById(R.id.album_detail_title);
        mAlbumImage = (ImageSwitcher) mView.findViewById(R.id.albumImage);
        mTextBox = mView.findViewById(R.id.album_detail_text_box);
        mWikiText = (TextView) mView.findViewById(R.id.album_detail_wikipedia);
        mLinkBox = (RelativeLayout) mView.findViewById(R.id.album_detail_wikipedia_more_box);
        mLinkText = (TextView) mView.findViewById(R.id.album_detail_wikipedia_more_link);
        mFab = (FloatingActionButton) mView.findViewById(R.id.fab);
    }

    private void fillShadowSongs() {
        ShadowLibraryFetcher shadowLibraryFetcher = new ShadowLibraryFetcher(getActivity())
                .setArtist(new Artist(-1, mAlbum.getArtist().getArtistName()))
                .setShadowLibraryListener(new ShadowLibraryLoadedListener() {
                    @Override
                    public void onShadowLibraryLoaded(List<ShadowAlbum> shadowLibrary) {
                        //nothing to do here
                    }

                    @Override
                    public void onShadowAlbumLoaded(ShadowAlbum shadowAlbum) {
                        if (shadowAlbum != null) {
                            mShadowSongs = shadowAlbum.getSongs();
                            fillAlbumList();
                        }
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

    private void fillShadowSongsIfNeeded() {
        if (mFillShadowSongs) {
            fillShadowSongs();
        }
    }

    private void initAlbumDetails() {
        mTextBox.setEnabled(false);
        mTextBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsTextExpanded) {
                    //shrink the textview back
                    ObjectAnimator animator = ObjectAnimator.ofInt(
                            mWikiText,
                            "maxLines",
                            2
                    );
                    animator.setDuration(500);
                    animator.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            mLinkBox.setVisibility(View.GONE);
                            mTextBox.setClickable(false);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mTextBox.setClickable(true);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
                    animator.start();

                    mIsTextExpanded = false;
                } else {
                    ObjectAnimator animator = ObjectAnimator.ofInt(
                            mWikiText,
                            "maxLines",
                            30
                    );
                    animator.setDuration(500);
                    animator.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            mTextBox.setClickable(false);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLinkBox.setVisibility(View.VISIBLE);
                            mTextBox.setClickable(true);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
                    animator.start();
                    mIsTextExpanded = true;

                }
            }
        });
        mAlbumImage.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView iv = new ImageView(getActivity());
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setLayoutParams(new ImageSwitcher.LayoutParams(RelativeLayout.LayoutParams.
                        FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT));
                return iv;
            }
        });
        //set the initial album artowrk before we apply any animations to the view switcher
        if (sInitialArt == null) {
            mAlbumImage.setImageDrawable(getResources().getDrawable(R.drawable.default_album_art)); //TODO use default artist artwork
        } else {
            mAlbumImage.setImageDrawable(sInitialArt);
            generatePaletteAsync((BitmapDrawable) sInitialArt);
            sInitialArt = null;
        }

        mAlbumTitle.setText(mAlbum.getAlbumName());

        if (mAlbum.getArtist().getArtistName().equals("<unknown>")) {
            mWikiText.setText("The tracks listed below do not have any proper artist information attached.");
        } else {
            DetailsFetcher articleFetcher = new DetailsFetcher(getActivity())
                    .setArticleSource(mAlbum)
                    .setArticleLoadedWatcher(new DetailsLoadedWatcher() {
                        @Override
                        public void onDetailsLoaded(Details details) {
                            Article article = details.getArticle();

                            Context ctx = getActivity();
                            if (ctx != null) {
                                if (article == null) {
                                    mWikiText.setText("Could not load album information");
                                } else {
                                    mWikiText.setText(article.getFirstParagraph());
                                    String source;
                                    Drawable sourceIcon;
                                    switch (article.getSource()) {
                                        case LASTFM:
                                            source = "Last.fm";
                                            sourceIcon = getResources().getDrawable(R.drawable.ic_lastfm_white_24dp);
                                            break;
                                        default:
                                            source = "Wikipedia";
                                            sourceIcon = getResources().getDrawable(R.drawable.ic_wikipedia_white_24dp);
                                            break;
                                    }
                                    String linkUrl = "<a href=\"" + article.getArticleUrl() + "\">Read more on " + source + "</a>";
                                    mLinkText.setText(Html.fromHtml(linkUrl));
                                    mLinkText.setMovementMethod(LinkMovementMethod.getInstance());
                                    mLinkText.setLinkTextColor(getResources().getColor(R.color.accent_material_dark));
                                    ImageView linkIcon = (ImageView) mView.findViewById(R.id.album_detail_wikipedia_more_icon);
                                    linkIcon.setImageDrawable(sourceIcon);
                                    mTextBox.setEnabled(true);
                                }
                            }
                        }
                    })
                    .loadInBackground();


        }
    }

    private void buildAlbumTrackList() {
        MergedProvider systemLibrary = MergedProvider.getInstance(getActivity());
        mAlbumTracks = systemLibrary.getTracksForAlbum(mAlbum.getArtist().getArtistName(), mAlbum);
    }

    private void fillAlbumList() {
        mSongsContainer.removeAllViews();

        //the merged list needs to be Track based instead of song based, so that onclicks
        // will work properly
        List<Track> mergedSongList = new ArrayList<Track>();
        if (mAlbumTracks != null) {
            for (Track track : mAlbumTracks) {
                mergedSongList.add(track);
            }
        }

        Artist fakeArtist = new Artist(-1, mAlbum.getArtist().getArtistName());
        Album fakeAlbum = mAlbum;
        if (mShadowSongs != null) {
            for (Song shadowSong : mShadowSongs) {
                Track fakeTrack = new Track(shadowSong, null, null);
                mergedSongList.add(fakeTrack);
            }
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
            if (track.getSong() instanceof ShadowSong) {
                ShadowSongView shadowSongView = new ShadowSongView(getActivity(), null, (ShadowSong) track.getSong(), false);
                shadowSongView.setArtist(fakeArtist);
                mSongsContainer.addView(shadowSongView);
            } else {
                SongView songView = new SongView(getActivity(), null, track, false);
                mSongsContainer.addView(songView);
            }
        }
    }

    private void initToolbar() {
        mToolbar = (HidingToolbar) mView.findViewById(R.id.local_toolbar);
        mToolbar.setTitleTextColor(getResources().getColor(R.color.toolbar_title_text));
        mToolbar.setTitle(mAlbum.getAlbumName());
        mToolbar.setNavigationIcon(R.drawable.ic_keyboard_backspace_white_24dp);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        if (!mFillShadowSongs) {
            mToolbar.inflateMenu(R.menu.menu_album_details);
            mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    boolean itemHandled = false;
                    switch (menuItem.getItemId()) {
                        case R.id.menu_show_missing:
                            fillShadowSongs();
                            mToolbar.getMenu().clear();
                    }
                    return itemHandled;
                }
            });
        }
        mToolbar.attachScrollView(mScrollview)
                .setFadeInBackground(new ColorDrawable(getResources().getColor(R.color.color_primary)));
    }

    private void initFab() {
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MergedProvider provider = MergedProvider.getInstance(getActivity());
                List<Track> tracks = provider.getTracksForAlbum(mAlbum.getArtist().getArtistName(), mAlbum);
                MusicControllerSingleton controller = MusicControllerSingleton.getInstance(getActivity());
                controller.addTracksToQueue(tracks);
                controller.playNext();
            }
        });
    }

    private void generatePaletteAsync(BitmapDrawable drawable) {
        Bitmap bitmap = null;
        try {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
            Palette.generateAsync(bitmap,
                    new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            applyPalette(palette);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "could not generate palette from artwork Drawable: " + e.getMessage());
        }
    }

    public void hideFloatingActionButton() {
        if (!mHidden) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(mFab, "scaleX", 1, 0);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(mFab, "scaleY", 1, 0);
            AnimatorSet animSetXY = new AnimatorSet();
            animSetXY.playTogether(scaleX, scaleY);
            animSetXY.setInterpolator(new AccelerateInterpolator());
            animSetXY.setDuration(100);
            animSetXY.start();
            mHidden = true;
        }
    }
}
