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
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.ciasaboark.canorum.Album;
import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.Song;
import org.ciasaboark.canorum.adapter.AlbumAdapter;
import org.ciasaboark.canorum.playlist.SystemLibrary;

import java.util.List;

/**
 * Created by Jonathan Nelson on 2/5/15.
 */
public class AlbumLibraryFragment extends Fragment implements AbsListView.OnItemClickListener, AbsListView.OnItemLongClickListener {
    private static final String TAG = "AlbumLibraryFragment";
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private List<Album> mAlbumList;
    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;
    private OnFragmentInteractionListener mListener;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;

    private String mParam1;
    private String mParam2;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AlbumLibraryFragment() {
    }

    public static AlbumLibraryFragment newInstance(String param1, String param2) {
        AlbumLibraryFragment fragment = new AlbumLibraryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
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

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        // TODO: Change Adapter to display your content
        SystemLibrary systemLibrary = new SystemLibrary(getActivity());
        mAlbumList = systemLibrary.getAlbumList();

        mAdapter = new AlbumAdapter(getActivity(), mAlbumList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.artist_fragment_item_grid, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
//        mListener = null;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //TODO create artist view fragment and load it here
        Toast.makeText(getActivity(), "clicked: " + mAlbumList.get(position), Toast.LENGTH_SHORT).show();
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        //get a list of all songs belonging to the selected artist
        Album album = (Album) parent.getItemAtPosition(position);
        final List<Song> artistSongs = getAlbumSongs(album);
        final MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(getActivity());
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);
        popupMenu.inflate(R.menu.library_long_click);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                boolean itemHandled = false;
                switch (item.getItemId()) {
                    case R.id.popup_menu_library_add_queue:
                        Toast.makeText(getActivity(), "Added " + mAlbumList.get(position) + " to queue", Toast.LENGTH_SHORT).show();
                        musicControllerSingleton.addSongsToQueue(artistSongs);
                        itemHandled = true;
                        break;
                    case R.id.popup_menu_library_play_next:
                        Toast.makeText(getActivity(), "Playing " + mAlbumList.get(position) + " next", Toast.LENGTH_SHORT).show();
                        musicControllerSingleton.addSongsToQueueHead(artistSongs);
                        itemHandled = true;
                        break;
                    case R.id.popup_menu_library_play_now:
                        Toast.makeText(getActivity(), "Playing " + mAlbumList.get(position), Toast.LENGTH_SHORT).show();
                        musicControllerSingleton.addSongsToQueueHead(artistSongs);
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

    private List<Song> getAlbumSongs(Album album) {
        SystemLibrary systemLibrary = new SystemLibrary(getActivity());
        List<Song> songList = systemLibrary.getSongsForAlbum(album);
        return songList;
    }


}
