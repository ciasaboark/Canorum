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

package org.ciasaboark.canorum.artwork.genre;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.Log;

import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.album.fetcher.FileSystemFetcher;
import org.ciasaboark.canorum.artwork.artist.fetcher.FileSystemArtistFetcher;
import org.ciasaboark.canorum.artwork.exception.ArtworkNotFoundException;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.PaletteGeneratedWatcher;
import org.ciasaboark.canorum.playlist.provider.MergedProvider;
import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.song.Artist;
import org.ciasaboark.canorum.song.Genre;
import org.ciasaboark.canorum.song.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Jonathan Nelson on 4/10/15.
 * Generates an image to represent a music genre.
 * Idealy the genre image should be composed of 4 album covers in the form of a mozaic:
 * <p/>
 * /-----------------------------------\
 * |                  |                |
 * |                  |                |
 * |                  |                |
 * |                  |                |
 * |                  |                |
 * |                  |                |
 * |------------------|----------------|
 * |                  |                |
 * |                  |                |
 * |                  |                |
 * |                  |                |
 * |                  |                |
 * |                  |                |
 * \------------------|----------------/
 * <p/>
 * If the selected Genre does not have 4 seperate albums then an artist image can be
 * substituted.  If there are not enough album covers and artist images for all four
 * corners then a single album image will be used, or a blank colored image if there
 * are no albums at all.
 */
public class GenreArtGenerator {
    private static final String TAG = "GenreArtLoader";
    private final Context mContext;
    private ArtLoadedWatcher mWatcher;
    private Genre mGenre;
    private ArtSize mArtSize = null;
    private PaletteGeneratedWatcher mPalletGeneratedWatcher;
    private boolean mIsInternetSearchEnabled = false;
    private Object mTag;


    public GenreArtGenerator(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }

