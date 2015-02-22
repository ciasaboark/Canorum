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
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.melnykov.fab.FloatingActionButton;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.albumart.AlbumArtLoader;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.info.Article;
import org.ciasaboark.canorum.info.ArticleFetcher;
import org.ciasaboark.canorum.info.ArticleLoadedWatcher;
import org.ciasaboark.canorum.playlist.provider.SystemLibrary;
import org.ciasaboark.canorum.song.Track;
import org.ciasaboark.canorum.song.extended.ExtendedAlbum;
import org.ciasaboark.canorum.view.SongView;

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
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String KEY_ALBUM = "param1";
    private static Drawable sInitialArt;
    FloatingActionButton mFab;
    private View mView;
    // TODO: Rename and change types of parameters
    private ExtendedAlbum mAlbum;
    private boolean mIsTextExpanded = false;
    private boolean mHidden = true;

    private OnFragmentInteractionListener mListener;

    public AlbumDetailFragment() {
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
    public static AlbumDetailFragment newInstance(ExtendedAlbum album) {
        return newInstance(album, null);
    }

    public static AlbumDetailFragment newInstance(ExtendedAlbum album, Drawable albumArt) {
        AlbumDetailFragment fragment = new AlbumDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ALBUM, album);
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
                showFloatingActionButton();

                //the main album details view should only do a transition animation if the new
                //image loads after the fragment is completely loaded
                final ImageSwitcher albumImage = (ImageSwitcher) mView.findViewById(R.id.albumImage);
                Animation in = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
                Animation out = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);
                albumImage.setInAnimation(in);
                albumImage.setOutAnimation(out);
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
            mAlbum = (ExtendedAlbum) getArguments().getSerializable(KEY_ALBUM);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_album_detail, container, false);
        initAlbumDetails();
        fillAlbumList();
        adjustToolbar();
        initFab();
        return mView;
    }

    private void initAlbumDetails() {
        TextView albumTitle = (TextView) mView.findViewById(R.id.album_detail_title);
        final ImageSwitcher albumImage = (ImageSwitcher) mView.findViewById(R.id.albumImage);
        final View textBox = mView.findViewById(R.id.album_detail_text_box);
        final TextView wikiText = (TextView) mView.findViewById(R.id.album_detail_wikipedia);
        final RelativeLayout linkBox = (RelativeLayout) mView.findViewById(R.id.album_detail_wikipedia_more_box);
        final TextView linkText = (TextView) mView.findViewById(R.id.album_detail_wikipedia_more_link);


        textBox.setEnabled(false);
        textBox.setOnClickListener(new View.OnClickListener() {
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
                            textBox.setClickable(false);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            textBox.setClickable(true);
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
                            textBox.setClickable(false);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            linkBox.setVisibility(View.VISIBLE);
                            textBox.setClickable(true);
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
        albumImage.setFactory(new ViewSwitcher.ViewFactory() {
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
            albumImage.setImageDrawable(getResources().getDrawable(R.drawable.default_album_art)); //TODO use default artist artwork
        } else {
            albumImage.setImageDrawable(sInitialArt);
            generatePaletteAsync((BitmapDrawable) sInitialArt);
            sInitialArt = null;
        }

        albumTitle.setText(mAlbum.getAlbumName());
        setToolbarTitle(mAlbum.getAlbumName());

        AlbumArtLoader artLoader = new AlbumArtLoader(getActivity())
                .setAlbum(mAlbum)
                .setArtSize(ArtSize.LARGE)
                .setArtLoadedWatcher(new ArtLoadedWatcher() {
                    @Override
                    public void onArtLoaded(final Drawable artwork, String tag) {

                        if (albumImage != null) {
                            if (artwork == null) {
                                //apply the default image if one has not already been applied
                                int curChild = albumImage.getDisplayedChild();
                                ImageView curView = (ImageView) albumImage.getChildAt(curChild);
                                if (curView.getDrawable() == null) {
                                    albumImage.setImageDrawable(getResources().getDrawable(R.drawable.default_album_art));
                                }
                            } else {
                                albumImage.setImageDrawable(artwork);
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

        if (mAlbum.getArtistName().equals("<unknown>")) {
            wikiText.setText("The tracks listed below do not have any proper artist information attached.");
        } else {
            ArticleFetcher articleFetcher = new ArticleFetcher(getActivity())
                    .setArticleSource(mAlbum)
                    .setArticleLoadedWatcher(new ArticleLoadedWatcher() {
                        @Override
                        public void onArticleLoaded(Article article) {
                            Context ctx = getActivity();
                            if (ctx != null) {
                                if (article == null) {
                                    wikiText.setText("Could not load album information");   //TODO stringify
                                } else {
                                    wikiText.setText(article.getFirstParagraph());
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
                                    linkText.setText(Html.fromHtml(linkUrl));
                                    linkText.setMovementMethod(LinkMovementMethod.getInstance());
                                    linkText.setLinkTextColor(getResources().getColor(R.color.accent_material_dark));
                                    ImageView linkIcon = (ImageView) mView.findViewById(R.id.album_detail_wikipedia_more_icon);
                                    linkIcon.setImageDrawable(sourceIcon);
                                    textBox.setEnabled(true);
                                }
                            }
                        }
                    })
                    .loadInBackground();
        }
    }

    private void fillAlbumList() {
        LinearLayout songsContainer = (LinearLayout) mView.findViewById(R.id.album_detail_songs_container);
        songsContainer.removeAllViews();

        SystemLibrary systemLibrary = new SystemLibrary(getActivity());

        List<Track> tracks = systemLibrary.getTracksForAlbum(mAlbum.getArtistName(), mAlbum);
        for (Track track : tracks) {
            SongView songView = new SongView(getActivity(), null, track);
            songsContainer.addView(songView);
        }
        //TODO
    }

    private void adjustToolbar() {
        Toolbar toolbar = (Toolbar) mView.findViewById(R.id.local_toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.toolbar_title_text));
        toolbar.setTitle(mAlbum.getAlbumName());
        toolbar.setNavigationIcon(R.drawable.ic_keyboard_backspace_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
    }

    private void initFab() {
        mFab = (FloatingActionButton) mView.findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SystemLibrary systemLibrary = new SystemLibrary(getActivity());
                List<Track> tracks = systemLibrary.getTracksForAlbum(mAlbum.getArtistName(), mAlbum);
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

    private void setToolbarTitle(String albumName) {
        //TODO use local toolbar
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
                }
            });
            colorAnimator.start();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
