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

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.song.Track;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Formatter;
import java.util.Locale;

/**
 * Created by Jonathan Nelson on 2/7/15.
 */
public class SongView extends RelativeLayout {
    private static final String TAG = "SongView";
    private final Context mContext;
    private final Track mTrack;
    private View mLayout;
    private TextView mSongTitle;
    private TextView mSongTrackNum;
    private TextView mSongDuration;
    private ImageView mSongMenu;
    private View mRootView;
    private boolean mUseLightTheme;

    public SongView(Context ctx, AttributeSet attrs, Track track) {
        this(ctx, attrs, track, false);
    }

    public SongView(Context ctx, AttributeSet attrs, Track track, boolean useLightTheme) {
        super(ctx, attrs);
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        if (track == null) {
            throw new IllegalArgumentException("track can not be null");
        }
        mContext = ctx;
        mTrack = track;
        mUseLightTheme = useLightTheme;

        init();
    }

    private void init() {
        if (mUseLightTheme) {
            mLayout = (RelativeLayout) inflate(mContext, R.layout.view_song_dark, this);
        } else {
            mLayout = (RelativeLayout) inflate(mContext, R.layout.view_song_light, this);
        }

        mRootView = mLayout.findViewById(R.id.song);
        mSongTitle = (TextView) mLayout.findViewById(R.id.song_title);
        mSongTrackNum = (TextView) mLayout.findViewById(R.id.song_tracknum);
        mSongDuration = (TextView) mLayout.findViewById(R.id.song_duration);
        mSongMenu = (ImageButton) mLayout.findViewById(R.id.song_menu_icon);

        mSongTitle.setText(mTrack.getSong().getTitle());
        String formattedTime = getFormattedTime(mTrack.getSong().getDuration());
        mSongDuration.setText(formattedTime);
        int trackNum = mTrack.getSong().getFormattedTrackNum();
        if (trackNum != 0) {
            mSongTrackNum.setText(String.valueOf(trackNum));
        }

        mRootView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(mContext);
                musicControllerSingleton.addTrackToQueueHead(mTrack);
                musicControllerSingleton.playNext();
            }
        });

        mSongMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu menu = new PopupMenu(mContext, mSongMenu);
                menu.inflate(R.menu.menu_songview);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        boolean itemHandled = false;
                        MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(mContext);
                        switch (item.getItemId()) {
                            case R.id.songview_play_next:
                                musicControllerSingleton.addTrackToQueueHead(mTrack);
                                musicControllerSingleton.playNext();
                                itemHandled = true;
                                break;
                            case R.id.songview_play_now:
                                musicControllerSingleton.addTrackToQueueHead(mTrack);
                                itemHandled = true;
                                break;
                            case R.id.songview_add_queue:
                                musicControllerSingleton.addTrackToQueue(mTrack);
                                itemHandled = true;
                                break;
                            case R.id.songview_search_youtube:
                                if (musicControllerSingleton.isPlaying()) {
                                    musicControllerSingleton.pause(false);
                                }
                                String searchString = mTrack.getSong().getAlbum().getArtist()
                                        .getArtistName() + " " + mTrack.getSong().getTitle();

                                boolean searchLaunched = false;
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
                                        Log.e(TAG, "unable to launch search query for string:'" + searchString + "': " + ex.getMessage());
                                    }

                                }

                                if (musicControllerSingleton.isPlaying() || searchLaunched) {
                                    musicControllerSingleton.pause(false);
                                }
                                itemHandled = true;
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

}
