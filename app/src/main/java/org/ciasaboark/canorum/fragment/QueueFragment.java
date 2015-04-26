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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.adapter.QueueAdapter;
import org.ciasaboark.canorum.playlist.playlist.io.PlaylistWriter;
import org.ciasaboark.canorum.song.Track;
import org.ciasaboark.canorum.view.DynamicListView;

import java.util.ArrayList;
import java.util.List;


public class QueueFragment extends Fragment {
    private static final String TAG = "QueueFragment";
    private OnFragmentInteractionListener mListener;
    private DynamicListView mList;
    private QueueAdapter mAdapter;
    private View mView;
    private Toolbar mToolbar;
    private List<Track> mQueuedTracks;
    private Menu mToolbarMenu;

    private MusicControllerSingleton mMusicControllerSingleton;

    public QueueFragment() {
        // Required empty public constructor
    }

    public static QueueFragment newInstance() {
        QueueFragment fragment = new QueueFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    private void findChildren() {
        mList = (DynamicListView) mView.findViewById(R.id.list);
        mAdapter = new QueueAdapter(getActivity(), R.layout.list_song, mQueuedTracks);
        mToolbar = (Toolbar) mView.findViewById(R.id.local_queue_toolbar);
    }

    private void initToolbars() {
        mToolbar.setBackgroundColor(getResources().getColor(R.color.color_primary));
        mToolbar.setTitle("Playback Queue");
        mToolbar.setTitleTextColor(getResources().getColor(R.color.toolbar_title_text));
        mListener.setToolbar(mToolbar);
    }

    private void initAdapter(List<Track> trackList) {
        mList.setAdapter(mAdapter);
        updateAdapter(new ArrayList<Track>(trackList));
    }

    private void initBroadcastReceivers() {
        IntentFilter intentFilter = new IntentFilter(MusicControllerSingleton.ACTION_NEXT);
        intentFilter.addAction(MusicControllerSingleton.ACTION_PREV);
        intentFilter.addAction(MusicControllerSingleton.ACTION_PAUSE);
        intentFilter.addAction(MusicControllerSingleton.ACTION_PLAY);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mQueuedTracks = mMusicControllerSingleton.getQueuedTracks();
                updateAdapter(mQueuedTracks);
            }
        }, intentFilter);
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
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_queue, container, false);
        mMusicControllerSingleton = MusicControllerSingleton.getInstance(getActivity());
        mQueuedTracks = mMusicControllerSingleton.getQueuedTracks();
        hasOptionsMenu();
        findChildren();

        mList.setListViewReorderListener(new DynamicListView.ListViewReorderListener() {
            @Override
            public void onListReordered(List list) {
                mMusicControllerSingleton.replaceQueue(list);
            }
        });

        initToolbars();
        initBroadcastReceivers();
        initAdapter(mQueuedTracks);

        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
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
        inflater.inflate(R.menu.menu_queue, menu);
        mToolbarMenu = menu;
        MenuItem save = mToolbarMenu.findItem(R.id.action_save_queue);
        MenuItem clear = mToolbarMenu.findItem(R.id.action_clear_queue);
        boolean saveAndClearEnabled = !mQueuedTracks.isEmpty();
        if (saveAndClearEnabled) {
            save.setEnabled(true);
            save.getIcon().setAlpha(255);
            clear.setEnabled(true);
            clear.getIcon().setAlpha(255);
        } else {
            save.setEnabled(false);
            save.getIcon().setAlpha(128);
            clear.setEnabled(false);
            clear.getIcon().setAlpha(128);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean itemHandled = false;
        final MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(getActivity());
        switch (item.getItemId()) {
            case R.id.action_clear_queue:
                ArrayList<Track> emptyQueue = new ArrayList<Track>();
                setPlayQueue(emptyQueue);
                musicControllerSingleton.replaceQueue(emptyQueue);
                updateAdapter(emptyQueue);
                updateMenuIcons();
                itemHandled = true;
                break;
            case R.id.action_save_queue:
                List<Track> trackList = musicControllerSingleton.getQueuedTracks();
                PlaylistWriter playlistWriter = new PlaylistWriter(getActivity())
                        .setListener(new PlaylistWriter.PlaylistWriterListener() {
                            @Override
                            public void onPlaylistWritten(boolean playListWritten, String message) {
                                if (playListWritten) {
                                    Toast.makeText(getActivity(), "Playlist written to disk", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getActivity(), "Playlist could not be written to disk: " + message, Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .saveTrackList(trackList);
                itemHandled = true;
                break;
        }
        return itemHandled;
    }


    private void setPlayQueue(List<Track> newQueue) {
        mQueuedTracks = newQueue;
    }

    private void updateAdapter(List<Track> trackList) {
        mAdapter.clear();
        mQueuedTracks = trackList;
        mAdapter.addAll(trackList);
        mAdapter.notifyDataSetChanged();
        updateMenuIcons();
    }

    private void updateMenuIcons() {
        if (mToolbarMenu == null) {
            Log.d(TAG, "delaying menu item update until after menu is inflated");
        } else {
            Activity activity = getActivity();
            if (activity != null) activity.invalidateOptionsMenu();
        }
    }
}
