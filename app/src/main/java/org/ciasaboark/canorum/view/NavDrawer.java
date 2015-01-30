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
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.activity.MainActivity;
import org.ciasaboark.canorum.activity.SettingsActivity;

/**
 * Created by Jonathan Nelson on 1/24/15.
 */
public class NavDrawer extends LinearLayout {
    private static final String TAG = "NavDrawer";
    private Context mContext;
    private AttributeSet mAttrs;
    private LinearLayout mLayout;
    private View mNavItemCur;
    private View mNavItemLibrary;
    private View mNavItemQueue;
    private View mNavItemHelp;
    private View mNavItemSettings;

    public NavDrawer(Context ctx, AttributeSet attr) {
        super(ctx, attr);
        mContext = ctx;
        mAttrs = attr;
        init();
    }

    private void init() {
        mLayout = (LinearLayout) inflate(mContext, R.layout.navigation_drawer, this);
        mNavItemCur = mLayout.findViewById(R.id.nav_item_cur);
        mNavItemLibrary = mLayout.findViewById(R.id.nav_item_library);
        mNavItemQueue = mLayout.findViewById(R.id.nav_item_queue);
        mNavItemHelp = mLayout.findViewById(R.id.nav_item_help);
        mNavItemSettings = mLayout.findViewById(R.id.nav_item_settings);
        attachOnClickListeners();
        colorizeSelectedSection();
    }

    private void attachOnClickListeners() {
        attachOnClickListener(mNavItemCur, MainActivity.class);
//        attachOnClickListener(mNavItemLibrary, LibraryActivity.class);
//        attachOnClickListener(mNavItemQueue, QueueActivity.class);
//        attachOnClickListener(mNavItemHelp, HelpActivity.class);
        attachOnClickListener(mNavItemSettings, SettingsActivity.class);
    }

    private void colorizeSelectedSection() {
        if (mAttrs != null) {
            TypedArray a = mContext.obtainStyledAttributes(mAttrs, R.styleable.NavDrawer);
            String section = a.getString(R.styleable.NavDrawer_section);
            View selectedSection = null;
            TextView selectedText = null;
            ImageView selectedIcon = null;

            switch (section) {
                case "cur":
                    selectedSection = mNavItemCur;
                    selectedText = (TextView) mLayout.findViewById(R.id.nav_item_cur_text);
                    selectedIcon = (ImageView) mLayout.findViewById(R.id.nav_item_cur_icon);
                    break;
                case "library":
                    selectedSection = mNavItemLibrary;
                    selectedText = (TextView) mLayout.findViewById(R.id.nav_item_settings_text);
                    selectedIcon = (ImageView) mLayout.findViewById(R.id.nav_item_settings_icon);
                    break;
                case "queue":
                    selectedSection = mNavItemQueue;
                    selectedText = (TextView) mLayout.findViewById(R.id.nav_item_queue_text);
                    selectedIcon = (ImageView) mLayout.findViewById(R.id.nav_item_queue_icon);
                    break;
                case "help":
                    selectedSection = mNavItemHelp;
                    selectedText = (TextView) mLayout.findViewById(R.id.nav_item_help_text);
                    selectedIcon = (ImageView) mLayout.findViewById(R.id.nav_item_help_icon);
                    break;
                case "settings":
                    selectedSection = mNavItemSettings;
                    selectedText = (TextView) mLayout.findViewById(R.id.nav_item_settings_text);
                    selectedIcon = (ImageView) mLayout.findViewById(R.id.nav_item_settings_icon);
                    break;
                default:
                    Log.w(TAG, "unknown selected section " + section);
            }
            a.recycle();

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

    private void attachOnClickListener(View v, final Class c) {
        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, c);
                mContext.startActivity(i);
            }
        });
    }


}
