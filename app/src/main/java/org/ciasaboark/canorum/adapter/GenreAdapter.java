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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.cache.ArtworkLruCache;
import org.ciasaboark.canorum.artwork.genre.GenreArtGenerator;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedListener;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.playlist.provider.MergedProvider;
import org.ciasaboark.canorum.song.Genre;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Jonathan Nelson on 2/19/15.
 */
public class GenreAdapter extends ArrayAdapter<Genre> implements FilterableAdapter<Genre> {
    private static final String TAG = "GenreAdapter";
    private Context mContext;
    private List<Genre> mData;
    private Map<Genre, Integer> mGenreTrackCount;
    private ArtworkLruCache mCache;

    private int mImgHeight = 0;
    private int mImgWidth = 0;
    private boolean mListenerTriggered = false;

    public GenreAdapter(Context context, int resource, List<Genre> data) {
        super(context, resource, data);
        mData = data;
        mGenreTrackCount = new HashMap<>();
        mContext = context;
        mCache = ArtworkLruCache.getInstance();
        fillGenreTrackCount();
        sortGenres();
    }

    private void fillGenreTrackCount() {
        MergedProvider mergedProvider = MergedProvider.getInstance(mContext);
        Iterator<Genre> it = mData.iterator();
        while (it.hasNext()) {
            Genre genre = it.next();
            int count = mergedProvider.getTracksForGenre(genre).size();
            if (count == 0) {
                it.remove();
            } else {
                mGenreTrackCount.put(genre, count);
            }
        }

    }

    private void sortGenres() {
        Collections.sort(mData, new Comparator<Genre>() {
            @Override
            public int compare(Genre lhs, Genre rhs) {
                return lhs.getGenre().compareTo(rhs.getGenre());
            }
        });
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        GenreHolder holder = null;
        final Genre genre = getItem(pos);

        if (convertView != null) {
            holder = (GenreHolder) convertView.getTag();
        } else {
            holder = new GenreHolder();
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.grid_genre_single, null);
            holder.genreImage = (ImageSwitcher) convertView.findViewById(R.id.genre_image);
            holder.genreText = (TextView) convertView.findViewById(R.id.genre_text);
            holder.genreCountText = (TextView) convertView.findViewById(R.id.genre_text_count);
            holder.genreTextBox = convertView.findViewById(R.id.genre_text_box);
            initImageSwitcher(holder.genreImage);
            convertView.setTag(holder);
        }

        final GenreHolder finalHolder = holder;
        holder.position = pos;
        holder.genreText.setText(genre.getGenre());
        holder.genreCountText.setText(mGenreTrackCount.get(genre) + " tracks");
        holder.genreTextBox.setBackgroundColor(mContext.getResources().getColor(R.color.color_primary));

        //temporarily disable the imageswitcher animations so we can apply the default album art with
        //no fanfare
        Animation inAnimation = holder.genreImage.getInAnimation();
        Animation outAnimation = holder.genreImage.getOutAnimation();
        holder.genreImage.setInAnimation(null);
        holder.genreImage.setOutAnimation(null);
        holder.genreImage.setImageDrawable(new ColorDrawable(mContext.getResources().getColor(android.R.color.white)));
        holder.genreImage.setInAnimation(inAnimation);
        holder.genreImage.setOutAnimation(outAnimation);

        Bitmap cachedBitmap = mCache.get("genre - " + genre.getGenre());
        if (cachedBitmap == null) {
//            if ((mImgHeight == 0 || mImgWidth == 0) && !mListenerTriggered) {
//                mListenerTriggered = true;
//                finalHolder.genreImage.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
//                    @Override
//                    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
//                        mImgHeight = bottom - top;
//                        mImgWidth = right - left;
//                        finalHolder.genreImage.removeOnLayoutChangeListener(this);
//                    }
//                });
//            }
            int width = mImgWidth;
            int height = mImgHeight;
            if (width == 0 || height == 0) {
                width = 400;
                height = 400;

            }
            GenreArtGenerator artLoader = new GenreArtGenerator(mContext)
                    .setGenre(genre)
                    .setArtDimensions(width, height)
                    .setTag(String.valueOf(finalHolder.position))
                    .setArtLoadedWatcher(new ArtLoadedListener() {
                        @Override
                        public void onArtLoaded(Drawable artwork, Object tag) {
                            if (artwork == null) {
                                finalHolder.genreImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.default_album_art));
                            } else {
                                if (String.valueOf(finalHolder.position).equals(tag)) {
                                    finalHolder.genreImage.setImageDrawable(artwork);
                                    mCache.put("genre - " + genre.getGenre(), ((BitmapDrawable) artwork).getBitmap());
                                }
                            }
                        }

                        @Override
                        public void onLoadProgressChanged(LoadProgress progress) {
                            //TODO
                        }
                    })
                    .setPalletGeneratedWatcher(new PaletteGeneratedWatcher() {
                        @Override
                        public void onPaletteGenerated(Palette palette, Object tag) {
                            if (palette != null && String.valueOf(finalHolder.position).equals(tag)) {
                                applyPalette(palette, String.valueOf(finalHolder.position), finalHolder);
                            }
                        }
                    })
                    .generateInBackground();
        } else {
            holder.genreImage.setImageDrawable(new BitmapDrawable(cachedBitmap));
            Palette.generateAsync(cachedBitmap, new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    applyPalette(palette, String.valueOf(finalHolder.position), finalHolder);
                }
            });
        }


        return convertView;
    }

    private void initImageSwitcher(ImageSwitcher imageSwitcher) {
        imageSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView iv = new ImageView(mContext);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setLayoutParams(new ImageSwitcher.LayoutParams(RelativeLayout.LayoutParams.
                        FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT));
                return iv;
            }
        });
        Animation in = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);
        Animation out = AnimationUtils.loadAnimation(mContext, R.anim.fade_out);
        imageSwitcher.setInAnimation(in);
        imageSwitcher.setOutAnimation(out);
        imageSwitcher.setImageDrawable(mContext.getResources().getDrawable(R.drawable.default_album_art));
    }

    private void applyPalette(Palette palette, String tag, GenreHolder holder) {
        if (String.valueOf(holder.position).equals(tag)) {
            int color = palette.getDarkVibrantColor(
                    palette.getDarkMutedColor(
                            palette.getMutedColor(
                                    mContext.getResources().getColor(R.color.color_primary)
                            )
                    )
            );
            holder.genreTextBox.setBackgroundColor(color);
        }
    }

    @Override
    public List<Genre> getFilteredList() {
        return mData;
    }

    private class GenreHolder {
        public ImageSwitcher genreImage;
        public TextView genreText;
        public TextView genreCountText;
        public View genreTextBox;
        public int position;
    }
}
