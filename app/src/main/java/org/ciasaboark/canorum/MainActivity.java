package org.ciasaboark.canorum;

import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.MediaController.MediaPlayerControl;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import org.ciasaboark.canorum.MusicService.MusicBinder;


public class MainActivity extends ActionBarActivity implements MusicController.SimpleMediaPlayerControl {
    private static final String TAG = "MainActivity";
    private ArrayList<Song> songList;
    private ListView songView;
    private TextView errorView;
    private MusicController controller;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;
    private boolean paused=false, playbackPaused=false;

    //drawer layout
    private String[] drawerList = {"one", "two", "settings"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songView = (ListView) findViewById(R.id.song_list);
        errorView = (TextView) findViewById(R.id.error_no_music);

        songList = new ArrayList<Song>();
        getSongList();
        Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song lhs, Song rhs) {
                return lhs.getTitle().compareTo(rhs.getTitle());
            }
        });

        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);
        setupController();

    }

    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
//        controller.show(0);

    }

    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder)service;
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

    @Override
    public void onResume() {
        super.onResume();
        if (songList.isEmpty()) {
            songView.setVisibility(View.GONE);
            errorView.setVisibility(View.VISIBLE);
        } else {
            songView.setVisibility(View.VISIBLE);
            errorView.setVisibility(View.GONE);
        }

        if (paused) {
            setupController();
            paused = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(playIntent);
        musicSrv = null;
    }

    @Override
    protected void onPause(){
        super.onPause();
        paused = true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_shuffle:
                //TODO
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv = null;
                finish();
                break;
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
            case R.id.action_cur:
                Intent j = new Intent(this, CurrentPlayingActivity.class);
                startActivity(j);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void getSongList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            do {
                long songId = musicCursor.getLong(idColumn);
                String songTitle = musicCursor.getString(titleColumn);
                String songArtist = musicCursor.getString(artistColumn);
                Song song = new Song (songId, songTitle, songArtist);
                songList.add(song);
            } while (musicCursor.moveToNext());
        }
    }

    @Override
    public void play() {
        musicSrv.go();
        controller.updateWidgets();
    }

    @Override
    public void pause() {
        Log.d(TAG, "pause()");
        playbackPaused = true;
        musicSrv.pausePlayer();
        controller.updateWidgets();
    }

    @Override
    public boolean hasPrev() {
        //TODO
        return true;
    }

    @Override
    public int getDuration() {
        int duration = 0;
        if (musicSrv != null && musicBound && musicSrv.isPlaying()) {
            duration =  musicSrv.getDur();
        }
        return duration;
    }

    @Override
    public int getCurrentPosition() {
        int pos = 0;
        if (musicSrv != null && musicBound && musicSrv.isPlaying()) {
            pos =  musicSrv.getPosn();
        }
        return pos;
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
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

    private void setupController() {
//        controller = new MusicController(this);
        controller = (MusicController) findViewById(R.id.media_controls_anchor);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        });
        controller.setMediaPlayerController(this);
        controller.setEnabled(true);
    }

    public void playNext() {
        Log.d(TAG, "playNext()");
        musicSrv.playNext();
        if(playbackPaused){
            setupController();
            playbackPaused=false;
        }
        controller.updateWidgets();
    }

    @Override
    public boolean hasNext() {
        return true; //TODO
    }

    public void playPrev() {
        Log.d(TAG, "playPrev()");
        musicSrv.playPrev();
        if(playbackPaused){
            setupController();
            playbackPaused=false;
        }
        controller.updateWidgets();
    }

    public void songPicked(View view) {
        Log.d(TAG, "songPicked");
        int songId = Integer.parseInt(view.getTag().toString());
        musicSrv.setSong(songId);
        Log.d(TAG, "song picked id: " + songId);
        musicSrv.playSong();
        if(playbackPaused){
            setupController();
            playbackPaused = false;
        }
        controller.updateWidgets();
    }
}
