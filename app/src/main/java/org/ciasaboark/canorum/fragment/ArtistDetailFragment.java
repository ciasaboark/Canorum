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
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionInflater;
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
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.melnykov.fab.FloatingActionButton;
import com.nirhart.parallaxscroll.views.ParallaxScrollView;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.artist.ArtistArtLoader;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.playlist.provider.MergedProvider;
import org.ciasaboark.canorum.song.Artist;
import org.ciasaboark.canorum.song.Track;
import org.ciasaboark.canorum.song.extended.ExtendedAlbum;
import org.ciasaboark.canorum.song.shadow.ShadowAlbum;
import org.ciasaboark.canorum.song.shadow.ShadowLibraryAction;
import org.ciasaboark.canorum.song.shadow.ShadowLibraryFetcher;
import org.ciasaboark.canorum.song.shadow.ShadowLibraryLoadedListener;
import org.ciasaboark.canorum.view.AlbumCompactView;
import org.ciasaboark.canorum.view.ShadowAlbumCompactView;

import java.util.ArrayList;
import java.util.List;

public class ArtistDetailFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TAG = "ArtistDetailFragment";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_INIT_ART = "init_art";
    private static Drawable sInitialArt;
    List<ExtendedAlbum> mAlbumList = new ArrayList<ExtendedAlbum>();
    List<AlbumCompactView> mAlbumViews = new ArrayList<AlbumCompactView>();
    private View mView;
    private FloatingActionButton mFab;
    private boolean mHidden = true;
    // TODO: Rename and change types of parameters
    private Artist mArtist;
    private Drawable mInitalArt;
    private boolean mIsTextExpanded = false;
    private OnFragmentInteractionListener mListener;
    private RelativeLayout mLoadingToast;
    private ProgressBar mLoadingProgress;
    private TextView mLoadingText;
    private ShadowLibraryFetcher mShadowLibraryFetcher;
    private ImageView mLoadingCancel;

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
        return newInstance(artist, null);
    }

    public static ArtistDetailFragment newInstance(Artist artist, Drawable artistArt) {
        ArtistDetailFragment fragment = new ArtistDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ARTIST, artist);
        sInitialArt = artistArt;
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
                final ImageSwitcher artistImage = (ImageSwitcher) mView.findViewById(R.id.artistImage);
                Animation in = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
                Animation out = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);
                artistImage.setInAnimation(in);
                artistImage.setOutAnimation(out);
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_artist_detail, container, false);
        initArtistDetails();
        fillAlbumList();
        initToolbar();
        initFab();
        initLoader();
        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void initArtistDetails() {
        TextView artistTitle = (TextView) mView.findViewById(R.id.artist_detail_title);
        final ImageSwitcher artistImage = (ImageSwitcher) mView.findViewById(R.id.artistImage);
        final TextView wikiText = (TextView) mView.findViewById(R.id.artist_detail_wikipedia);
        final View textBox = mView.findViewById(R.id.artist_detail_text_box);
        final RelativeLayout linkBox = (RelativeLayout) mView.findViewById(R.id.artist_detail_wikipedia_more_box);
        final TextView linkText = (TextView) mView.findViewById(R.id.artist_detail_wikipedia_more_link);


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
        //set the initial artist artowrk before we apply any animations to the view switcher
        if (sInitialArt == null) {
            artistImage.setImageDrawable(getResources().getDrawable(R.drawable.default_album_art)); //TODO use default artist artwork
        } else {
            artistImage.setImageDrawable(sInitialArt);
            generatePaletteAsync((BitmapDrawable) sInitialArt);
            sInitialArt = null;
        }

        artistTitle.setText(mArtist.getArtistName());

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
            //temporarly disable article loader
//            ArticleFetcher articleFetcher = new ArticleFetcher(getActivity())
//                    .setArticleSource(mArtist)
//                    .setArticleLoadedWatcher(new DetailsLoadedWatcher() {
//                        @Override
//                        public void onArticleLoaded(Article article) {
//                            Activity ctx = getActivity();
//                            if (ctx != null) {
//                                if (article == null) {
//                                    wikiText.setText("Could not load artist information");   //TODO stringify
//                                } else {
//                                    wikiText.setText(article.getFirstParagraph());
//                                    String source;
//                                    Drawable sourceIcon;
//                                    switch (article.getSource()) {
//                                        case LASTFM:
//                                            source = "Last.fm";
//                                            sourceIcon = getResources().getDrawable(R.drawable.ic_lastfm_white_24dp);
//                                            break;
//                                        default:
//                                            source = "Wikipedia";
//                                            sourceIcon = getResources().getDrawable(R.drawable.ic_wikipedia_white_24dp);
//                                            break;
//                                    }
//                                    String linkUrl = "<a href=\"" + article.getArticleUrl() + "\">Read more on " + source + "</a>";
//                                    linkText.setText(Html.fromHtml(linkUrl));
//                                    linkText.setMovementMethod(LinkMovementMethod.getInstance());
//                                    linkText.setLinkTextColor(getResources().getColor(R.color.accent_material_dark));
//                                    ImageView linkIcon = (ImageView) mView.findViewById(R.id.artist_detail_wikipedia_more_icon);
//                                    linkIcon.setImageDrawable(sourceIcon);
//                                    textBox.setEnabled(true);
//                                }
//                            }
//                        }
//                    })
//                    .loadInBackground();

        }
    }

    private void fillAlbumList() {
        LinearLayout albumsContainer = (LinearLayout) mView.findViewById(R.id.artist_detail_albums_container);
        albumsContainer.removeAllViews();

        final MergedProvider provider = MergedProvider.getInstance(getActivity());
        mAlbumList = provider.getAlbumsForArtist(mArtist);
        for (final ExtendedAlbum album : mAlbumList) {
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
                    final List<Track> albumSongs = provider.getTracksForAlbum(album.getArtistName(), album);
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
        //TODO
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) mView.findViewById(R.id.local_toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.toolbar_title_text));
        toolbar.setTitle(mArtist.getArtistName());
        toolbar.setNavigationIcon(R.drawable.ic_keyboard_backspace_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        toolbar.inflateMenu(R.menu.menu_artist_detail);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                boolean itemHandled = false;
                switch (menuItem.getItemId()) {
                    case R.id.menu_artist_detail_shadow:
                        initShadowLibrary();
                        itemHandled = true;
                }
                return itemHandled;
            }
        });
    }

    private void initFab() {
        ParallaxScrollView scrollView = (ParallaxScrollView) mView.findViewById(R.id.scrollview);
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

        //the floating action button is hidden on view creation and shown during onStart()
        mFab.setVisibility(View.INVISIBLE);
    }

    private void initLoader() {
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

        ImageSwitcher imageSwitcher = (ImageSwitcher) mView.findViewById(R.id.artistImage);
        int curChild = imageSwitcher.getDisplayedChild();
        ImageView imageView = (ImageView) imageSwitcher.getChildAt(curChild);
        Bitmap bitmap = drawable.getBitmap();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();


        int viewWidth = imageView.getWidth();
        int viewHeight = imageView.getHeight();

        //if the view height or width == 0 then the view has not be created yet, just apply the
        //drawable as given
        if (viewWidth == 0 || viewHeight == 0) {
            imageSwitcher.setImageDrawable(drawable);
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
            imageSwitcher.setImageDrawable(new BitmapDrawable(croppedBitmap));
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

            int vibrantColor = palette.getVibrantColor(-1);
            if (vibrantColor != -1) {
                int oldColor = mFab.getColorNormal();
                if (vibrantColor != oldColor) {
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
        }
    }

    private void initShadowLibrary() {
        mShadowLibraryFetcher = new ShadowLibraryFetcher(getActivity())
                .setArtist(mArtist)
                .setShadowLibraryListener(new ShadowLibraryLoadedListener() {
                    @Override
                    public void onShadowLibraryLoaded(List<ShadowAlbum> shadowLibrary) {
//                        Log.d(TAG, "breakpoint here");
//                        Log.d(TAG, "shadow library:\n" + shadowLibrary.toString());
//                        addShadowAlbumList(shadowLibrary);
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
                    public void onShadowAlbumLoaded(ShadowAlbum album) {
                        addShadowAlbum(album);
                    }
                })
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

    private void addShadowAlbum(ShadowAlbum shadowAlbum) {
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

    private void addShadowAlbumList(List<ShadowAlbum> shadowAlbums) {
        for (ShadowAlbum shadowAlbum : shadowAlbums) {
            addShadowAlbum(shadowAlbum);
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
