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
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.Song;

import java.util.Formatter;
import java.util.Locale;

/**
 * Created by Jonathan Nelson on 2/7/15.
 */
public class SongView extends RelativeLayout {
    private final Context mContext;
    private final Song mSong;
    private View mLayout;
    private TextView mSongTitle;
    private TextView mSongTrackNum;
    private TextView mSongDuration;
    private ImageView mSongMenu;

    public SongView(Context ctx, AttributeSet attrs, Song song) {
        super(ctx, attrs);
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        if (song == null) {
            throw new IllegalArgumentException("song can not be null");
        }
        mContext = ctx;
        mSong = song;
        init();
    }

    private void init() {
        mLayout = (RelativeLayout) inflate(mContext, R.layout.view_song, this);
        mSongTitle = (TextView) mLayout.findViewById(R.id.song_title);
        mSongTrackNum = (TextView) mLayout.findViewById(R.id.song_tracknum);
        mSongDuration = (TextView) mLayout.findViewById(R.id.song_duration);
        mSongMenu = (ImageButton) mLayout.findViewById(R.id.song_menu_icon);

        mSongTitle.setText(mSong.getTitle());
        String formattedTime = getFormattedTime(mSong.getDuration());
        mSongDuration.setText(formattedTime);
        mSongTrackNum.setText(mSong.getTrackNumText());


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
