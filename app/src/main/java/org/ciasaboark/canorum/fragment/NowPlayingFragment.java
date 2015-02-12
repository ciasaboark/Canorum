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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.song.Song;
import org.ciasaboark.canorum.view.MusicControllerView;
import org.ciasaboark.canorum.view.NowPlayingView;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NowPlayingFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link NowPlayingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NowPlayingFragment extends Fragment {
    private static final String TAG = "NowPlayingFragment";
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private MusicControllerView mMediaControls;
    private NowPlayingView mNowPlayingView;
    private IntentFilter mIntentFilter;
    private BroadcastReceiver mBroadcastReceiver;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public NowPlayingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment NowPlayingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static NowPlayingFragment newInstance(String param1, String param2) {
        NowPlayingFragment fragment = new NowPlayingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
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


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_now_playing, container, false);

        mNowPlayingView = (NowPlayingView) rootView.findViewById(R.id.now_playing);
        mNowPlayingView.setPaletteGenerateListener(new PaletteGeneratedWatcher() {
            @Override
            public void onPaletteGenerated(Palette palette) {
                Palette.Swatch muted = palette.getMutedSwatch();
                Palette.Swatch darkmuted = palette.getDarkMutedSwatch();
                int color;

                if (darkmuted != null) {
                    color = darkmuted.getRgb();
                } else if (muted != null) {
                    color = muted.getRgb();
                } else {
                    color = getActivity().getResources().getColor(R.color.color_primary);
                }

                mListener.setToolbarColor(color);
            }
        });
        mMediaControls = (MusicControllerView) rootView.findViewById(R.id.media_controls);

        setupController();
        initBroadcastReceivers();

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(MusicControllerSingleton.ACTION_PLAY);
        mIntentFilter.addAction(MusicControllerSingleton.ACTION_PAUSE);
        mIntentFilter.addAction(MusicControllerSingleton.ACTION_NEXT);
        mIntentFilter.addAction(MusicControllerSingleton.ACTION_PREV);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case MusicControllerSingleton.ACTION_PLAY:
                        Song curSong = null;
                        curSong = (Song) intent.getSerializableExtra("curSong");
                        if (curSong == null) {
                            Log.w(TAG, "got broadcast notification that a song has began playing, but could " +
                                    "not get song from intent");
                        }
                        updateNowPlayCard();
                        break;
                    default:
                        Log.d(TAG, "got a broadcast notification with action type " +
                                intent.getAction() + " which is not yet supported");
                }
            }
        };

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    @Override
    public void onResume() {
        super.onResume();
        mNowPlayingView.updateWidgets();
        mMediaControls.updateWidgets();
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

    private void setupController() {
//        mMediaControls.setPrevNextListeners(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                musicControllerSingleton.playPrev();
//            }
//        }, new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                musicControllerSingleton.playNext();
//            }
//        });
        mMediaControls.setMediaPlayerController(MusicControllerSingleton.getInstance(getActivity()));
        mMediaControls.setEnabled(true);
    }

    private void initBroadcastReceivers() {
        //TODO
    }

    private void updateNowPlayCard() {
        mNowPlayingView.updateWidgets();
    }

}
