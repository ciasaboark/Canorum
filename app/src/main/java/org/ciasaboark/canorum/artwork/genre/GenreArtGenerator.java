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

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.support.v7.graphics.Palette;
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
    private static final int WIDTH_LARGE = 1000;
    private static final int HEIGHT_LARGE = 1000;
    private static final int WIDTH_SMALL = 300;
    private static final int HEIGHT_SMALL = 300;

    private final Context mContext;
    private ArtLoadedWatcher mWatcher;
    private Genre mGenre;
    private Integer mBitmapWidth;
    private Integer mBitmapHeight;
    private PaletteGeneratedWatcher mPalletGeneratedWatcher;
    private boolean mIsInternetSearchEnabled = false;
    private Object mTag;


    public GenreArtGenerator(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }

        mContext = ctx;
    }

    public GenreArtGenerator setArtDimensions(int width, int height) {
        mBitmapWidth = width;
        mBitmapHeight = height;
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

        if (mBitmapWidth == null || mBitmapHeight == null) {
            Log.e(TAG, "will not generate genre image without image dimenions given");
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
                        .setSize(ArtSize.LARGE)
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
                        .setSize(ArtSize.LARGE)
                        .setArtist(artist);
                try {
                    BitmapDrawable bitmap = fileSystemFetcher.loadArtwork();
                    artworkBitmaps.add(bitmap.getBitmap());
                } catch (ArtworkNotFoundException e) {
                    //not a big deal
                }
            }

            //send back some default artwork if we still dont have enough bitmaps to work with
            if (artworkBitmaps.size() == 0) {
                result = generateBlankImage();
            } else if (artworkBitmaps.size() == 1) {
                result = new BitmapDrawable(artworkBitmaps.get(0));
            } else if (artworkBitmaps.size() == 2) {
                result = generateTupleBitmap(artworkBitmaps);
            } else if (artworkBitmaps.size() == 3) {
                result = generateTrippleBitmap(artworkBitmaps);
            } else if (artworkBitmaps.size() == 4) {
                result = generateMozaic(artworkBitmaps);
            } else {
                result = generateBlankImage();
            }

            return result;
        }

        @Override
        protected void onPostExecute(BitmapDrawable drawable) {
            super.onPostExecute(drawable);
            sendBackBitmap(drawable);
            generatePalette(drawable);

        }

        private void sendBackBitmap(final BitmapDrawable bitmapDrawable) {
            if (mContext instanceof Activity) {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mWatcher.onArtLoaded(bitmapDrawable, mTag);
                    }
                });
            } else {
                mWatcher.onArtLoaded(bitmapDrawable, mTag);
            }
        }

        private void generatePalette(BitmapDrawable bitmapDrawable) {
            if (mPalletGeneratedWatcher != null && bitmapDrawable != null) {
                Palette.generateAsync(bitmapDrawable.getBitmap(), new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(final Palette palette) {
                        if (mContext instanceof Activity) {
                            ((Activity) mContext).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mPalletGeneratedWatcher.onPaletteGenerated(palette, mTag);
                                }
                            });
                        } else {
                            mPalletGeneratedWatcher.onPaletteGenerated(palette, mTag);
                        }
                    }
                });
            }
        }

        private BitmapDrawable generateTupleBitmap(List<Bitmap> bitmapList) {
            Bitmap canvasBitmap = getBitmap();
            Canvas canvas = new Canvas(canvasBitmap);
            Paint paint = new Paint();
            paint.setAntiAlias(false);
            paint.setFilterBitmap(false);
            paint.setDither(false);

            Paint shadowPaint = new Paint();
            shadowPaint.setShadowLayer(15.0f, 0, 0, 0xff000000);
            int yOffset = canvasBitmap.getWidth() / 2;
            for (int i = 0; i < 2; i++) {
                Bitmap bitmap = bitmapList.get(i);
                bitmap = scaleBitmapToHeightWithMinWidth(bitmap, canvasBitmap.getHeight(), yOffset);
                //draw a shadow under this layer
                canvas.drawRect(yOffset * i, 0, canvasBitmap.getWidth(), canvasBitmap.getHeight(), shadowPaint);
                canvas.drawBitmap(bitmap, yOffset * i, 0, paint);
            }

            return new BitmapDrawable(canvasBitmap);
        }

        private Bitmap scaleBitmapToHeightWithMinWidth(Bitmap bitmap, int newHeight, int minWidth) {
            //scale the height first, then scale to a minimum width
            bitmap = cropBitmapToSquare(bitmap);
            int imgHeight = bitmap.getHeight();
            int imgWidth = bitmap.getWidth();
            float scaleFactor = ((float) newHeight) / imgHeight;
            int newWidth = (int) (imgWidth * scaleFactor);
            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            bitmap = scaleBitmapToMinWidth(bitmap, minWidth);
            return bitmap;
        }

        private Bitmap scaleBitmapToMinWidth(Bitmap bitmap, int minWidth) {
            Bitmap scaledBitmap = bitmap;
            int imgWidth = bitmap.getWidth();
            if (imgWidth < minWidth) {
                float scaleFactor = ((float) minWidth) / imgWidth;
                int imgHeight = bitmap.getHeight();
                int newHeight = (int) (imgHeight * scaleFactor);
                scaledBitmap = Bitmap.createScaledBitmap(bitmap, minWidth, newHeight, true);
            }
            return scaledBitmap;
        }

        private BitmapDrawable generateTrippleBitmap(List<Bitmap> bitmapList) {
            Bitmap canvasBitmap = getBitmap();
            Canvas canvas = new Canvas(canvasBitmap);
            Paint paint = new Paint();
            paint.setAntiAlias(false);
            paint.setFilterBitmap(false);
            paint.setDither(false);
            float yOffset = canvasBitmap.getWidth() / 3;

            Paint shadowPaint = new Paint();
            shadowPaint.setShadowLayer(15.0f, 0, 0, 0xff000000);
            for (int i = 0; i < 3; i++) {
                Bitmap bitmap = bitmapList.get(i);
                bitmap = scaleBitmapToHeightWithMinWidth(bitmap, canvasBitmap.getHeight(), (int) yOffset);
                //draw a shadow under this layer
                canvas.drawRect(yOffset * i, 0, canvasBitmap.getWidth(), canvasBitmap.getHeight(), shadowPaint);
                canvas.drawBitmap(bitmap, yOffset * i, 0, paint);
            }

            return new BitmapDrawable(canvasBitmap);
        }

        private BitmapDrawable generateBlankImage() {
            return null;
        }

        private BitmapDrawable generateMozaic(List<Bitmap> bitmapList) {
            Bitmap canvasBitmap = getBitmap();
            Canvas canvas = new Canvas(canvasBitmap);
            Paint paint = new Paint();
            paint.setAntiAlias(false);
            paint.setFilterBitmap(false);
            paint.setDither(false);

            Paint shadowPaint = new Paint();
            shadowPaint.setShadowLayer(15.0f, 0, 0, 0xff000000);

            for (int i = 0; i < 4; i++) {
                Bitmap bitmap = bitmapList.get(i);
                bitmap = resizeToQuarter(bitmap);

                float x = 0;
                float y = 0;

                //place the bitmaps on the canvas in the order:
                //  0 | 3
                //  --|--
                //  2 | 1
                boolean drawShadow = false;
                switch (i) {
                    case 3:
                        x = canvasBitmap.getWidth() / 2;
                        y = 0;
                        drawShadow = true;
                        break;
                    case 2:
                        x = 0;
                        y = canvasBitmap.getHeight() / 2;
                        drawShadow = true;
                        break;
                    case 1:
                        x = canvasBitmap.getWidth() / 2;
                        y = canvasBitmap.getHeight() / 2;
                        break;
                }

                if (drawShadow) {
                    float shadowWidth = canvasBitmap.getWidth() / 2;
                    float shadowHeight = canvasBitmap.getHeight() / 2;
                    float left = x;
                    float top = y;
                    float right = x + shadowWidth;
                    float bottom = y + shadowWidth;
                    canvas.drawRect(left, top, right, bottom, shadowPaint);
                }

                canvas.drawBitmap(bitmap, x, y, paint);
                bitmap = null;
            }

            BitmapDrawable mozaic = new BitmapDrawable(canvasBitmap);

            return mozaic;
        }

        private Bitmap getBitmap() {
            Bitmap.Config config = Bitmap.Config.ARGB_8888;
            return Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, config);
        }

        /**
         * Resize the given bitmap so that it will fit within one quarter of the canvas
         *
         * @param bitmap the bitmap to resize.
         * @return
         */
        private Bitmap resizeToQuarter(Bitmap bitmap) {
            Bitmap scaledBitmap = bitmap;
            //the given bitmap might not be square,
            scaledBitmap = cropBitmapToSquare(bitmap);
            int height = bitmap.getHeight();
            int width = bitmap.getWidth();
            int newHeight = mBitmapHeight / 2;
            int newWidth = mBitmapWidth / 2;
            float scaleWidth = ((float) newWidth) / width;
            float scaleHeight = ((float) newHeight) / height;

            scaledBitmap = Bitmap.createScaledBitmap(bitmap, newHeight, newWidth, true);
            return scaledBitmap;
        }

        private Bitmap cropBitmapToSquare(Bitmap bitmap) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int lowestDimension = width < height ? width : height;
            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, lowestDimension, lowestDimension);
            return croppedBitmap;
        }
    }
}
