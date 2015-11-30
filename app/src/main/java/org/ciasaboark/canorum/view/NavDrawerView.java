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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.album.AlbumArtLoader;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedListener;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.fragment.TOP_LEVEL_FRAGMENTS;
import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.song.Track;

/**
 * Created by Jonathan Nelson on 1/24/15.
 */
public class NavDrawerView extends LinearLayout {
    private static final String TAG = "NavDrawer";
    private Context mContext;
    private AttributeSet mAttrs;
    private LinearLayout mLayout;
    private View mNavItemCur;
    private View mNavItemLibrary;
    private View mNavItemQueue;
    private View mNavItemHelp;
    private View mNavItemSettings;
    private View mNavItemPlaylists;
    private View mNavItemRecents;
    private ImageView mHeaderImageView;
    private NavDrawerListener mListener;
    private ImageView mHeaderIcon;
    private Animation mRotateForeverAnimation;
    private int mHighlightColor;
    private TOP_LEVEL_FRAGMENTS mCurItem = null;

    public NavDrawerView(Context ctx, AttributeSet attr) {
        super(ctx, attr);
        mContext = ctx;
        mAttrs = attr;
        init();
    }

    private void init() {
        mLayout = (LinearLayout) inflate(mContext, R.layout.view_nav_drawer, this);
        mHeaderImageView = (ImageView) mLayout.findViewById(R.id.nav_header_image);
        mNavItemCur = mLayout.findViewById(R.id.nav_item_cur);
        mNavItemLibrary = mLayout.findViewById(R.id.nav_item_library);
        mNavItemQueue = mLayout.findViewById(R.id.nav_item_queue);
        mNavItemHelp = mLayout.findViewById(R.id.nav_item_help);
        mNavItemSettings = mLayout.findViewById(R.id.nav_item_settings);
        mNavItemPlaylists = mLayout.findViewById(R.id.nav_item_playlists);
        mNavItemRecents = mLayout.findViewById(R.id.nav_item_recents);
        mHeaderIcon = (ImageView) mLayout.findViewById(R.id.nav_header_icon);
        mHighlightColor = getResources().getColor(R.color.nav_selected_background);


        attachOnClickListeners();
        initBroadcastReceivers();
        initHeader();
    }

    private void attachOnClickListeners() {
        attachOnClickListener(mNavItemCur, TOP_LEVEL_FRAGMENTS.CUR_PLAYING);
        attachOnClickListener(mNavItemLibrary, TOP_LEVEL_FRAGMENTS.LIBRARY);
        attachOnClickListener(mNavItemQueue, TOP_LEVEL_FRAGMENTS.QUEUE);
        attachOnClickListener(mNavItemHelp, TOP_LEVEL_FRAGMENTS.HELP);
        attachOnClickListener(mNavItemSettings, TOP_LEVEL_FRAGMENTS.SETTINGS);
        attachOnClickListener(mNavItemPlaylists, TOP_LEVEL_FRAGMENTS.PLAYLISTS);
        attachOnClickListener(mNavItemRecents, TOP_LEVEL_FRAGMENTS.RECENTS);
    }

