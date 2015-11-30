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

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.genre.GenreArtGenerator;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedListener;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.details.DetailsFetcher;
import org.ciasaboark.canorum.details.DetailsLoadedWatcher;
import org.ciasaboark.canorum.details.types.Details;
import org.ciasaboark.canorum.playlist.provider.MergedProvider;
import org.ciasaboark.canorum.song.Genre;
import org.ciasaboark.canorum.song.Track;
import org.ciasaboark.canorum.view.HidingToolbar;
import org.ciasaboark.canorum.view.SongView;

import java.util.List;


public class GenreDetailFragment extends Fragment {
    private static final String KEY_GENRE = "genre";
    private static final String TAG = "GenreDetailFragment";
    List<Track> mTracks;
    private OnFragmentInteractionListener mListener;
    private Genre mGenre;
    private View mView;
    private MusicControllerSingleton mController;

    private ImageView mGenreImage;
    private TextView mGenreDetailText;
    private View mGenreDetailTextBox;
    private LinearLayout mGenreSongsHolder;
    private FloatingActionButton mFab;
    private HidingToolbar mToolbar;
    private ScrollView mScrollView;

    public static GenreDetailFragment newInstance(Genre genre) {
        GenreDetailFragment fragment = new GenreDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_GENRE, genre);
        fragment.setArguments(args);
        return fragment;
    }

    public GenreDetailFragment() {
        // Required empty public constructor
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
                if (!mTracks.isEmpty()) {
                    showFloatingActionButton();
                }
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
            mGenre = (Genre) getArguments().getSerializable(KEY_GENRE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_genre_detail, container, false);
        mController = MusicControllerSingleton.getInstance(getActivity());
        MergedProvider mergedProvider = MergedProvider.getInstance(getActivity());
        mTracks = mergedProvider.getTracksForGenre(mGenre);
        init();
        return mView;
    }

    private void init() {
        mScrollView = (ScrollView) mView.findViewById(R.id.scrollview);
        mGenreImage = (ImageView) mView.findViewById(R.id.genre_image);
        mGenreDetailText = (TextView) mView.findViewById(R.id.genre_detail_text);
        mGenreDetailTextBox = mView.findViewById(R.id.genre_detail_text_box);
        mGenreSongsHolder = (LinearLayout) mView.findViewById(R.id.genre_songs_container);
        mFab = (FloatingActionButton) mView.findViewById(R.id.fab);
        mToolbar = (HidingToolbar) mView.findViewById(R.id.local_toolbar);

        initGenreImage();
        initGenreDetailText();
        initGenreSongList();
        initFab();
        initToolbar();
    }

    private void initGenreImage() {
        int width = mGenreImage.getWidth();
        int height = mGenreImage.getHeight();
        if (width == 0 || height == 0) {
            width = 1000;
            height = 1000;
        }
        GenreArtGenerator generator = new GenreArtGenerator(getActivity())
                .setArtDimensions(width, height)
                .setArtLoadedWatcher(new ArtLoadedListener() {
                    @Override
                    public void onArtLoaded(Drawable artwork, Object tag) {
                        mGenreImage.setImageDrawable(artwork);
                    }

                    @Override
                    public void onLoadProgressChanged(LoadProgress progress) {

                    }
                })
                .setGenre(mGenre)
                .setPalletGeneratedWatcher(new PaletteGeneratedWatcher() {
                    @Override
                    public void onPaletteGenerated(Palette palette, Object tag) {
                        applyPalette(palette);
                    }
                })
                .generateInBackground();
    }

    private void initGenreDetailText() {
        DetailsFetcher fetcher = new DetailsFetcher(getActivity())
                .setArticleLoadedWatcher(new DetailsLoadedWatcher() {
                    @Override
                    public void onDetailsLoaded(Details details) {
                        if (details == null) {
                            mGenreDetailText.setText("Unable to load genre information.");  //TODO stringify
                        } else {
                            mGenreDetailText.setText(details.getArticle().getFirstParagraph());
                        }
                    }
                })
                .setArticleSource(mGenre)
                .loadInBackground();
    }

    private void initGenreSongList() {
        mGenreSongsHolder.removeAllViews();
        for (final Track track : mTracks) {
            final SongView songView = new SongView(getActivity(), null, track);
            mGenreSongsHolder.addView(songView);
        }
    }

    private void initFab() {
        //hide the fab
        mFab.setVisibility(View.GONE);

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mController.replaceQueue(mTracks);
                mController.playNext();
            }
        });
    }

    private void initToolbar() {
        mToolbar.setTitle(mGenre.getGenre());
        mToolbar.setNavigationIcon(R.drawable.ic_keyboard_backspace_white_24dp);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        mToolbar.attachScrollView(mScrollView)
                .setFadeInBackground(new ColorDrawable(getResources().getColor(R.color.color_primary)));
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

        if (color != -1) {
            Drawable d = mGenreDetailTextBox.getBackground();
            if (d != null && d instanceof ColorDrawable) {
                int oldColor = ((ColorDrawable) d).getColor();
                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), oldColor, color);
                colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator animator) {
                        int color = (Integer) animator.getAnimatedValue();
                        mGenreDetailTextBox.setBackgroundColor(color);
                        mToolbar.setFadeInBackground(new ColorDrawable(color));
                    }
                });
                colorAnimation.start();
            }
        }
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
        if (mFab.getVisibility() != View.VISIBLE) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(mFab, "scaleX", 0, 1);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(mFab, "scaleY", 0, 1);
            AnimatorSet animSetXY = new AnimatorSet();
            animSetXY.playTogether(scaleX, scaleY);
            animSetXY.setInterpolator(new AccelerateInterpolator());
            animSetXY.setDuration(300);
            mFab.setVisibility(View.VISIBLE);
            animSetXY.start();
        }
    }
}
