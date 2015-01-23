package org.ciasaboark.canorum;

import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.app.Notification;
import android.app.PendingIntent;

import org.ciasaboark.canorum.view.MusicController;

import java.util.ArrayList;

/**
 * Created by Jonathan Nelson on 1/16/15.
 */
public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private static final String TAG = "MediaService";
    private static final int NOTIFY_ID = 1;
    private boolean mPreparing = true;
    private Song mCurSong;

    private MediaPlayer player;
    private ArrayList<Song> songs;
    private int songPos;
    private final IBinder musicBind = new MusicBinder();
    private String songTitle = "";

    @Override
    public void onCreate() {
        super.onCreate();
        songPos = 0;
        player = new MediaPlayer();
        initMusicPlayer();

    }

    public void initMusicPlayer() {
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnErrorListener(this);
        player.setOnCompletionListener(this);
    }

    @Override
    public IBinder onBind(Intent i) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent i) {
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(player.getCurrentPosition() > 0){
            mp.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mPreparing = false;
        mp.start();
        Intent playIntent = new Intent(MusicControllerSingleton.ACTION_PLAY);
        playIntent.putExtra("curSong", mCurSong);
        LocalBroadcastManager.getInstance(this).sendBroadcast(playIntent);
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.android_music_player_play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
        .setContentText(songTitle);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);
    }

    public void setList(ArrayList<Song> songs) {
        this.songs = songs;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void playSong() {
        mPreparing = true;
        player.reset();
        Song playSong = songs.get(songPos);
        mCurSong = playSong;
        songTitle = playSong.getTitle();
        long currSong = playSong.getId();
        Uri trackUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);
        try {
            player.setDataSource(getApplicationContext(), trackUri);

        } catch (Exception e) {
            Log.e(TAG, "Error setting player data source: " + e);
        }
        player.prepareAsync();
    }

    public Song getCurSong() {
        //if we are still preparing the song to be played then we cant trust that it will load
        //correctly
        return mPreparing ? null : mCurSong;
    }

    public void setSong(int songIndex) {
        songPos = songIndex;
    }

    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return player.getDuration();
    }

    public boolean isPlaying(){
        return player.isPlaying();
    }

    public void pausePlayer(){
        player.pause();
    }

    public void seek(int posn){
        player.seekTo(posn);
    }

    public void go(){
        player.start();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("playing"));
    }

    public void playPrev(){
        Log.d(TAG, "playPrev()");
        songPos--;
        if (songPos < 0) {
            songPos = songs.size() - 1;
        }
        playSong();
    }

    public void playNext(){
        Log.d(TAG, "playNext()");
        songPos++;
        if(songPos >= songs.size()) {
            songPos = 0;
        }
        playSong();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }
}
