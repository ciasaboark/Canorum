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

package org.ciasaboark.canorum.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.song.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Jonathan Nelson on 2/19/15.
 */
public class SongAdapter extends ArrayAdapter<Track> implements Filterable, FilterableAdapter<Track> {
    private static final String TAG = "SongAdapter";
    private final Context mContext;
    private List<Track> mDataBackup;
    private List<Track> mData;
    private String mFilteredText;

    public SongAdapter(Context ctx, List<Track> data) {
        super(ctx, R.layout.list_song, data);
        mContext = ctx;
        mDataBackup = new ArrayList<Track>(data);
        mData = data;
        sortList(mData);
    }

    private void sortList(List<Track> tracks) {
        Collections.sort(tracks, new Comparator<Track>() {
            @Override
            public int compare(Track lhs, Track rhs) {
                return lhs.getSong().getTitle().compareTo(rhs.getSong().getTitle());
            }
        });
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        SongHolder holder = null;
        final Track track = mData.get(pos);

        if (convertView != null) {
            holder = (SongHolder) convertView.getTag();
        } else {
            holder = new SongHolder();
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_song, null);
            holder.titleText = (TextView) convertView.findViewById(R.id.track_title);
            holder.artistText = (TextView) convertView.findViewById(R.id.track_artist);
            holder.albumText = (TextView) convertView.findViewById(R.id.track_album);
            convertView.setTag(holder);
        }

        holder.position = pos;
        if (mFilteredText == null || mFilteredText.length() == 0) {
            holder.titleText.setText(track.getSong().getTitle());
            holder.artistText.setText(track.getArtist().getArtistName());
            holder.albumText.setText(track.getAlbum().getAlbumName());
        } else {
            //highlight filtered text in track title (if any)
            String title = track.getSong().getTitle();
            int titleStartPos = title.toLowerCase().indexOf(mFilteredText);
            if (titleStartPos == -1) {
                //the filter text is not in the song title
                holder.titleText.setText(title);
            } else {
                int endPos = titleStartPos + mFilteredText.length();
                Spannable spannable = new SpannableString(title);
                int accentColor = mContext.getResources().getColor(R.color.color_accent);
                ColorStateList highlightColor = new ColorStateList(new int[][]{new int[]{}}, new int[]{accentColor});
                TextAppearanceSpan highlightSpan = new TextAppearanceSpan(null, Typeface.BOLD, -1, highlightColor, null);
                spannable.setSpan(highlightSpan, titleStartPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.titleText.setText(spannable);
            }

            //highlight filtered text in artist title (if any)
            String artist = track.getArtist().getArtistName();
            int artistStartPos = artist.toLowerCase().indexOf(mFilteredText);
            if (artistStartPos == -1) {
                holder.artistText.setText(artist);
            } else {
                int endPos = artistStartPos + mFilteredText.length();
                Spannable spannable = new SpannableString(artist);
                int accentColor = mContext.getResources().getColor(R.color.color_accent);
                ColorStateList highlightColor = new ColorStateList(new int[][]{new int[]{}}, new int[]{accentColor});
                TextAppearanceSpan highlightSpan = new TextAppearanceSpan(null, Typeface.BOLD, -1, highlightColor, null);
                spannable.setSpan(highlightSpan, artistStartPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.artistText.setText(spannable);
            }

            //highlight filtered text in album title (if any)
            String album = track.getAlbum().getAlbumName();
            int albumStartPos = album.toLowerCase().indexOf(mFilteredText);
            if (albumStartPos == -1) {
                holder.albumText.setText(album);
            } else {
                int endPos = albumStartPos + mFilteredText.length();
                Spannable spannable = new SpannableString(album);
                int accentColor = mContext.getResources().getColor(R.color.color_accent);
                ColorStateList highlightColor = new ColorStateList(new int[][]{new int[]{}}, new int[]{accentColor});
                TextAppearanceSpan highlightSpan = new TextAppearanceSpan(null, Typeface.BOLD, -1, highlightColor, null);
                spannable.setSpan(highlightSpan, albumStartPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.albumText.setText(spannable);
            }
        }


        return convertView;
    }

    @Override
    public Filter getFilter() {

        Filter filter = new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<Track> filteredTracks = new ArrayList<Track>();
                mFilteredText = constraint.toString().toLowerCase();

                //if the filer is empty then show all the original tracks
                if (constraint == null || constraint.length() == 0) {
                    filteredTracks = mDataBackup;
                } else {
                    constraint = constraint.toString().toLowerCase();
                    for (Track track : mDataBackup) {
                        //filtering is done by track title, artist, and album
                        if (track.getSong().getTitle().toLowerCase().contains(constraint.toString())) {
                            filteredTracks.add(track);
                        } else if (track.getArtist().getArtistName().toLowerCase().contains(constraint.toString())) {
                            filteredTracks.add(track);
                        } else if (track.getAlbum().getAlbumName().toLowerCase().contains(constraint.toString())) {
                            filteredTracks.add(track);
                        }
                    }
                }

                results.count = filteredTracks.size();
                results.values = filteredTracks;

                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                List<Track> filteredTracks = (List<Track>) results.values;
                sortList(filteredTracks);
                clear();
                for (Track track : filteredTracks) {
                    add(track);
                }
            }
        };

        return filter;
    }

    @Override
    public List<Track> getFilteredList() {
        return mData;
    }

    private class SongHolder {
        public TextView titleText;
        public TextView artistText;
        public TextView albumText;
        public int position;
    }
}
