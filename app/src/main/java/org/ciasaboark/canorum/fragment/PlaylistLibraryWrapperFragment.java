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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.view.MiniControllerView;

public class PlaylistLibraryWrapperFragment extends Fragment {
    private static final String TAG = "PlaylistLibraryWrapperFragment";
    private View mView;
    private View mInnerFragmentContainer;
    private OnFragmentInteractionListener mListener;
    private MiniControllerView mController;

    public PlaylistLibraryWrapperFragment() {
        // Required empty public constructor
    }

    public static PlaylistLibraryWrapperFragment newInstance() {
        PlaylistLibraryWrapperFragment fragment = new PlaylistLibraryWrapperFragment();
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_playlist_library_wrapper, container, false);
        initInnerFragment();
        initMiniController();
        initBroadcastReceivers();
        return mView;
    }

    private void initInnerFragment() {
        mInnerFragmentContainer = mView.findViewById(R.id.playlist_inner_fragment);
        PlaylistLibraryFragment libraryFragment = PlaylistLibraryFragment.newInstance();
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.playlist_inner_fragment, libraryFragment).commit();
    }

    private void initMiniController() {
        MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(getActivity());
        mController = (MiniControllerView) mView.findViewById(R.id.mini_controller);
        mController.setMediaPlayerController(musicControllerSingleton);
        mController.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.navigateToTopLevelFragment(TOP_LEVEL_FRAGMENTS.CUR_PLAYING);
            }
        });
        if (musicControllerSingleton.isPlaying() || musicControllerSingleton.isPaused()) {
            showMiniController();
        } else {
            hideMiniController();
        }
    }

    private void initBroadcastReceivers() {
        IntentFilter intentFilter = new IntentFilter(MusicControllerSingleton.ACTION_NEXT);
        intentFilter.addAction(MusicControllerSingleton.ACTION_PREV);
        intentFilter.addAction(MusicControllerSingleton.ACTION_PAUSE);
        intentFilter.addAction(MusicControllerSingleton.ACTION_PLAY);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                showMiniController();
            }
        }, intentFilter);
    }

    private void showMiniController() {
        MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(getActivity());
        mController.updateWidgets();
        if (musicControllerSingleton.isPlaying() || musicControllerSingleton.isPaused()) {
            mController.setVisibility(View.VISIBLE);
            mController.setEnabled(true);
        }
    }

    private void hideMiniController() {
        mController.setEnabled(false);
        mController.setVisibility(View.GONE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

}