    private void initBroadcastReceivers() {
        LocalBroadcastManager.getInstance(mContext).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Track curTrack = MusicControllerSingleton.getInstance(mContext).getCurTrack();
                if (curTrack == null) {
                    mHeaderImageView.setImageDrawable(null);
                } else {
                    Album album = curTrack.getSong().getAlbum();
                    AlbumArtLoader albumArtLoader = new AlbumArtLoader(mContext)
                            .setAlbum(album)
                            .setArtSize(ArtSize.SMALL)
                            .setProvideDefaultArtwork(true)
                            .setDefaultArtwork(null)
                            .setTag(album)
                            .setArtLoadedListener(new ArtLoadedListener() {
                                @Override
                                public void onArtLoaded(Drawable artwork, Object tag) {
                                    Track curTrack = MusicControllerSingleton.getInstance(mContext).getCurTrack();
                                    if (artwork != null && curTrack != null && curTrack.equals(tag)) {
                                        mHeaderImageView.setImageDrawable(artwork);
                                    }
                                }

                                @Override
                                public void onLoadProgressChanged(LoadProgress progress) {

                                }
                            })
                            .loadInBackground();
                }
            }
        }, new IntentFilter(MusicControllerSingleton.ACTION_PLAY));
    }

    private void initHeader() {
        mRotateForeverAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mRotateForeverAnimation.setDuration(60000);
        mRotateForeverAnimation.setInterpolator(new LinearInterpolator());
        mRotateForeverAnimation.setRepeatCount(Animation.INFINITE);
        mHeaderIcon.setAnimation(mRotateForeverAnimation);
        mRotateForeverAnimation.start();
    }

    private void attachOnClickListener(View v, final TOP_LEVEL_FRAGMENTS item) {
        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onItemSelected(item);
                }
            }
        });
    }

    public Drawable getHeaderDrawable() {
        return mHeaderImageView.getDrawable();
    }

    public void setHeaderDrawable(Drawable d) {
        mHeaderImageView.setImageDrawable(d);
    }

    public void setPalette(Palette palette) {
        int defaultHightlightColor = getResources().getColor(R.color.nav_selected_background);
        mHighlightColor = palette.getVibrantColor(
                palette.getLightMutedColor(
                        palette.getMutedColor(
                                defaultHightlightColor
                        )
                )
        );

        colorizeSelectedSection();
    }

    private void colorizeSelectedSection() {
        if (mCurItem != null) {
            View selectedSection = null;
            TextView selectedText = null;
            ImageView selectedIcon = null;

            switch (mCurItem) {
                case CUR_PLAYING:
                    selectedSection = mNavItemCur;
                    selectedText = (TextView) mLayout.findViewById(R.id.nav_item_cur_text);
                    selectedIcon = (ImageView) mLayout.findViewById(R.id.nav_item_cur_icon);
                    break;
                case LIBRARY:
                    selectedSection = mNavItemLibrary;
                    selectedText = (TextView) mLayout.findViewById(R.id.nav_item_library_text);
                    selectedIcon = (ImageView) mLayout.findViewById(R.id.nav_item_library_icon);
                    break;
                case QUEUE:
                    selectedSection = mNavItemQueue;
                    selectedText = (TextView) mLayout.findViewById(R.id.nav_item_queue_text);
                    selectedIcon = (ImageView) mLayout.findViewById(R.id.nav_item_queue_icon);
                    break;
                case HELP:
                    selectedSection = mNavItemHelp;
                    selectedText = (TextView) mLayout.findViewById(R.id.nav_item_help_text);
                    selectedIcon = (ImageView) mLayout.findViewById(R.id.nav_item_help_icon);
                    break;
                case SETTINGS:
                    selectedSection = mNavItemSettings;
                    selectedText = (TextView) mLayout.findViewById(R.id.nav_item_settings_text);
                    selectedIcon = (ImageView) mLayout.findViewById(R.id.nav_item_settings_icon);
                    break;
                case PLAYLISTS:
                    selectedSection = mNavItemPlaylists;
                    selectedText = (TextView) mLayout.findViewById(R.id.nav_item_playlists_text);
                    selectedIcon = (ImageView) mLayout.findViewById(R.id.nav_item_playlists_icon);
                    break;
                case RECENTS:
                    selectedSection = mNavItemRecents;
                    selectedText = (TextView) mLayout.findViewById(R.id.nav_item_recents_text);
                    selectedIcon = (ImageView) mLayout.findViewById(R.id.nav_item_recents_icon);
                    break;
                default:
                    Log.w(TAG, "unknown selected section " + mCurItem);
            }

            if (selectedSection != null) {
                selectedSection.setBackgroundColor(mHighlightColor);
//                selectedText.setTextColor(getResources().getColor(R.color.color_primary));
//                if (selectedIcon != null) {
//                    Drawable d = selectedIcon.getDrawable();
//                    d.mutate().setColorFilter(getResources().getColor(R.color.color_primary), PorterDuff.Mode.MULTIPLY);
//                    selectedIcon.setImageDrawable(d);
//                }
            }
        }
    }

    public void setSelectedSection(TOP_LEVEL_FRAGMENTS item) {
        mCurItem = item;
        unselecteAllSections();
        colorizeSelectedSection();
    }

    private void unselecteAllSections() {
        //TODO find a better way to do this
        Resources res = getResources();
        mNavItemCur.setBackground(null);
        ((TextView) mLayout.findViewById(R.id.nav_item_cur_text)).setTextColor(res.getColor(R.color.primary_text_default_material_dark));

        mNavItemLibrary.setBackground(null);
        ((TextView) mLayout.findViewById(R.id.nav_item_library_text)).setTextColor(res.getColor(R.color.primary_text_default_material_dark));

        mNavItemQueue.setBackground(null);
        ((TextView) mLayout.findViewById(R.id.nav_item_queue_text)).setTextColor(res.getColor(R.color.primary_text_default_material_dark));

        mNavItemHelp.setBackground(null);
        ((TextView) mLayout.findViewById(R.id.nav_item_help_text)).setTextColor(res.getColor(R.color.primary_text_default_material_dark));

        mNavItemSettings.setBackground(null);
        ((TextView) mLayout.findViewById(R.id.nav_item_settings_text)).setTextColor(res.getColor(R.color.primary_text_default_material_dark));

        mNavItemPlaylists.setBackground(null);
        ((TextView) mLayout.findViewById(R.id.nav_item_playlists_text)).setTextColor(res.getColor(R.color.primary_text_default_material_dark));

        mNavItemRecents.setBackground(null);
        ((TextView) mLayout.findViewById(R.id.nav_item_recents_text)).setTextColor(res.getColor(R.color.primary_text_default_material_dark));
    }

    public void setListener(NavDrawerListener listener) {
        mListener = listener;
    }

    public interface NavDrawerListener {
        public void onItemSelected(TOP_LEVEL_FRAGMENTS item);
    }


}
