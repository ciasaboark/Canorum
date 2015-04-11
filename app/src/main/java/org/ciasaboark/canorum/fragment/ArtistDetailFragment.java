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
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.artist.ArtistArtLoader;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.details.DetailsFetcher;
import org.ciasaboark.canorum.details.DetailsLoadedWatcher;
import org.ciasaboark.canorum.details.article.Article;
import org.ciasaboark.canorum.details.types.ArtistDetails;
import org.ciasaboark.canorum.details.types.Details;
import org.ciasaboark.canorum.playlist.provider.MergedProvider;
import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.song.Artist;
import org.ciasaboark.canorum.song.Track;
import org.ciasaboark.canorum.song.shadow.ShadowAlbum;
import org.ciasaboark.canorum.song.shadow.ShadowLibraryAction;
import org.ciasaboark.canorum.song.shadow.ShadowLibraryFetcher;
import org.ciasaboark.canorum.song.shadow.ShadowLibraryLoadedListener;
import org.ciasaboark.canorum.view.AlbumCompactView;
import org.ciasaboark.canorum.view.HidingToolbar;
import org.ciasaboark.canorum.view.ShadowAlbumCompactView;
import org.ciasaboark.canorum.view.SimilarArtistPortrait;

import java.util.ArrayList;
import java.util.List;

public class ArtistDetailFragment extends Fragment {
    private static final String TAG = "ArtistDetailFragment";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_INIT_ART = "init_art";
    private static Drawable sInitialArt;
    List<Album> mAlbumList = new ArrayList<Album>();
    List<AlbumCompactView> mAlbumViews = new ArrayList<AlbumCompactView>();
    private View mView;
    private FloatingActionButton mFab;
    private boolean mHidden = true;
    private Artist mArtist;
    private Drawable mInitalArt;
    private boolean mIsTextExpanded = false;
    private OnFragmentInteractionListener mListener;
    private RelativeLayout mLoadingToast;
    private ProgressBar mLoadingProgress;
    private TextView mLoadingText;
    private ShadowLibraryFetcher mShadowLibraryFetcher;
    private ImageView mLoadingCancel;
    private int mDynamicAccentColor;
    private Button mLoadMoreButton;
    private List<String> mShadowAlbumTitles = new ArrayList<String>();
    private ShadowLibraryLoadedListener mShadowLibraryListener = new ShadowLibraryLoadedListener() {
        @Override
        public void onShadowLibraryLoaded(List<ShadowAlbum> shadowLibrary) {
        }

        @Override
        public void onShadowAlbumLoaded(ShadowAlbum album) {
            addShadowAlbum(album);
        }

        @Override
        public void onShadowLibraryUpdate(ShadowLibraryAction action, String message) {
            switch (action) {
                case LOAD_FINISH:
                    mLoadingProgress.setVisibility(View.INVISIBLE);
                    mLoadingToast.animate()
                            .translationY(0)
                            .alpha(0.0f)
                            .setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {
                                    //nothing to do here
                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mLoadingToast.setVisibility(View.GONE);

                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {
                                    //nothing to do here
                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {
                                    //nothing to do here
                                }
                            })
                            .start();

                    mLoadingText.setText(message);
                    break;
                case LOAD_START:
                    mLoadingToast.setVisibility(View.VISIBLE);
                    mLoadingToast.setAlpha(0.0f);
                    mLoadingToast.animate()
                            .translationY(mLoadingToast.getHeight())
                            .alpha(1.0f)
                            .start();

                    mLoadingProgress.setVisibility(View.VISIBLE);
                    mLoadingText.setText(message);
                    break;
                default:
                    mLoadingText.setText(message);
            }
        }

        @Override
        public void onAlbumTitlesLoaded(List<String> albumTitles) {

        }
    };
    private HidingToolbar mToolbar;
    private ScrollView mScrollView;

