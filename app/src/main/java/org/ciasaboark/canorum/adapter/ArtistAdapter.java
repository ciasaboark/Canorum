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
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.artist.ArtistArtLoader;
import org.ciasaboark.canorum.artwork.cache.ArtworkLruCache;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedListener;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.playlist.provider.MergedProvider;
import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.song.Artist;
import org.ciasaboark.canorum.song.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonathan Nelson on 2/5/15.
 */
public class ArtistAdapter extends ArrayAdapter<Artist> implements FilterableAdapter<Artist> {
    private static final String TAG = "ArtistAdapter";
    private final Context mContext;
    private List<Artist> mData;
    private ArtworkLruCache mCache;

    private List<ImageSwitcher> mImageSwitchers = new ArrayList<ImageSwitcher>();

    public ArtistAdapter(Context ctx, List<Artist> data) {
        super(ctx, R.layout.grid_artist_single, data);
        mContext = ctx;
        mData = data;
        mCache = ArtworkLruCache.getInstance();
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        ArtistHolder holder = null;
        final Artist artist = getItem(pos);

        if (convertView != null) {
            holder = (ArtistHolder) convertView.getTag();
        } else {
            holder = new ArtistHolder();
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.grid_artist_single, null);
            holder.artistImage = (ImageSwitcher) convertView.findViewById(R.id.artistImage);
            holder.artistText = (TextView) convertView.findViewById(R.id.artist_grid_artist_text);
            holder.albumText = (TextView) convertView.findViewById(R.id.artist_grid_album_text);
            holder.menuButton = (ImageView) convertView.findViewById(R.id.artist_grid_menu);
            holder.textBox = convertView.findViewById(R.id.artist_grid_text_box);
            initImageSwitcher(holder.artistImage);
            convertView.setTag(holder);
        }

        final ArtistHolder finalHolder = holder;
        holder.position = pos;
        holder.artistText.setText(artist.getArtistName());
        final MergedProvider provider = MergedProvider.getInstance(mContext);
        List<Album> artistAlbums = provider.getAlbumsForArtist(artist);
        String albumText = artistAlbums.size() + (artistAlbums.size() > 1 ? " albums" : " album");
        holder.albumText.setText(albumText);
        holder.textBox.setBackgroundColor(mContext.getResources().getColor(R.color.color_primary));
        holder.menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final List<Track> artistTracks = provider.getTracksForArtist(artist);
                final MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(mContext);
                PopupMenu popupMenu = new PopupMenu(mContext, finalHolder.menuButton);
                popupMenu.inflate(R.menu.library_long_click);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        boolean itemHandled = false;
                        switch (item.getItemId()) {
                            case R.id.popup_menu_library_add_queue:
                                Toast.makeText(mContext, "Added " + artist + " to queue", Toast.LENGTH_SHORT).show();
                                musicControllerSingleton.addTracksToQueue(artistTracks);
                                itemHandled = true;
                                break;
                            case R.id.popup_menu_library_play_next:
                                Toast.makeText(mContext, "Playing " + artist + " next", Toast.LENGTH_SHORT).show();
                                musicControllerSingleton.addTracksToQueueHead(artistTracks);
                                itemHandled = true;
                                break;
                            case R.id.popup_menu_library_play_now:
                                Toast.makeText(mContext, "Playing " + artist, Toast.LENGTH_SHORT).show();
                                musicControllerSingleton.addTracksToQueueHead(artistTracks);
                                musicControllerSingleton.playNext();
                                itemHandled = true;
                                break;
                        }
                        return itemHandled;
                    }
                });
                popupMenu.show();
            }
        });

        //temporarily disable the imageswitcher animations so we can apply the default album art with
        //no fanfare
        Animation inAnimation = holder.artistImage.getInAnimation();
        Animation outAnimation = holder.artistImage.getOutAnimation();
        holder.artistImage.setInAnimation(null);
        holder.artistImage.setOutAnimation(null);
        holder.artistImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.default_album_art)); //TODO get default artist artwork from somewhere
        holder.artistImage.setInAnimation(inAnimation);
        holder.artistImage.setOutAnimation(outAnimation);


        Bitmap cachedBitmap = mCache.get("artist - " + artist.toString());
        if (cachedBitmap == null) {
            ArtistArtLoader artLoader = new ArtistArtLoader(mContext)
                    .setArtist(artist)
                    .setArtSize(ArtSize.SMALL)
                    .setTag(String.valueOf(finalHolder.position))
                    .setInternetSearchEnabled(true)
                    .setArtLoadedWatcher(new ArtLoadedListener() {
                        @Override
                        public void onArtLoaded(Drawable artwork, Object tag) {
                            if (artwork != null) {
                                if (String.valueOf(finalHolder.position).equals(tag)) {
                                    finalHolder.artistImage.setImageDrawable(artwork);
                                    mCache.put("artist - " + artist.toString(), ((BitmapDrawable) artwork).getBitmap());
                                }
                            }
                        }

                        @Override
                        public void onLoadProgressChanged(LoadProgress progress) {
                            //TODO
                        }
                    })
                    .setPaletteGeneratedWatcher(new PaletteGeneratedWatcher() {
                        @Override
                        public void onPaletteGenerated(Palette palette, Object tag) {
                            if (palette != null && String.valueOf(finalHolder.position).equals(tag)) {
                                applyPalette(palette, String.valueOf(finalHolder.position), finalHolder);
                            }
                        }
                    })
                    .loadInBackground();
        } else {
            finalHolder.artistImage.setImageDrawable(new BitmapDrawable(cachedBitmap));
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
        mImageSwitchers.add(imageSwitcher);
    }

    private void applyPalette(Palette palette, String tag, ArtistHolder holder) {
        if (String.valueOf(holder.position).equals(tag)) {
            int color = palette.getDarkVibrantColor(
                    palette.getDarkMutedColor(
                            palette.getMutedColor(
                                    mContext.getResources().getColor(R.color.color_primary)
                            )
                    )
            );
            holder.textBox.setBackgroundColor(color);
        }
    }

    @Override
    public List<Artist> getFilteredList() {
        return mData;
    }

    private class ArtistHolder {
        public ImageSwitcher artistImage;
        public TextView artistText;
        public TextView albumText;
        public ImageView menuButton;
        public View textBox;
        public int position;
    }
}
