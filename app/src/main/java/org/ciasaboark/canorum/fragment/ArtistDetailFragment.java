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
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.artist.ArtistArtLoader;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.playlist.SystemLibrary;
import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.song.Artist;
import org.ciasaboark.canorum.view.AlbumCompactView;
import org.ciasaboark.canorum.wikipedia.ArticleLoadedWatcher;
import org.ciasaboark.canorum.wikipedia.WikipediaArticle;
import org.ciasaboark.canorum.wikipedia.WikipediaArticleFetcher;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ArtistDetailFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ArtistDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ArtistDetailFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String KEY_ARTIST = "param1";
    private View mView;

    // TODO: Rename and change types of parameters
    private Artist mArtist;
    private boolean mIsTextExpanded = false;

    private OnFragmentInteractionListener mListener;

    public ArtistDetailFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ArtistDetailFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ArtistDetailFragment newInstance(Artist artist) {
        ArtistDetailFragment fragment = new ArtistDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ARTIST, artist);
        fragment.setArguments(args);
        return fragment;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mArtist = (Artist) getArguments().getSerializable(KEY_ARTIST);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_artist_detail, container, false);
        fillAlbumList();
        initArtistDetails();
        return mView;
    }

    private void fillAlbumList() {
        LinearLayout albumsContainer = (LinearLayout) mView.findViewById(R.id.artist_detail_albums_container);
        albumsContainer.removeAllViews();

        SystemLibrary systemLibrary = new SystemLibrary(getActivity());
        List<Album> albums = systemLibrary.getAlbumsForArtist(mArtist);
        for (Album album : albums) {
            AlbumCompactView albumCompactView = new AlbumCompactView(getActivity(), null, album);
            albumsContainer.addView(albumCompactView);
        }
        //TODO
    }

    private void initArtistDetails() {
        TextView artistTitle = (TextView) mView.findViewById(R.id.artist_detail_title);
        final ImageSwitcher artistImage = (ImageSwitcher) mView.findViewById(R.id.artist_detail_image);
        final TextView wikiText = (TextView) mView.findViewById(R.id.artist_detail_wikipedia);
        final Button moreButton = (Button) mView.findViewById(R.id.artist_detail_wikipedia_more);
        final RelativeLayout linkBox = (RelativeLayout) mView.findViewById(R.id.artist_detail_wikipedia_more_box);
        final TextView linkText = (TextView) mView.findViewById(R.id.artist_detail_wikipedia_more_link);


        moreButton.setEnabled(false);
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsTextExpanded) {
                    //shrink the textview back
                    ObjectAnimator animator = ObjectAnimator.ofInt(
                            wikiText,
                            "maxLines",
                            2
                    );
                    animator.setDuration(500);
                    animator.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            linkBox.setVisibility(View.GONE);
                            moreButton.setClickable(false);
                            moreButton.setText("More");
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            moreButton.setClickable(true);
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
                            wikiText,
                            "maxLines",
                            30
                    );
                    animator.setDuration(500);
                    animator.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            moreButton.setClickable(false);
                            moreButton.setText("Less");
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            linkBox.setVisibility(View.VISIBLE);
                            moreButton.setClickable(true);
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
        artistImage.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView iv = new ImageView(getActivity());
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setLayoutParams(new ImageSwitcher.LayoutParams(RelativeLayout.LayoutParams.
                        FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT));
                return iv;
            }
        });
        Animation in = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
        Animation out = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);
        artistImage.setInAnimation(in);
        artistImage.setOutAnimation(out);

        artistTitle.setText(mArtist.getArtistName());
        mListener.setToolbarTitle(mArtist.getArtistName());

        ArtistArtLoader artLoader = new ArtistArtLoader(getActivity())
                .setArtist(mArtist)
                .setArtSize(ArtSize.LARGE)
                .setArtLoadedWatcher(new ArtLoadedWatcher() {
                    @Override
                    public void onArtLoaded(Drawable artwork) {
                        artistImage.setImageDrawable(artwork);
                    }

                    @Override
                    public void onLoadProgressChanged(LoadProgress progress) {
                        //ignore
                    }
                })
                .setPaletteGeneratedWatcher(new PaletteGeneratedWatcher() {
                    @Override
                    public void onPaletteGenerated(Palette palette) {
                        Palette.Swatch darkVibrant = palette.getDarkVibrantSwatch();
                        Palette.Swatch muted = palette.getMutedSwatch();
                        Palette.Swatch darkmuted = palette.getDarkMutedSwatch();
                        int color;

                        if (darkVibrant != null) {
                            color = darkVibrant.getRgb();
                        } else if (darkmuted != null) {
                            color = darkmuted.getRgb();
                        } else if (muted != null) {
                            color = muted.getRgb();
                        } else {
                            color = -1;
                        }

                        if (color == -1) {
                            mListener.setToolbarTransparent();
                        } else {
                            mListener.setToolbarColor(color);
                            final RelativeLayout wikiTextBox = (RelativeLayout) mView.findViewById(R.id.artist_detail_text_box);
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
                    }
                })
                .loadInBackground();

        WikipediaArticleFetcher articleFetcher = new WikipediaArticleFetcher(getActivity())
                .setArticle(mArtist.getArtistName())
                .setArticleLoadedWatcher(new ArticleLoadedWatcher() {
                    @Override
                    public void onArticleLoaded(WikipediaArticle article) {
                        wikiText.setText(article.getFirstParagraph());
                        String linkUrl = "<a href=\"" + article.getArticleUrl() + "\">Read more on Wikipedia</a>";
                        linkText.setText(Html.fromHtml(linkUrl));
                        linkText.setMovementMethod(LinkMovementMethod.getInstance());
                        moreButton.setEnabled(true);
                    }
                })
                .loadInBackground();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
