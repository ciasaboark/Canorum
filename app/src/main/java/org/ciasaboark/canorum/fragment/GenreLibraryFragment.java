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
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.adapter.GenreAdapter;
import org.ciasaboark.canorum.playlist.provider.MergedProvider;
import org.ciasaboark.canorum.song.Genre;
import org.ciasaboark.canorum.song.Track;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;


public class GenreLibraryFragment extends Fragment implements AdapterView.OnItemClickListener {
    private OnFragmentInteractionListener mListener;
    private GridView mList;
    private ArrayAdapter<Genre> mAdapter;
    private List<Genre> mGenreList;
    private View mView;
    private RelativeLayout mWordCloudView;
    private LruCache<String, Bitmap> mMemoryCache;


    public GenreLibraryFragment() {
        // Required empty public constructor
    }

    public static GenreLibraryFragment newInstance() {
        GenreLibraryFragment fragment = new GenreLibraryFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public static <K, V extends Comparable<V>> Map<K, V> sortByValues(final Map<K, V> map) {
        Comparator<K> valueComparator = new Comparator<K>() {
            public int compare(K k1, K k2) {
                int compare =
                        map.get(k1).compareTo(map.get(k2));
                if (compare == 0)
                    return 1;
                else
                    return compare;
            }
        };

        Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
        sortedByValues.putAll(map);
        return sortedByValues;
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

        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/10th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 10;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_genre_library, container, false);
        mList = (GridView) mView.findViewById(R.id.list);
        MergedProvider provider = MergedProvider.getInstance(getActivity());
        mGenreList = provider.getKnownGenres();
        mWordCloudView = (RelativeLayout) mView.findViewById(R.id.word_cloud);

        mAdapter = new GenreAdapter(getActivity(), R.layout.grid_genre_single, mGenreList, mMemoryCache);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);
        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        mView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                mView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                drawWordCloud();
            }
        });
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

    private void drawWordCloud() {
        Map<String, Integer> words = new HashMap<>();
        MergedProvider mergedProvider = MergedProvider.getInstance(getActivity());
        for (Genre genre : mGenreList) {
            List<Track> genreTracks = mergedProvider.getTracksForGenre(genre);
            words.put(genre.getGenre(), genreTracks.size());
        }

        mWordCloudView.removeAllViews();

        int wordCloudWidth = mWordCloudView.getMeasuredWidth();
        int wordCloudHeight = mWordCloudView.getHeight();

        if (wordCloudHeight == 0 || wordCloudWidth == 0) {
            Log.d("asdf", "can not measure width yet, will not render word cloud");
            return;
        }

        Random random = new Random();

        for (String word : words.keySet()) {
            TextView textView = new TextView(getActivity());

            mWordCloudView.addView(textView);

            textView.setText(word);
            final int MIN_TEXT_SIZE = 6;
            final int MAX_TEXT_SIZE = 15;
            int wordCount = words.get(word);
            int textSize = MIN_TEXT_SIZE + --wordCount;
            textSize = textSize > 20 ? 20 : textSize;
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PT, textSize);

            int translationXMin = 0;
            int translationXMax = wordCloudWidth / 2;
            int translationX = random.nextInt((translationXMax - translationXMin) + translationXMin);

            int translationYMin = 0;
            int translationYMax = wordCloudHeight / 2;
            int translationY = random.nextInt((translationYMax - translationYMin) + translationYMin);

            boolean flipX = random.nextBoolean();
            if (flipX) {
                translationX = -translationX;
            }

            boolean flipY = random.nextBoolean();
            if (flipY) {
                translationY = -translationY;
            }

            textView.setTranslationX(translationX);
            textView.setTranslationY(translationY);
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Genre genre = mGenreList.get(position);
        GenreDetailFragment genreFragment = GenreDetailFragment.newInstance(genre);
        getActivity().getSupportFragmentManager().beginTransaction()
                .addToBackStack(null)
                .replace(R.id.library_inner_fragment, genreFragment)
                .commit();
    }
}
