package org.ciasaboark.canorum;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.ciasaboark.canorum.view.MusicController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Jonathan Nelson on 1/22/15.
 */
public class MusicControllerSingleton implements MusicController.SimpleMediaPlayerControl {
    private static final String TAG = "MusicControllerSingleton";

    //attached to the local broadcast notifications
    public static final String ACTION_PLAY  = "ACTION_PLAY";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_NEXT  = "ACTION_NEXT";
    public static final String ACTION_PREV  = "ACTION_PREV";
    public static final String ACTION_SEEK  = "ACTION_SEEK";

    private static  MusicControllerSingleton instance;
    private static Context context;
    private static MusicService musicSrv;
    private static boolean musicBound = false;
    private static ArrayList<Song> songList;
    private static boolean paused = false;
    private static boolean playbackPaused = false;
    private Intent playIntent;

    public static MusicControllerSingleton getInstance(Context c) {
        if (c == null) {
            throw new IllegalArgumentException("Context can not be null");
        }
        context = c;
        if (instance == null) {
            instance = new MusicControllerSingleton();
        }
        return instance;
    }

    public Song getCurSong() {
        return musicSrv == null ? null : musicSrv.getCurSong();
    }

    private MusicControllerSingleton() {
        //Singleton pattern
        songList = new ArrayList<Song>();
        if (playIntent == null) {
            playIntent = new Intent(context, MusicService.class);
            context.bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            context.startService(playIntent);
        }
    }

    public int getSongListSize() {
        return songList.size();
    }


    @Override
    public void play() {
        musicSrv.go();
        //if we send an ACTION_PLAY notification to the controller views here then it may arrive
        //before the music has started to play, so we let the service send the broadcast message
        //from onPrepared()
    }

    private void sendNotification(String action) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(action));
    }

    @Override
    public void pause() {
        Log.d(TAG, "pause()");
        playbackPaused = true;
        musicSrv.pausePlayer();
        sendNotification(ACTION_PAUSE);
    }

    public boolean isPaused() {
        return playbackPaused;
    }

    @Override
    public boolean hasPrev() {
        //TODO
        return true;
    }

    @Override
    public int getDuration() {
        int duration = 0;
        if (musicSrv != null && musicBound) {
            //music service will return -1 if no duration is available
            int dur = musicSrv.getDur();
            if (dur != -1) {
                duration = dur;
            }
        }
        return duration;
    }

    @Override
    public int getCurrentPosition() {
        int pos = 0;
        if (musicSrv != null && musicBound) {
            pos =  musicSrv.getPosn();
        }
        return pos;
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
        sendNotification(ACTION_SEEK);
    }

    @Override
    public boolean isPlaying() {
        boolean isPlaying = false;
        if (musicSrv != null && musicBound) {
            isPlaying = musicSrv.isPlaying();
        }

        return isPlaying;
    }

    @Override
    public MusicController.RepeatMode getRepeatMode() {
        return null;
    }

    @Override
    public MusicController.ShuffleMode getShuffleMode() {
        return null;
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public boolean hasNext() {
        return true; //TODO
    }

    @Override
    public void playPrev() {
        Log.d(TAG, "playPrev()");
        musicSrv.playPrev();
        if(playbackPaused){
            playbackPaused = false;
        }
        sendNotification(ACTION_PREV);
    }

    @Override
    public void playNext() {
        Log.d(TAG, "playNext()");
        musicSrv.playNext();
        if(playbackPaused){
            playbackPaused = false;
        }
        sendNotification(ACTION_NEXT);
    }

    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    public void getSongList() {
        ContentResolver musicResolver = context.getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int yearColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.YEAR);
            int albumIdColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

            do {
                long songId = musicCursor.getLong(idColumn);
                String songTitle = musicCursor.getString(titleColumn);
                String songArtist = musicCursor.getString(artistColumn);
                String songAlbum = musicCursor.getString(albumColumn);
                long albumId = musicCursor.getLong(albumIdColumn);
                Song song = new Song (songId, songTitle, songArtist, songAlbum, albumId);
                songList.add(song);
            } while (musicCursor.moveToNext());
        }

        //sort the list by title
        Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song lhs, Song rhs) {
                return lhs.getTitle().compareTo(rhs.getTitle());
            }
        });
    }

    public void stop() {
        context.stopService(playIntent);
        musicSrv = null;
    }
}
