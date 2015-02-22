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
import android.graphics.drawable.Drawable;
import android.support.v4.util.LruCache;
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
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.artist.ArtistArtLoader;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.song.Artist;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonathan Nelson on 2/5/15.
 */
public class ArtistAdapter extends ArrayAdapter<Artist> implements FilterableAdapter<Artist> {
    private static final String TAG = "ArtistAdapter";
    private final Context mContext;
    private List<Artist> mData;
    private LruCache<String, Bitmap> mCache;

    private List<ImageSwitcher> mImageSwitchers = new ArrayList<ImageSwitcher>();

    public ArtistAdapter(Context ctx, List<Artist> data, LruCache<String, Bitmap> cache) {
        super(ctx, R.layout.artist_grid_single, data);
        mContext = ctx;
        mData = data;
        mCache = cache;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        NewHolder holder = null;
        final Artist artist = getItem(pos);

        if (convertView != null) {
            holder = (NewHolder) convertView.getTag();
        } else {
            holder = new NewHolder();
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.artist_grid_single, null);
            holder.artistImage = (ImageSwitcher) convertView.findViewById(R.id.artistImage);
            holder.artistText = (TextView) convertView.findViewById(R.id.artist_grid_text);
            initImageSwitcher(holder.artistImage);
            convertView.setTag(holder);
        }

        holder.position = pos;
        holder.artistText.setText(artist.getArtistName());
        //temporarily disable the imageswitcher animations so we can apply the default album art with
        //no fanfare
        Animation inAnimation = holder.artistImage.getInAnimation();
        Animation outAnimation = holder.artistImage.getOutAnimation();
        holder.artistImage.setInAnimation(null);
        holder.artistImage.setOutAnimation(null);
        holder.artistImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.default_album_art)); //TODO get default artist artwork from somewhere
        holder.artistImage.setInAnimation(inAnimation);
        holder.artistImage.setOutAnimation(outAnimation);
        final NewHolder finalHolder = holder;

        Bitmap cachedBitmap = mCache.get(artist.toString());
        if (cachedBitmap == null) {
            ArtistArtLoader artLoader = new ArtistArtLoader(mContext)
                    .setArtist(artist)
                    .setArtSize(ArtSize.SMALL)
                    .setTag(String.valueOf(finalHolder.position))
                    .setArtLoadedWatcher(new ArtLoadedWatcher() {
                        @Override
                        public void onArtLoaded(Drawable artwork, String tag) {
                            if (artwork != null) {
                                if (String.valueOf(finalHolder.position).equals(tag)) {
                                    finalHolder.artistImage.setImageDrawable(artwork);
                                    mCache.put(artist.toString(), ((BitmapDrawable) artwork).getBitmap());
                                }
                            }
                        }

                        @Override
                        public void onLoadProgressChanged(LoadProgress progress) {
                            //TODO
                        }
                    })
                    .loadInBackground();
        } else {
            finalHolder.artistImage.setImageDrawable(new BitmapDrawable(cachedBitmap));
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
        mImageSwitchers.add(imageSwitcher);
    }

    @Override
    public List<Artist> getFilteredList() {
        return mData;
    }

    private class NewHolder {
        public ImageSwitcher artistImage;
        public TextView artistText;
        public int position;
    }
}
