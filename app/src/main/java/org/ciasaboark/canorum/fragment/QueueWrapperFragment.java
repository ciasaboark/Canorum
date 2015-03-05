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
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.view.MiniControllerView;


public class QueueWrapperFragment extends Fragment {
    private OnFragmentInteractionListener mListener;
    private View mView;
    private MiniControllerView mController;

    public QueueWrapperFragment() {
        // Required empty public constructor
    }

    public static QueueWrapperFragment newInstance() {
        QueueWrapperFragment fragment = new QueueWrapperFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    private void initBroadcastReceivers() {
        IntentFilter intentFilter = new IntentFilter(MusicControllerSingleton.ACTION_NEXT);
        intentFilter.addAction(MusicControllerSingleton.ACTION_PREV);
        intentFilter.addAction(MusicControllerSingleton.ACTION_PAUSE);
        intentFilter.addAction(MusicControllerSingleton.ACTION_PLAY);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mController.updateWidgets();
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
        mView = inflater.inflate(R.layout.fragment_queue_wrapper, container, false);
        mController = (MiniControllerView) mView.findViewById(R.id.mini_controller);
        initMiniController();
        initQueueFragment();
        setToolbarTitle("Play Queue");
        return mView;
    }

    private void initMiniController() { //TODO we should be passing a request to the main activity here instead of doing the fragment transation ourselfs
        MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(getActivity());
        mController.setMediaPlayerController(musicControllerSingleton);
        mController.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BitmapDrawable initialAlbumArt = mController.getAlbumArtwork();
                NowPlayingFragment nowPlayingFragment = NowPlayingFragment.newInstance(initialAlbumArt);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    nowPlayingFragment.setSharedElementEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.move));
                    nowPlayingFragment.setSharedElementReturnTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.move));
                    nowPlayingFragment.setEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.explode));
                }


                View albumImage = mController.getAlbumImageView();

                getActivity().getSupportFragmentManager().beginTransaction()
                        .addSharedElement(albumImage, "albumImage")
                        .addToBackStack(null)
                        .replace(R.id.main_fragment, nowPlayingFragment)
                        .commit();
            }
        });
        if (musicControllerSingleton.isPlaying() || musicControllerSingleton.isPaused()) {
            showMiniController();
        } else {
            hideMiniController();
        }
    }

    private void initQueueFragment() {
        Fragment queueFragment = QueueFragment.newInstance();
        FragmentManager fm = getActivity().getSupportFragmentManager();
        fm.beginTransaction()
                .add(R.id.queue_inner_fragment, queueFragment)
                .commit();
    }

    private void setToolbarTitle(String s) {
        //TODO use local toolbar
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
