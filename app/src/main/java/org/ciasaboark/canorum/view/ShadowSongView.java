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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.song.Artist;
import org.ciasaboark.canorum.song.shadow.ShadowSong;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Formatter;
import java.util.Locale;

/**
 * Created by Jonathan Nelson on 2/7/15.
 */
public class ShadowSongView extends RelativeLayout {
    private static final String TAG = "ShadowSongView";
    private final Context mContext;
    private final ShadowSong mSong;
    private View mLayout;
    private TextView mSongTitle;
    private TextView mSongTrackNum;
    private TextView mSongDuration;
    private ImageView mSongMenu;
    private View mRootView;
    private boolean mUseLightTheme;
    private Artist mArtist;

    public ShadowSongView(Context ctx, AttributeSet attrs, ShadowSong song) {
        this(ctx, attrs, song, false);
    }

    public ShadowSongView(Context ctx, AttributeSet attrs, ShadowSong song, boolean useLightTheme) {
        super(ctx, attrs);
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        if (song == null) {
            throw new IllegalArgumentException("track can not be null");
        }
        mContext = ctx;
        mSong = song;
        mUseLightTheme = useLightTheme;

        init();
    }

    private void init() {
        if (mUseLightTheme) {
            mLayout = (RelativeLayout) inflate(mContext, R.layout.view_shadow_song_dark, this);
        } else {
            mLayout = (RelativeLayout) inflate(mContext, R.layout.view_shadow_song_light, this);
        }

        mRootView = mLayout.findViewById(R.id.song);
        mSongTitle = (TextView) mLayout.findViewById(R.id.song_title);
        mSongTrackNum = (TextView) mLayout.findViewById(R.id.song_tracknum);
        mSongDuration = (TextView) mLayout.findViewById(R.id.song_duration);
        mSongMenu = (ImageButton) mLayout.findViewById(R.id.song_menu_icon);

        mSongTitle.setText(mSong.getTitle());
        String formattedTime = getFormattedTime(mSong.getDuration());
        mSongDuration.setText(formattedTime);
        int trackNum = mSong.getFormattedTrackNum();
        if (trackNum != 0) {
            mSongTrackNum.setText(String.valueOf(trackNum));
        }

        mRootView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, "not yet implemented", Toast.LENGTH_SHORT).show();
            }
        });

        mSongMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu menu = new PopupMenu(mContext, mSongMenu);
                menu.inflate(R.menu.menu_shop_sites);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        boolean itemHandled = false;
                        MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(mContext);
                        switch (item.getItemId()) {
                            case R.id.popup_menu_shop_amazon:
                                Toast.makeText(mContext, "Not yet implemented", Toast.LENGTH_SHORT).show();
                                itemHandled = true;
                                break;
                            case R.id.popup_menu_shop_google_play:
                                Toast.makeText(mContext, "Not yet implemented", Toast.LENGTH_SHORT).show();
                                itemHandled = true;
                                break;
                            case R.id.popup_menu_shop_youtube:
                                boolean searchLaunched = false;
                                String searchString;
                                if (mArtist != null) {
                                    searchString = mArtist.getArtistName() + " " + mSong.getTitle();
                                } else {
                                    searchString = mSong.getTitle();
                                }
                                try {
                                    Intent youtubeIntent = new Intent(Intent.ACTION_SEARCH);
                                    youtubeIntent.setPackage("com.google.android.youtube");
                                    youtubeIntent.putExtra("query", searchString);
                                    youtubeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    mContext.startActivity(youtubeIntent);
                                    searchLaunched = true;
                                } catch (ActivityNotFoundException e) {
                                    //if the youtube app is not installed then we can just launch a regular web query
                                    Intent webIntent = new Intent(Intent.ACTION_VIEW);
                                    try {
                                        String encodedQueryString = URLEncoder.encode(searchString, "UTF-8");
                                        String baseUrl = "https://www.youtube.com/results?search_query=";
                                        webIntent.setData(Uri.parse(baseUrl + encodedQueryString));
                                        mContext.startActivity(webIntent);
                                        searchLaunched = true;
                                    } catch (UnsupportedEncodingException ex) {
                                        Log.e(TAG, "unable to launch search query for string:'" + mSong.getTitle() + "': " + ex.getMessage());
                                    }
                                }

                                if (searchLaunched) {
                                    if (musicControllerSingleton.isPlaying()) {
                                        musicControllerSingleton.pause();
                                    }
                                }
                                itemHandled = searchLaunched;
                                break;
                        }
                        return itemHandled;
                    }
                });
                menu.show();
            }
        });


    }

    private String getFormattedTime(int totalSeconds) {
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        Formatter formatter = new Formatter(sb, Locale.getDefault());
        if (hours > 0) {
            return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return formatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    public void setArtist(Artist artist) {
        mArtist = artist;
    }

}
