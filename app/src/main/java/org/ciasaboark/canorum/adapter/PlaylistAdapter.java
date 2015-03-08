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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.playlist.playlist.Playlist;
import org.ciasaboark.canorum.song.Track;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Jonathan Nelson on 3/7/15.
 */
public class PlaylistAdapter extends ArrayAdapter<Playlist> implements FilterableAdapter<Playlist> {
    private static final String TAG = "GenreAdapter";
    private Context mContext;
    private List<Playlist> mData;
    private View.OnClickListener mMenuListener;

    public PlaylistAdapter(Context context, int resource, List<Playlist> data) {
        super(context, resource, data);
        mData = data;
        mContext = context;
    }

    public void setMenuListener(View.OnClickListener listener) {
        mMenuListener = listener;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        PlaylistHolder holder = null;
        final Playlist playlist = getItem(pos);

        if (convertView != null) {
            holder = (PlaylistHolder) convertView.getTag();
        } else {
            holder = new PlaylistHolder();
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_playlist, null);
            holder.playlistIcon = (ImageView) convertView.findViewById(R.id.playlist_icon);
            holder.playlistName = (TextView) convertView.findViewById(R.id.playlist_name);
            holder.playlistDate = (TextView) convertView.findViewById(R.id.playlist_date);
            holder.playlistMenuIcon = (ImageView) convertView.findViewById(R.id.playlist_menu_icon);
            convertView.setTag(holder);
        }

        final PlaylistHolder finalHolder = holder;
        holder.position = pos;
        holder.playlistName.setText(playlist.getName());
        Date date = new Date(playlist.getCreationTimeStamp());
        DateFormat formatter = DateFormat.getDateTimeInstance();
        holder.playlistDate.setText(formatter.format(date));
        holder.playlistMenuIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(mContext, finalHolder.playlistMenuIcon);
                popupMenu.inflate(R.menu.menu_playlist);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        boolean itemHandled = false;
                        MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(mContext);
                        switch (item.getItemId()) {
                            case R.id.menu_playlist_play:
                                List<Track> tracks = playlist.getTrackList();
                                musicControllerSingleton.replaceQueue(tracks);
                                musicControllerSingleton.playNext();
                                itemHandled = true;
                                break;
                            case R.id.menu_playlist_shuffle:
                                List<Track> shuffledTracks = playlist.getTrackList();
                                Collections.shuffle(shuffledTracks);
                                musicControllerSingleton.replaceQueue(shuffledTracks);
                                musicControllerSingleton.playNext();
                                itemHandled = true;
                                break;
                            case R.id.menu_playlist_rename:
                                Toast.makeText(mContext, "Not yet supported", Toast.LENGTH_SHORT).show();
                                itemHandled = true;
                                break;
                            case R.id.menu_playlist_delete:
                                Toast.makeText(mContext, "Not yet supported", Toast.LENGTH_SHORT).show();
                                itemHandled = true;
                                break;
                        }
                        return itemHandled;
                    }
                });
                popupMenu.show();
            }
        });
        //TODO set icon
//        BitmapDrawable icon = new BitmapDrawable(playlist.getIcon());
//        holder.playlistIcon.setImageDrawable(icon);


        return convertView;
    }

    @Override
    public List<Playlist> getFilteredList() {
        return mData;
    }

    private class PlaylistHolder {
        public ImageView playlistIcon;
        public TextView playlistName;
        public TextView playlistDate;
        public ImageView playlistMenuIcon;
        public int position;
    }
}
