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

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;

import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.adapter.PlaylistAdapter;
import org.ciasaboark.canorum.playlist.playlist.Playlist;
import org.ciasaboark.canorum.playlist.playlist.StaticPlaylist;
import org.ciasaboark.canorum.playlist.playlist.io.PlaylistReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaylistLibraryFragment extends Fragment {
    private static final String TAG = "PlaylistLibraryFragmet";
    private View mView;
    private OnFragmentInteractionListener mListener;
    private GridView mList;
    private ArrayAdapter<Playlist> mAdapter;
    private Map<Playlist, File> mKnownPlaylists;
    private List<Playlist> mPlaylistList;
    private View mErrorView;

    public static PlaylistLibraryFragment newInstance() {
        PlaylistLibraryFragment fragment = new PlaylistLibraryFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public PlaylistLibraryFragment() {
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_playlist_library, container, false);
        mList = (GridView) mView.findViewById(R.id.list);
        mErrorView = mView.findViewById(R.id.list_error);
        mKnownPlaylists = buildPlaylistsMap();
        if (mKnownPlaylists.isEmpty()) {
            showNoPlaylistsFound();
        } else {
            showPlaylistsList();
        }

        initToolbar();

        return mView;
    }

    private Map<Playlist, File> buildPlaylistsMap() {
        Map<Playlist, File> playlists = new HashMap<Playlist, File>();
        File playlistDir = PlaylistReader.getPlayListDirectory(getActivity());
        File[] files = playlistDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".plst")) {
                    FileInputStream fis = null;
                    ObjectInputStream ois = null;
                    try {
                        fis = new FileInputStream(file);
                        ois = new ObjectInputStream(fis);
                        Playlist.PlaylistMetadata metadata = (Playlist.PlaylistMetadata) ois.readObject();
                        StaticPlaylist playlist = (StaticPlaylist) ois.readObject();
                        playlist.setPlaylistMetadata(metadata);
                        playlists.put(playlist, file);
                    } catch (Exception e) {
                        Log.e(TAG, "error opening playlist file " + file);
                    } finally {
                        if (ois != null) {
                            try {
                                ois.close();
                            } catch (IOException e) {
                                Log.e(TAG, "error closing object input stream while building playlist: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException e) {
                                Log.e(TAG, "error closing file input stream while building playlist: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        return playlists;
    }

    private void showNoPlaylistsFound() {
        mErrorView.setVisibility(View.VISIBLE);
        mList.setVisibility(View.INVISIBLE);
    }

    private void showPlaylistsList() {
        mErrorView.setVisibility(View.INVISIBLE);
        mList.setVisibility(View.VISIBLE);
        mPlaylistList = new ArrayList<Playlist>(mKnownPlaylists.keySet());
        //sory by creation date
        Collections.sort(mPlaylistList, new Comparator<Playlist>() {
            @Override
            public int compare(Playlist lhs, Playlist rhs) {
                return ((Long) lhs.getCreationTimeStamp()).compareTo(rhs.getCreationTimeStamp());
            }
        });

        mAdapter = new PlaylistAdapter(getActivity(), R.layout.grid_playlist_single, mPlaylistList);
        mList.setAdapter(mAdapter);
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) mView.findViewById(R.id.local_playlist_toolbar);
        toolbar.setTitle("Playlists");
        toolbar.setTitleTextColor(getResources().getColor(R.color.toolbar_title_text));
        toolbar.setBackgroundColor(getResources().getColor(R.color.color_primary));
        mListener.setToolbar(toolbar);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_playlists, menu);
    }
}
