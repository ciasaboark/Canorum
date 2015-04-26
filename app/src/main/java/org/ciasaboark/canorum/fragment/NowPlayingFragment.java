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
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.graphics.Palette;
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
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.song.Track;
import org.ciasaboark.canorum.view.MusicControllerView;
import org.ciasaboark.canorum.view.NowPlayingView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class NowPlayingFragment extends Fragment {
    private static final String TAG = "NowPlayingFragment";
    private static BitmapDrawable sInitialArtwork = null;
    private MusicControllerView mMediaControls;
    private NowPlayingView mNowPlayingView;
    private IntentFilter mIntentFilter;
    private BroadcastReceiver mBroadcastReceiver;
    private OnFragmentInteractionListener mListener;
    private View mView;
    private Toolbar mToolbar;

    public NowPlayingFragment() {
        // Required empty public constructor
    }

    public static NowPlayingFragment newInstance() {
        return newInstance(null);
    }

    public static NowPlayingFragment newInstance(BitmapDrawable initialArtwork) {
        NowPlayingFragment fragment = new NowPlayingFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        sInitialArtwork = initialArtwork;
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
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_now_playing, container, false);

        mNowPlayingView = (NowPlayingView) mView.findViewById(R.id.now_playing);
        mNowPlayingView.setPaletteGenerateListener(new PaletteGeneratedWatcher() {
            @Override
            public void onPaletteGenerated(Palette palette, Object tag) {
                Context ctx = getActivity();
                if (ctx != null) {
                    mMediaControls.onPaletteGenerated(palette);
                    mListener.onPaletteGenerated(palette);
                }
            }
        });
        mMediaControls = (MusicControllerView) mView.findViewById(R.id.media_controls);

        setupController();
        initBroadcastReceivers();

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(MusicControllerSingleton.ACTION_PLAY);
        mIntentFilter.addAction(MusicControllerSingleton.ACTION_PAUSE);
        mIntentFilter.addAction(MusicControllerSingleton.ACTION_NEXT);
        mIntentFilter.addAction(MusicControllerSingleton.ACTION_PREV);

        initToolbar();
        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sInitialArtwork != null) {
            mNowPlayingView.primeAlbumArtwork(sInitialArtwork);
            sInitialArtwork = null;
        }
        mNowPlayingView.updateWidgets();
        mMediaControls.updateWidgets();
        updateToolbar();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
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
        inflater.inflate(R.menu.menu_now_playing, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean itemHandled = false;
        switch (item.getItemId()) {
            case R.id.action_now_youtube:
                MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(getActivity());
                Track curTrack = musicControllerSingleton.getCurTrack();
                String searchString = curTrack.getSong().getAlbum().getArtist().getArtistName() + " " + curTrack.getSong().getTitle();

                boolean searchLaunched = false;
                try {
                    Intent youtubeIntent = new Intent(Intent.ACTION_SEARCH);
                    youtubeIntent.setPackage("com.google.android.youtube");
                    youtubeIntent.putExtra("query", searchString);
                    youtubeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(youtubeIntent);
                    searchLaunched = true;
                } catch (ActivityNotFoundException e) {
                    //if the youtube app is not installed then we can just launch a regular web query
                    Intent webIntent = new Intent(Intent.ACTION_VIEW);
                    try {
                        String encodedQueryString = URLEncoder.encode(searchString, "UTF-8");
                        String baseUrl = "https://www.youtube.com/results?search_query=";
                        webIntent.setData(Uri.parse(baseUrl + encodedQueryString));
                        startActivity(webIntent);
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
            case R.id.action_now_artwork_search:
                Toast.makeText(getActivity(), "Not supported yet", Toast.LENGTH_SHORT).show();
                break;
        }
        return itemHandled;
    }

    private void setupController() {
        MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(getActivity());
        mMediaControls.setMediaPlayerController(musicControllerSingleton);
        mMediaControls.setEnabled(true);
    }

    private void initBroadcastReceivers() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case MusicControllerSingleton.ACTION_PLAY:
                        Track curSong = (Track) intent.getSerializableExtra("curSong");
                        if (curSong == null) {
                            Log.w(TAG, "got broadcast notification that a song has began playing, but could " +
                                    "not get song from intent");
                        }
                        updateNowPlayCard();
                        updateToolbar();
                        break;
                    default:
                        Log.d(TAG, "got a broadcast notification with action type " +
                                intent.getAction() + " which is not yet supported");
                }
            }
        };
    }

    private void updateNowPlayCard() {
        mNowPlayingView.updateWidgets();
    }

    private void updateToolbar() {
        Track curTrack = MusicControllerSingleton.getInstance(getActivity()).getCurTrack();
        String title;
        if (curTrack == null) {
            mToolbar.setTitle(getString(R.string.app_name));
        } else {
            mToolbar.setTitle(curTrack.getSong().getTitle());
        }

        mToolbar.setTitleTextColor(getResources().getColor(R.color.toolbar_title_text));
        mListener.setToolbar(mToolbar);
    }

    private void initToolbar() {
        mToolbar = (Toolbar) mView.findViewById(R.id.local_toolbar);
        updateToolbar();
    }

}
