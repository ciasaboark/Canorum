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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ciasaboark.canorum.R;

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
    private ImageView mHeaderImageView;
    private NavDrawerListener mListener;

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
        attachOnClickListeners();
        initBroadcastReceivers();
    }

    private void attachOnClickListeners() {
        attachOnClickListener(mNavItemCur, NAV_DRAWER_ITEM.CUR_PLAYING);
        attachOnClickListener(mNavItemLibrary, NAV_DRAWER_ITEM.LIBRARY);
        attachOnClickListener(mNavItemQueue, NAV_DRAWER_ITEM.QUEUE);
        attachOnClickListener(mNavItemHelp, NAV_DRAWER_ITEM.HELP);
        attachOnClickListener(mNavItemSettings, NAV_DRAWER_ITEM.SETTINGS);
    }

    private void initBroadcastReceivers() {
//        LocalBroadcastManager.getInstance(mContext).registerReceiver(new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                int colorPrimary = getResources().getColor(R.color.color_primary);
//                int newColor = intent.getIntExtra(AlbumArtLoader.BROADCAST_COLOR_CHANGED_PRIMARY, colorPrimary);
//                //toolbar disabled for now
//                Drawable d = mHeaderImageView.getBackground();
//                int oldColor = newColor;
//                if (d instanceof ColorDrawable) {
//                    oldColor = ((ColorDrawable) d).getColor();
//                }
//                final boolean useAlpha = !(newColor == colorPrimary);
//
//                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), oldColor, newColor);
//                colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//
//                    @Override
//                    public void onAnimationUpdate(ValueAnimator animator) {
//                        int color = (Integer) animator.getAnimatedValue();
////                        float[] hsv = new float[3];
////                        Color.colorToHSV(color, hsv);
////                        hsv[2] *= 0.8f; // value component
////                        int darkColor = Color.HSVToColor(hsv);
////                        int darkColorWithAlpha = Color.argb(150, Color.red(darkColor), Color.green(darkColor),
////                                Color.blue(darkColor));
//                        mHeaderImageView.setBackgroundColor(color);
//                    }
//
//                });
//                colorAnimation.start();
//
//            }
//        }, new IntentFilter(AlbumArtLoader.BROADCAST_COLOR_CHANGED));
    }

    private void attachOnClickListener(View v, final NAV_DRAWER_ITEM item) {
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

    public void setSelectedSection(NAV_DRAWER_ITEM item) {
        unselecteAllSections();
        colorizeSelectedSection(item);
    }

    private void unselecteAllSections() {
        //TODO find a better way to do this
        Resources res = getResources();
        mNavItemCur.setBackground(null);
        ((TextView) mLayout.findViewById(R.id.nav_item_cur_text)).setTextColor(res.getColor(R.color.primary_text_default_material_light));
        ((ImageView) mLayout.findViewById(R.id.nav_item_cur_icon)).setBackground(res.getDrawable(R.drawable.ic_play_grey600_24dp));

        mNavItemLibrary.setBackground(null);
        ((TextView) mLayout.findViewById(R.id.nav_item_library_text)).setTextColor(res.getColor(R.color.primary_text_default_material_light));
        ((ImageView) mLayout.findViewById(R.id.nav_item_library_icon)).setBackground(res.getDrawable(R.drawable.ic_library_music_grey600_24dp));

        mNavItemQueue.setBackground(null);
        ((TextView) mLayout.findViewById(R.id.nav_item_queue_text)).setTextColor(res.getColor(R.color.primary_text_default_material_light));
        ((ImageView) mLayout.findViewById(R.id.nav_item_queue_icon)).setBackground(res.getDrawable(R.drawable.ic_playlist_plus_grey600_24dp));

        mNavItemHelp.setBackground(null);
        ((TextView) mLayout.findViewById(R.id.nav_item_help_text)).setTextColor(res.getColor(R.color.primary_text_default_material_light));
        ((ImageView) mLayout.findViewById(R.id.nav_item_help_icon)).setBackground(res.getDrawable(R.drawable.ic_help_circle_grey600_24dp));

        mNavItemSettings.setBackground(null);
        ((TextView) mLayout.findViewById(R.id.nav_item_settings_text)).setTextColor(res.getColor(R.color.primary_text_default_material_light));
        ((ImageView) mLayout.findViewById(R.id.nav_item_settings_icon)).setBackground(res.getDrawable(R.drawable.ic_settings_grey600_24dp));
    }

    private void colorizeSelectedSection(NAV_DRAWER_ITEM item) {
        if (mAttrs != null) {
            View selectedSection = null;
            TextView selectedText = null;
            ImageView selectedIcon = null;

            switch (item) {
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
                default:
                    Log.w(TAG, "unknown selected section " + item);
            }

            if (selectedSection != null) {
                selectedSection.setBackgroundColor(getResources().getColor(R.color.nav_selected_background));
                selectedText.setTextColor(getResources().getColor(R.color.color_primary));
                if (selectedIcon != null) {
                    Drawable d = selectedIcon.getDrawable();
                    d.mutate().setColorFilter(getResources().getColor(R.color.color_primary), PorterDuff.Mode.MULTIPLY);
                    selectedIcon.setImageDrawable(d);
                }
            }
        }
    }

    public void setListener(NavDrawerListener listener) {
        mListener = listener;
    }

    public enum NAV_DRAWER_ITEM {
        LIBRARY,
        CUR_PLAYING,
        SETTINGS,
        QUEUE,
        HELP;
    }

    public interface NavDrawerListener {
        public void onItemSelected(NAV_DRAWER_ITEM item);
    }


}