    public ArtistDetailFragment() {
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
                if (!mAlbumList.isEmpty()) {
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
            mArtist = (Artist) getArguments().getSerializable(KEY_ARTIST);
        }
        mDynamicAccentColor = getResources().getColor(R.color.color_accent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_artist_detail, container, false);
        initArtistDetails();
        initToolbar();
        fillAlbumList();
        initFab();
        buildShadowAlbumsTitlesList();
        initLoader();
        return mView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mArtist = (Artist) savedInstanceState.getSerializable(KEY_ARTIST);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mHidden) {
            mFab.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_ARTIST, mArtist);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        if (mShadowLibraryFetcher != null) {
            mShadowLibraryFetcher.cancel();
        }
        if (mToolbar != null) {
            mToolbar.detatchScrollView();
        }
    }

    private void initArtistDetails() {
        mScrollView = (ScrollView) mView.findViewById(R.id.scrollview);
        final ImageView artistImage = (ImageView) mView.findViewById(R.id.artistImage);
        final TextView wikiText = (TextView) mView.findViewById(R.id.artist_detail_wikipedia);
        final View textBox = mView.findViewById(R.id.artist_detail_text_box);
        final RelativeLayout linkBox = (RelativeLayout) mView.findViewById(R.id.artist_detail_wikipedia_more_box);
        final TextView linkText = (TextView) mView.findViewById(R.id.artist_detail_wikipedia_more_link);
        final LinearLayout similarArtistsHolder = (LinearLayout) mView.findViewById(R.id.similar_artists_holder);
        final TextView similarArtistsTitle = (TextView) mView.findViewById(R.id.similar_artists_title);
        similarArtistsTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int curVisibility = similarArtistsHolder.getVisibility();
                int newVisibility;
                switch (curVisibility) {
                    case View.VISIBLE:
                        newVisibility = View.GONE;
                        break;
                    default:
                        newVisibility = View.VISIBLE;
                }
                similarArtistsHolder.setVisibility(newVisibility);
            }
        });
        similarArtistsTitle.setEnabled(false);


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

        //set the initial artist artowrk before we apply any animations to the view switcher
        if (sInitialArt == null) {
            artistImage.setImageDrawable(getResources().getDrawable(R.drawable.default_album_art)); //TODO use default artist artwork
        } else {
            artistImage.setImageDrawable(sInitialArt);
            generatePaletteAsync((BitmapDrawable) sInitialArt);
            sInitialArt = null;
        }

        ArtistArtLoader artLoader = new ArtistArtLoader(getActivity())
                .setArtist(mArtist)
                .setArtSize(ArtSize.LARGE)
                .setInternetSearchEnabled(true)
                .setArtLoadedWatcher(new ArtLoadedWatcher() {
                    @Override
                    public void onArtLoaded(final Drawable artwork, Object tag) {
                        if (artistImage != null) {
                            setImageDrawable((BitmapDrawable) artwork);
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
                        //its possible that the palette was not generated until the user left this fragment
                        Context ctx = getActivity();
                        if (ctx != null) {
                            applyPalette(palette);
                        }
                    }
                })
                .loadInBackground();

        if (mArtist.getArtistName().equals("<unknown>")) {
            wikiText.setText("The tracks listed below do not have any proper artist information attached.");
        } else {
            DetailsFetcher detailsFetcher = new DetailsFetcher(getActivity())
                    .setArticleSource(mArtist)
                    .setArticleLoadedWatcher(new DetailsLoadedWatcher() {
                        @Override
                        public void onDetailsLoaded(Details details) {
                            Activity ctx = getActivity();
                            if (ctx != null) {
                                if (details == null) {
                                    wikiText.setText("Could not load artist information");   //TODO stringify
                                } else {
                                    Article article = details.getArticle();
                                    String firstParagraph = article == null ? "" : article.getFirstParagraph();
                                    wikiText.setText(firstParagraph == null ? "" : firstParagraph);
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
                                    ImageView linkIcon = (ImageView) mView.findViewById(R.id.artist_detail_wikipedia_more_icon);
                                    linkIcon.setImageDrawable(sourceIcon);
                                    textBox.setEnabled(true);

                                    //show the similar artists
                                    similarArtistsHolder.removeAllViews();
                                    if (!((ArtistDetails) details).getSimilarArtists().isEmpty()) {
                                        similarArtistsTitle.setVisibility(View.VISIBLE);
                                        similarArtistsTitle.setEnabled(true);
                                    }
                                    for (final Artist artist : ((ArtistDetails) details).getSimilarArtists()) {
                                        SimilarArtistPortrait similarArtistPortrait = new SimilarArtistPortrait(getActivity(), null, artist);
                                        similarArtistsHolder.addView(similarArtistPortrait);
                                        similarArtistPortrait.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                ArtistDetailFragment artistDetailFragment = ArtistDetailFragment.newInstance(artist);
                                                getActivity().getSupportFragmentManager().beginTransaction()
                                                        .addToBackStack(null)
                                                        .replace(R.id.library_inner_fragment, artistDetailFragment)
                                                        .commit();
                                            }
                                        });
                                    }
                                }
                            }

                        }
                    })
                    .loadInBackground();

        }
    }

    private void initToolbar() {
        mToolbar = (HidingToolbar) mView.findViewById(R.id.local_toolbar);
        mToolbar.setTitleTextColor(getResources().getColor(R.color.toolbar_title_text));
        mToolbar.setTitle(mArtist.getArtistName());
        mToolbar.setNavigationIcon(R.drawable.ic_keyboard_backspace_white_24dp);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        mToolbar.inflateMenu(R.menu.menu_artist_detail);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                boolean itemHandled = false;
                switch (menuItem.getItemId()) {
                    case R.id.menu_artist_detail_shadow:
                        //its possible that the user added a few shadow albums to the view, or filled in
                        //some incomplete local albums, so we will rebuild the album list (which
                        //will clear all views)
                        fillAlbumList();
                        initShadowLibrary();
                        itemHandled = true;
                }
                return itemHandled;
            }
        });
        mToolbar.attachScrollView(mScrollView)
                .setFadeInBackground(new ColorDrawable(getResources().getColor(R.color.color_primary)));
    }

    private void fillAlbumList() {
        LinearLayout albumsContainer = (LinearLayout) mView.findViewById(R.id.artist_detail_albums_container);
        final MergedProvider provider = MergedProvider.getInstance(getActivity());
        mAlbumList = provider.getAlbumsForArtist(mArtist);

        if (!mAlbumList.isEmpty()) {
            albumsContainer.removeAllViews();
        }

        for (final Album album : mAlbumList) {
            final AlbumCompactView albumCompactView = new AlbumCompactView(getActivity(), null, album);
            mAlbumViews.add(albumCompactView);
            albumCompactView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    ImageSwitcher albumImage = albumCompactView.getAlbumArt();


                    int childNum = albumImage.getDisplayedChild();
                    Drawable albumArtwork = ((ImageView) albumImage.getChildAt(childNum)).getDrawable();
                    AlbumDetailFragment albumDetailFragment = AlbumDetailFragment.newInstance(album, albumArtwork);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setSharedElementEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.move));
                        setSharedElementReturnTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.move));
                        setExitTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.slide_left));
                        albumImage.setTransitionName("albumImage");
                        albumDetailFragment.setSharedElementEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.move));
                        albumDetailFragment.setSharedElementReturnTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.move));
                        albumDetailFragment.setEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.fade));
                        albumDetailFragment.setEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.fade));
                    }

                    getActivity().getSupportFragmentManager().beginTransaction()
                            .addToBackStack(null)
                            .addSharedElement(albumImage, "albumImage")
                            .replace(R.id.library_inner_fragment, albumDetailFragment)
                            .commit();

                }
            });
            albumCompactView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    final List<Track> albumSongs = provider.getTracksForAlbum(album.getArtist().getArtistName(), album);
                    final MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(getActivity());
                    PopupMenu popupMenu = new PopupMenu(getActivity(), albumCompactView);
                    popupMenu.inflate(R.menu.library_long_click);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            boolean itemHandled = false;
                            switch (item.getItemId()) {
                                case R.id.popup_menu_library_add_queue:
                                    Toast.makeText(getActivity(), "Added " + album + " to queue", Toast.LENGTH_SHORT).show();
                                    musicControllerSingleton.addTracksToQueue(albumSongs);
                                    itemHandled = true;
                                    break;
                                case R.id.popup_menu_library_play_next:
                                    Toast.makeText(getActivity(), "Playing " + album + " next", Toast.LENGTH_SHORT).show();
                                    musicControllerSingleton.addTracksToQueueHead(albumSongs);
                                    itemHandled = true;
                                    break;
                                case R.id.popup_menu_library_play_now:
                                    Toast.makeText(getActivity(), "Playing " + album, Toast.LENGTH_SHORT).show();
                                    musicControllerSingleton.addTracksToQueueHead(albumSongs);
                                    musicControllerSingleton.playNext();
                                    itemHandled = true;
                                    break;
                            }
                            return itemHandled;
                        }
                    });
                    popupMenu.show();
                    return true;
                }
            });
            albumsContainer.addView(albumCompactView);
        }
    }

    private void initFab() {
        mFab = (FloatingActionButton) mView.findViewById(R.id.fab);

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MergedProvider provider = MergedProvider.getInstance(getActivity());
                List<Track> tracks = provider.getTracksForArtist(mArtist);
                MusicControllerSingleton controller = MusicControllerSingleton.getInstance(getActivity());
                controller.addTracksToQueue(tracks);
                controller.playNext();
            }
        });

        mFab.setVisibility(View.INVISIBLE);
    }

    private void buildShadowAlbumsTitlesList() {
        ShadowLibraryFetcher albumTitlesFetcher = new ShadowLibraryFetcher(getActivity())
                .setLoadTitlesOnly(true)
                .setArtist(mArtist)
                .setShadowLibraryListener(new ShadowLibraryLoadedListener() {
                    @Override
                    public void onShadowLibraryLoaded(List<ShadowAlbum> shadowLibrary) {

                    }

                    @Override
                    public void onShadowAlbumLoaded(ShadowAlbum shadowAlbum) {

                    }

                    @Override
                    public void onShadowLibraryUpdate(ShadowLibraryAction action, String message) {

                    }

                    @Override
                    public void onAlbumTitlesLoaded(final List<String> albumTitles) {
                        //the 'load more' link should only load albums that we don't have locally,
                        //so first we have to filter those out
                        for (Album knownAlbum : mAlbumList) {
                            String knownAlbumTitle = knownAlbum.getAlbumName();
                            if (albumTitles.contains(knownAlbumTitle)) {
                                albumTitles.remove(knownAlbumTitle);
                            }
                        }

                        mShadowAlbumTitles = albumTitles;

                        if (mShadowAlbumTitles != null && !mShadowAlbumTitles.isEmpty())
                            mLoadMoreButton.setEnabled(true);
                    }
                })
                .loadInBackground();
    }

    private void initLoader() {
        mLoadMoreButton = (Button) mView.findViewById(R.id.load_more_button);
        mLoadMoreButton.setEnabled(false);
        mLoadMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mShadowAlbumTitles.isEmpty()) {
                    mLoadMoreButton.setEnabled(false);
                } else {
                    //pull in at most 3 more titles to load
                    List<String> titlesToLoad = new ArrayList<String>();
                    for (int i = 0; i < 3; i++) {
                        try {
                            String albumTitle = mShadowAlbumTitles.remove(i);
                            titlesToLoad.add(albumTitle);
                        } catch (IndexOutOfBoundsException e) {
                            //this is fine, we didn't check the size of the available titles list beforehand
                            break;
                        }
                    }

                    if (!titlesToLoad.isEmpty()) {
                        ShadowLibraryFetcher albumsFetcher = new ShadowLibraryFetcher(getActivity())
                                .setArtist(mArtist)
                                .setShadowLibraryListener(mShadowLibraryListener)
                                .loadAlbumsInBackground(titlesToLoad.toArray(new String[]{}));
                    }
                }
            }
        });
        mLoadingToast = (RelativeLayout) mView.findViewById(R.id.artist_detail_loading_toast);
        mLoadingProgress = (ProgressBar) mView.findViewById(R.id.artist_detail_loading_toast_progress);
        mLoadingText = (TextView) mView.findViewById(R.id.artist_detail_loading_toast_text);
        mLoadingCancel = (ImageView) mView.findViewById(R.id.artist_detail_loading_toast_cancel);
        mLoadingCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mShadowLibraryFetcher != null) {
                    mShadowLibraryFetcher.cancel();
                }
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

    private void setImageDrawable(BitmapDrawable drawable) {
        if (drawable == null) {
            return;
        }

        ImageView imageView = (ImageView) mView.findViewById(R.id.artistImage);
        Bitmap bitmap = drawable.getBitmap();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int viewWidth = imageView.getWidth();
        int viewHeight = imageView.getHeight();

        //if the view height or width == 0 then the view has not be created yet, just apply the
        //drawable as given
        if (viewWidth == 0 || viewHeight == 0) {
            imageView.setImageDrawable(drawable);
        } else {
            //otherwise scale the image twice, once to fit the view width exactly, then scale to
            //at least the view height
            Bitmap scaledBitmap = scaleBitmapToWidth(bitmap, viewWidth);
            scaledBitmap = scaleBitmapToMinHeight(scaledBitmap, viewHeight);

            int newWidth = scaledBitmap.getWidth();
            int newHeight = scaledBitmap.getHeight();
            int xPos = 0;
            if (newWidth > viewWidth) {
                int diff = newWidth - viewWidth;
                xPos = diff / 2;
            }
            Bitmap croppedBitmap = Bitmap.createBitmap(scaledBitmap, xPos, 0, viewWidth, viewHeight);
            imageView.setImageDrawable(new BitmapDrawable(croppedBitmap));
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
            //no colors found in the palette
        } else {
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

            mDynamicAccentColor = palette.getVibrantColor(
                    mDynamicAccentColor
            );

            int oldColor = mFab.getColorNormal();

            if (mDynamicAccentColor != oldColor) {
                final ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), oldColor, mDynamicAccentColor);
                colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int color = (Integer) colorAnimator.getAnimatedValue();
                        mFab.setColorNormal(color);
                        mFab.setColorPressed(color);
                        mToolbar.setFadeInBackground(new ColorDrawable(color));
                        mLoadMoreButton.setTextColor(color);
                    }
                });
                colorAnimator.start();
            }
        }
    }

    public static ArtistDetailFragment newInstance(Artist artist) {
        return newInstance(artist, null);
    }

    private void initShadowLibrary() {
        mShadowLibraryFetcher = new ShadowLibraryFetcher(getActivity())
                .setArtist(mArtist)
                .setShadowLibraryListener(mShadowLibraryListener)
                .loadInBackground();
    }

    private Bitmap scaleBitmapToWidth(Bitmap bitmap, int width) {
        Bitmap scaledBitmap;

        int bitmapHeight = bitmap.getHeight();
        int bitmapWidth = bitmap.getWidth();

        float scale = 0f;
        if (bitmapWidth > width) {
            scale = (float) bitmapWidth / (float) width;
        } else {
            scale = (float) width / (float) bitmapWidth;
        }

        int newHeight = Math.round(bitmapHeight * scale);
        int newWidth = Math.round(bitmapWidth * scale);
        scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

        return scaledBitmap;
    }

    private Bitmap scaleBitmapToMinHeight(Bitmap bitmap, int height) {
        Bitmap scaledBitmap;
        int bitmapHeight = bitmap.getHeight();
        if (bitmapHeight >= height) {
            scaledBitmap = bitmap;
        } else {
            int bitmapWidth = bitmap.getWidth();
            float scale = (float) height / (float) bitmapHeight;
            int newHeight = Math.round(bitmapHeight * scale);
            int newWidth = Math.round(bitmapWidth * scale);
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        }
        return scaledBitmap;
    }

    public static ArtistDetailFragment newInstance(Artist artist, Drawable artistArt) {
        ArtistDetailFragment fragment = new ArtistDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ARTIST, artist);
        sInitialArt = artistArt;
        fragment.setArguments(args);
        return fragment;
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

    private void addShadowAlbumList(List<ShadowAlbum> shadowAlbums) {
        for (ShadowAlbum shadowAlbum : shadowAlbums) {
            addShadowAlbum(shadowAlbum);
        }
    }

    private void addShadowAlbum(final ShadowAlbum shadowAlbum) {
        boolean wasMergedWithExistingAlbum = false;
        //if we already have a view with the same album, then use that one
        for (AlbumCompactView albumView : mAlbumViews) {
            if (albumView.getAlbum().equals(shadowAlbum)) {
                albumView.addShadowSongs(shadowAlbum.getSongs());
                wasMergedWithExistingAlbum = true;
            }
        }

        //otherwise create a new view for the shadow album
        if (!wasMergedWithExistingAlbum) {
            final ShadowAlbumCompactView shadowAlbumCompactView = new ShadowAlbumCompactView(getActivity(), null, shadowAlbum);
            LinearLayout albumsContainer = (LinearLayout) mView.findViewById(R.id.artist_detail_albums_container);
            albumsContainer.addView(shadowAlbumCompactView);
            shadowAlbumCompactView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ImageSwitcher albumImage = shadowAlbumCompactView.getAlbumArt();

                    int childNum = albumImage.getDisplayedChild();
                    Drawable albumArtwork = ((ImageView) albumImage.getChildAt(childNum)).getDrawable();
                    AlbumDetailFragment albumDetailFragment = AlbumDetailFragment.newInstance(
                            shadowAlbum, albumArtwork);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setSharedElementEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.move));
                        setSharedElementReturnTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.move));
                        setExitTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.slide_left));
                        albumImage.setTransitionName("albumImage");
                        albumDetailFragment.setSharedElementEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.move));
                        albumDetailFragment.setSharedElementReturnTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.move));
                        albumDetailFragment.setEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.fade));
                        albumDetailFragment.setEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.fade));
                    }

                    getActivity().getSupportFragmentManager().beginTransaction()
                            .addToBackStack(null)
                            .addSharedElement(albumImage, "albumImage")
                            .replace(R.id.library_inner_fragment, albumDetailFragment)
                            .commit();
                }
            });
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