        mContext = ctx;
    }

    public GenreArtGenerator setArtSize(ArtSize artSize) {
        mArtSize = artSize;
        return this;
    }

    public GenreArtGenerator setTag(Object tag) {
        mTag = tag;
        return this;
    }

    public GenreArtGenerator setGenre(Genre genre) {
        mGenre = genre;
        return this;
    }

    public GenreArtGenerator setArtLoadedWatcher(ArtLoadedWatcher watcher) {
        mWatcher = watcher;
        return this;
    }

    public GenreArtGenerator setPalletGeneratedWatcher(PaletteGeneratedWatcher watcher) {
        mPalletGeneratedWatcher = watcher;
        return this;
    }

    public GenreArtGenerator generateInBackground() {
        if (mGenre == null) {
            Log.e(TAG, "will not generate genre image with no Genre given");
        }
        if (mWatcher == null) {
            Log.e(TAG, "will not generate genre image with no ArtLoadedWatcher given");
        }

        if (mArtSize == null) {
            mArtSize = ArtSize.LARGE;
        }

        BackgroundGenerator generator = new BackgroundGenerator();
        generator.execute(mGenre);

        return this;
    }

    private class BackgroundGenerator extends AsyncTask<Genre, Void, BitmapDrawable> {
        @Override
        protected BitmapDrawable doInBackground(Genre... params) {
            BitmapDrawable result = null;
            Genre genre = params[0];
            MergedProvider provider = MergedProvider.getInstance(mContext);
            List<Track> genreTracks = provider.getTracksForGenre(genre);
            List<Album> genreAlbums = new ArrayList<>();
            List<Artist> genreArtists = new ArrayList<>();
            for (Track track : genreTracks) {
                Album album = track.getSong().getAlbum();
                Artist artist = album.getArtist();

                if (!genreAlbums.contains(album)) {
                    genreAlbums.add(album);
                }
                if (!genreArtists.contains(artist)) {
                    genreArtists.add(artist);
                }
            }

            //if we have less than 4 albums to work with then just send back a blank image
            if (genreAlbums.size() < 4) {
                return generateBlankImage();
            } else {
                //Since the genre grid view will be loaded at the same time as the artist and
                // album grid view we will avoid loading our images from the network, and will
                // instead directly use the album artwork FileSystemFetcher until we have four
                // images to work with, or we run out of albums
                List<Bitmap> artworkBitmaps = new ArrayList<>();
                Random random = new Random();
                while (!genreAlbums.isEmpty() && artworkBitmaps.size() < 4) {
                    //pull a random index
                    int randomIndex = random.nextInt(genreAlbums.size());
                    Album album = genreAlbums.remove(randomIndex);

                    FileSystemFetcher fileSystemFetcher = new FileSystemFetcher(mContext)
                            .setSize(mArtSize)
                            .setAlbum(album);
                    try {
                        BitmapDrawable bitmap = fileSystemFetcher.loadArtwork();
                        artworkBitmaps.add(bitmap.getBitmap());
                    } catch (ArtworkNotFoundException e) {
                        //not a big deal
                    }
                }

                //if we still don't have 4 bitmaps to work with then we can try a similar approach
                // with the artist promo images
                while (!genreArtists.isEmpty() && artworkBitmaps.size() < 4) {
                    //pull a random index
                    int randomIndex = random.nextInt(genreArtists.size());
                    Artist artist = genreArtists.remove(randomIndex);

                    FileSystemArtistFetcher fileSystemFetcher = new FileSystemArtistFetcher(mContext)
                            .setSize(mArtSize)
                            .setArtist(artist);
                    try {
                        BitmapDrawable bitmap = fileSystemFetcher.loadArtwork();
                        artworkBitmaps.add(bitmap.getBitmap());
                    } catch (ArtworkNotFoundException e) {
                        //not a big deal
                    }
                }

                //send back some default artwork if we still dont have enough bitmaps to work with
                if (artworkBitmaps.size() < 4) {
                    result = generateBlankImage();
                } else {
                    result = generateMozaic(artworkBitmaps);
                }

            }

            return result;
        }

        private BitmapDrawable generateBlankImage() {
            Bitmap bitmap = getBitmap();
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(mContext.getResources().getColor(android.R.color.holo_blue_bright));   //TODO settle on an image color
            BitmapDrawable blankImage = new BitmapDrawable(bitmap);
            return blankImage;
        }

        private BitmapDrawable generateMozaic(List<Bitmap> bitmapList) {
            Bitmap canvasBitmap = getBitmap();
            //resize the first 4 bitmaps
            Bitmap[] bitmaps = new Bitmap[4];
            for (int i = 0; i < 4; i++) {
                bitmaps[i] = bitmapList.get(i);
            }

            bitmapList = null;
            Canvas canvas = new Canvas(canvasBitmap);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setDither(true);
            for (int i = 0; i < 4; i++) {
                Bitmap bitmap = bitmaps[i];
                float x = 0;
                float y = 0;

                //place the bitmaps on the canvas in the order:
                //  0 | 1
                //  --|--
                //  2 | 3
                switch (i) {
                    case 1:
                        x = canvasBitmap.getWidth() / 2;
                        y = 0;
                        break;
                    case 2:
                        x = 0;
                        y = canvasBitmap.getHeight() / 2;
                        break;
                    case 3:
                        x = canvasBitmap.getWidth() / 2;
                        y = canvasBitmap.getHeight() / 2;
                        break;
                }

                canvas.drawBitmap(bitmap, x, y, paint);
            }

            BitmapDrawable mozaic = new BitmapDrawable(canvasBitmap);

            return mozaic;
        }

        private Bitmap getBitmap() {
            int height = mArtSize == ArtSize.SMALL ? 100 : 400;
            int width = mArtSize == ArtSize.SMALL ? 100 : 400;
            Bitmap.Config config = Bitmap.Config.ARGB_8888;
            return Bitmap.createBitmap(width, height, config);
        }

        @Override
        protected void onPostExecute(BitmapDrawable drawable) {
            super.onPostExecute(drawable);
            mWatcher.onArtLoaded(drawable, mTag);
        }

        private Bitmap resizeToQuarter(Bitmap bitmap) {
            int height = (mArtSize == ArtSize.SMALL ? 100 : 400) / 4;
            int width = (mArtSize == ArtSize.SMALL ? 100 : 400) / 4;
            return Bitmap.createScaledBitmap(bitmap, width, height, true);
        }
    }
}
