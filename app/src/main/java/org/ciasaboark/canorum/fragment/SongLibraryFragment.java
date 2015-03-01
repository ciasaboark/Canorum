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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.adapter.SongAdapter;
import org.ciasaboark.canorum.playlist.provider.MergedProvider;
import org.ciasaboark.canorum.song.Track;

import java.util.List;


public class SongLibraryFragment extends Fragment implements AbsListView.OnItemClickListener, AbsListView.OnItemLongClickListener {
    private OnFragmentInteractionListener mListener;
    private View mView;
    private ListView mListView;
    private SongAdapter mAdapter;
    private List<Track> mTrackList;
    private EditText mSearchText;
    private FloatingActionButton mFab;

    public SongLibraryFragment() {
        // Required empty public constructor
    }

    public static SongLibraryFragment newInstance() {
        SongLibraryFragment fragment = new SongLibraryFragment();
        Bundle args = new Bundle();

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MergedProvider provider = MergedProvider.getInstance(getActivity());
        mTrackList = provider.getTrackList();
        mAdapter = new SongAdapter(getActivity(), mTrackList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_song_library, container, false);
        mListView = (ListView) mView.findViewById(R.id.list);
        mSearchText = (EditText) mView.findViewById(R.id.search_text);
        mFab = (FloatingActionButton) mView.findViewById(R.id.fab);
        initFab();
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);
        mSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //nothing to do here
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
                //nothing to do here
            }
        });

        return mView;
    }

    private void initFab() {
        mFab.attachToListView(mListView);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Track> tracks = mAdapter.getFilteredList();
                MusicControllerSingleton controller = MusicControllerSingleton.getInstance(getActivity());
                controller.addTracksToQueueHead(tracks);
                controller.playNext();
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Track track = mTrackList.get(position);
        MusicControllerSingleton controller = MusicControllerSingleton.getInstance(getActivity());
        controller.addTrackToQueueHead(track);
        controller.playNext();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final Track track = mTrackList.get(position);
        final MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(getActivity());
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);
        popupMenu.inflate(R.menu.library_long_click);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                boolean itemHandled = false;
                switch (item.getItemId()) {
                    case R.id.popup_menu_library_add_queue:
                        Toast.makeText(getActivity(), "Added " + track + " to queue", Toast.LENGTH_SHORT).show();
                        musicControllerSingleton.addTrackToQueue(track);
                        itemHandled = true;
                        break;
                    case R.id.popup_menu_library_play_next:
                        Toast.makeText(getActivity(), "Playing " + track + " next", Toast.LENGTH_SHORT).show();
                        musicControllerSingleton.addTrackToQueueHead(track);
                        itemHandled = true;
                        break;
                    case R.id.popup_menu_library_play_now:
                        Toast.makeText(getActivity(), "Playing " + track, Toast.LENGTH_SHORT).show();
                        musicControllerSingleton.addTrackToQueueHead(track);
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
}
