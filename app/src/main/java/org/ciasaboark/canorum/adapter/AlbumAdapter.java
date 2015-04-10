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
import org.ciasaboark.canorum.artwork.album.AlbumArtLoader;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.playlist.provider.MergedProvider;
import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.song.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonathan Nelson on 2/5/15.
 */
public class AlbumAdapter extends ArrayAdapter<Album> implements FilterableAdapter<Album> {
    private static final String TAG = "AlbumAdapter";
    private final Context mContext;
    private List<Album> mData;
    private LruCache<String, Bitmap> mCache;

    private List<ImageSwitcher> mImageSwitchers = new ArrayList<ImageSwitcher>();

    public AlbumAdapter(Context ctx, List<Album> albums, LruCache<String, Bitmap> cache) {
        super(ctx, R.layout.artist_grid_single, albums);
        mContext = ctx;
        mData = albums;
        mCache = cache;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        NewHolder holder = null;
        final Album album = getItem(pos);

        if (convertView != null) {
            holder = (NewHolder) convertView.getTag();
        } else {
            holder = new NewHolder();
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.album_grid_single, null);
            holder.albumImage = (ImageSwitcher) convertView.findViewById(R.id.albumImage);
            holder.artistText = (TextView) convertView.findViewById(R.id.album_grid_artist_text);
            holder.albumText = (TextView) convertView.findViewById(R.id.album_grid_album_text);
            holder.menuButton = (ImageView) convertView.findViewById(R.id.album_grid_menu);
            holder.textBox = convertView.findViewById(R.id.album_grid_text_box);
            initImageSwitcher(holder.albumImage);
            convertView.setTag(holder);
        }

        final NewHolder finalHolder = holder;
        holder.position = pos;
        holder.artistText.setText(album.getArtist().getArtistName());
        holder.albumText.setText(album.getAlbumName());
        holder.textBox.setBackgroundColor(mContext.getResources().getColor(R.color.color_primary));
        holder.menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final MergedProvider provider = MergedProvider.getInstance(mContext);
                final List<Track> albumTracks = provider.getTracksForAlbum(album.getArtist().getArtistName(), album);
                final MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(mContext);
                PopupMenu popupMenu = new PopupMenu(mContext, finalHolder.menuButton);
                popupMenu.inflate(R.menu.library_long_click);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        boolean itemHandled = false;
                        switch (item.getItemId()) {
                            case R.id.popup_menu_library_add_queue:
                                Toast.makeText(mContext, "Added " + album + " to queue", Toast.LENGTH_SHORT).show();
                                musicControllerSingleton.addTracksToQueue(albumTracks);
                                itemHandled = true;
                                break;
                            case R.id.popup_menu_library_play_next:
                                Toast.makeText(mContext, "Playing " + album + " next", Toast.LENGTH_SHORT).show();
                                musicControllerSingleton.addTracksToQueueHead(albumTracks);
                                itemHandled = true;
                                break;
                            case R.id.popup_menu_library_play_now:
                                Toast.makeText(mContext, "Playing " + album, Toast.LENGTH_SHORT).show();
                                musicControllerSingleton.addTracksToQueueHead(albumTracks);
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
        Animation inAnimation = holder.albumImage.getInAnimation();
        Animation outAnimation = holder.albumImage.getOutAnimation();
        holder.albumImage.setInAnimation(null);
        holder.albumImage.setOutAnimation(null);
        holder.albumImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.default_album_art));
        holder.albumImage.setInAnimation(inAnimation);
        holder.albumImage.setOutAnimation(outAnimation);

        Bitmap cachedBitmap = mCache.get(album.toString());
        if (cachedBitmap == null) {
            AlbumArtLoader artLoader = new AlbumArtLoader(mContext)
                    .setAlbum(album)
                    .setArtSize(ArtSize.SMALL)
                    .setTag(String.valueOf(finalHolder.position))
                    .setArtLoadedWatcher(new ArtLoadedWatcher() {
                        @Override
                        public void onArtLoaded(Drawable artwork, Object tag) {
                            if (artwork != null) {
                                if (String.valueOf(finalHolder.position).equals(tag)) {
                                    finalHolder.albumImage.setImageDrawable(artwork);
                                    mCache.put(album.toString(), ((BitmapDrawable) artwork).getBitmap());
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
                        public void onPaletteGenerated(Palette palette) {
                            applyPalette(palette, String.valueOf(finalHolder.position), finalHolder);
                        }
                    })
                    .setInternetSearchEnabled(true)
                    .loadInBackground();
        } else {
            holder.albumImage.setImageDrawable(new BitmapDrawable(cachedBitmap));
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

    private void applyPalette(Palette palette, String tag, NewHolder holder) {
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
    public List<Album> getFilteredList() {
        return mData;
    }

    private class NewHolder {
        public ImageSwitcher albumImage;
        public TextView artistText;
        public TextView albumText;
        public View textBox;
        public ImageView menuButton;
        public int position;
    }
}
