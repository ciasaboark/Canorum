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

package org.ciasaboark.canorum.provider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.RemoteViews;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.activity.MainActivity;
import org.ciasaboark.canorum.artwork.ArtSize;
import org.ciasaboark.canorum.artwork.album.AlbumArtLoader;
import org.ciasaboark.canorum.artwork.watcher.ArtLoadedWatcher;
import org.ciasaboark.canorum.artwork.watcher.LoadProgress;
import org.ciasaboark.canorum.fragment.TOP_LEVEL_FRAGMENTS;
import org.ciasaboark.canorum.receiver.RemoteControlsReceiver;
import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.song.Track;

/**
 * Created by Jonathan Nelson on 3/24/15.
 */
public class MediaControlsWidgetProvider extends AppWidgetProvider {
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstanceNoCreate(context);
        if (musicControllerSingleton == null) {
            //the widget is updating before the app has had a chance to settle
            return;
        }

        final Track curTrack = musicControllerSingleton.getCurTrack();
        //update the widgets with no album art to begin with
        updateWidgets(context, appWidgetManager, appWidgetIds, null, curTrack);

        if (curTrack != null) {
            Album album = curTrack.getSong().getAlbum();
            AlbumArtLoader albumArtLoader = new AlbumArtLoader(context)
                    .setAlbum(album)
                    .setArtSize(ArtSize.SMALL)
                    .setInternetSearchEnabled(true)
                    .setProvideDefaultArtwork(true)
                    .setTag(curTrack)
                    .setArtLoadedWatcher(new ArtLoadedWatcher() {
                        @Override
                        public void onArtLoaded(Drawable artwork, Object tag) {
                            Track track = MusicControllerSingleton.getInstance(context).getCurTrack();
                            if (track != null && track.equals((Track) tag)) {
                                //update the widgets with the album art
                                updateWidgets(context, appWidgetManager, appWidgetIds, artwork, track);
                            }
                        }

                        @Override
                        public void onLoadProgressChanged(LoadProgress progress) {

                        }
                    })
                    .loadInBackground();
        }


    }

    private void updateWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, Drawable d, Track track) {
        //TODO
        int n = appWidgetIds.length;
        for (int i = 0; i < n; i++) {
            int appWidgetId = appWidgetIds[i];
            updateWidget(context, appWidgetManager, appWidgetId, d, track);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Drawable d, Track track) {
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

        RemoteViews views = getRemoteViews(context, minWidth, minHeight);


        // Create an Intent to launch ExampleActivity
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("STARTING FRAGMENT", TOP_LEVEL_FRAGMENTS.CUR_PLAYING);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId + 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.albumImage, pendingIntent);

        //attach intents to media controls
        Intent playIntent = new Intent();
        playIntent.setClass(context, RemoteControlsReceiver.class);
        playIntent.setAction(RemoteControlsReceiver.ACTION_PLAY);
        PendingIntent playPendIntent = PendingIntent.getBroadcast(context, appWidgetId + 1, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstance(context);

        Intent prevIntent = new Intent();
        prevIntent.setClass(context, RemoteControlsReceiver.class);
        prevIntent.setAction(RemoteControlsReceiver.ACTION_PREV);
        PendingIntent prevPendIntent = PendingIntent.getBroadcast(context, appWidgetId + 4, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (musicControllerSingleton.hasPrev()) {
            views.setImageViewResource(R.id.controls_button_media_prev, R.drawable.ic_skip_previous_white_24dp);
            views.setOnClickPendingIntent(R.id.controls_button_media_prev, prevPendIntent);
        } else {
            views.setImageViewResource(R.id.controls_button_media_prev, R.drawable.ic_skip_previous_grey600_24dp);
        }

        Intent nextIntent = new Intent();
        nextIntent.setClass(context, RemoteControlsReceiver.class);
        nextIntent.setAction(RemoteControlsReceiver.ACTION_NEXT);
        PendingIntent nextPendIntent = PendingIntent.getBroadcast(context, appWidgetId + 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (musicControllerSingleton.hasNext()) {
            views.setImageViewResource(R.id.controls_button_media_next, R.drawable.ic_skip_next_white_24dp);
            views.setOnClickPendingIntent(R.id.controls_button_media_next, nextPendIntent);
        } else {
            views.setImageViewResource(R.id.controls_button_media_next, R.drawable.ic_skip_next_grey600_24dp);
        }

        Intent pauseIntent = new Intent();
        pauseIntent.setClass(context, RemoteControlsReceiver.class);
        pauseIntent.setAction(RemoteControlsReceiver.ACTION_PAUSE);
        PendingIntent pausePendIntent = PendingIntent.getBroadcast(context, appWidgetId + 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (musicControllerSingleton.isPlaying()) {
            views.setOnClickPendingIntent(R.id.controls_button_media_play, pausePendIntent);
            views.setImageViewResource(R.id.controls_button_media_play, R.drawable.ic_pause_white_48dp);
        } else {
            views.setOnClickPendingIntent(R.id.controls_button_media_play, playPendIntent);
            views.setImageViewResource(R.id.controls_button_media_play, R.drawable.ic_play_white_48dp);
        }


        //set the artist and song text
        String artistText = track == null ? "" : track.getSong().getAlbum().getArtist().getArtistName();
        String songText = track == null ? "" : track.getSong().getTitle();
        views.setTextViewText(R.id.mini_song_artist, artistText);
        views.setTextViewText(R.id.mini_song_title, songText);

        if (d == null) {
            //use the default album image
            BitmapDrawable defaultAlbumArt = (BitmapDrawable) context.getResources().getDrawable(R.drawable.default_album_art);
            views.setImageViewBitmap(R.id.albumImage, defaultAlbumArt.getBitmap());
        } else {
            Bitmap bitmap = null;
            if (d instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) d).getBitmap();
            }
            if (bitmap != null) {
                views.setImageViewBitmap(R.id.albumImage, bitmap);
            }
        }

        // Tell the AppWidgetManager to perform an update on the current app widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        //TODO resize widget
        int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        onUpdate(context, appWidgetManager, new int[]{appWidgetId});
    }


    private RemoteViews getRemoteViews(Context context, int minWidth, int minHeight) {
        // First find out rows and columns based on width provided.
        int rows = getCellsForSize(minHeight);
        int columns = getCellsForSize(minWidth);

        int layout;
        if (columns >= 4 && rows == 1) {
            // Get 4 column widget remote view and return
            layout = R.layout.widget_media_controls_4x1;
        } else if (columns >= 4 && rows == 2) {
            layout = R.layout.widget_media_controls_4x2;
        } else if (columns >= 4 && rows >= 3) {
            layout = R.layout.widget_media_controls_4x3;
        } else if (columns == 3 && rows == 1) {
            layout = R.layout.widget_media_controls_3x1;
        } else {
            layout = R.layout.widget_media_controls_1x1;
        }

        return new RemoteViews(context.getPackageName(), layout);
    }

    /* Returns number of cells needed for given size of the widget.
    *
            * @param size Widget size in dp.
    * @return Size in number of cells.
    */
    private static int getCellsForSize(int size) {
        int n = 2;
        while (70 * n - 30 < size) {
            ++n;
        }
        return n - 1;
    }

}
