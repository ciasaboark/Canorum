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

package org.ciasaboark.canorum.receiver;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.provider.MediaControlsWidgetProvider;
import org.ciasaboark.canorum.service.MusicService;

/**
 * Created by Jonathan Nelson on 3/24/15.
 * Listens for broadcast messages from remote views (i.e. widgets and notifications)
 */
public class RemoteControlsReceiver extends BroadcastReceiver {
    public static final String TAG = "WidgetControlsListener";
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PREV = "ACTION_PREV";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_STOP = "ACTION_STOP";

    @Override
    public void onReceive(Context context, Intent intent) {
        MusicControllerSingleton musicControllerSingleton = MusicControllerSingleton.getInstanceNoCreate(context);
        boolean actionFromNotification = intent.getBooleanExtra(MusicService.KEY_ACTION_FROM_NOTIFICATION, false);

        switch (intent.getAction()) {
            case ACTION_PLAY:
                if (!musicControllerSingleton.isPlaying())
                    musicControllerSingleton.play();
                break;
            case ACTION_PAUSE:
                if (!musicControllerSingleton.isPaused())
                    musicControllerSingleton.pause(actionFromNotification);
                break;
            case ACTION_NEXT:
                if (musicControllerSingleton.hasNext())
                    musicControllerSingleton.playNext();
                break;
            case ACTION_PREV:
                if (musicControllerSingleton.hasPrev())
                    musicControllerSingleton.playPrev();
                break;
            case ACTION_STOP:
                musicControllerSingleton.stop();
                break;
        }

        updateAllWidgets(context);
        MusicControllerSingleton.getInstance(context).updateNotification();
    }

    public static void updateAllWidgets(Context context) {
        int[] widgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, MediaControlsWidgetProvider.class));
        Intent i = new Intent(context, MediaControlsWidgetProvider.class);
        i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        context.sendBroadcast(i);
    }
}
