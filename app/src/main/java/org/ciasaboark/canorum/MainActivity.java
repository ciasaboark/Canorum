package org.ciasaboark.canorum;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.CursorLoader;
import android.content.IntentFilter;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.widget.Toast;

import org.ciasaboark.canorum.MusicService.MusicBinder;
import org.ciasaboark.canorum.view.MusicController;


public class MainActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "MainActivity";
    private static final int ALBUM_ART_LOADER = 1;
    private MusicControllerSingleton musicControllerSingleton;
    private View mErrorView;
    private View mCurPlayCard;
    private TextView mCurTitle;
    private TextView mCurArtist;
    private TextView mCurAlbum;
    private ImageView mCurAlbumArt;
    private MusicController controller;
    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter mIntentFilter;




    //drawer layout
    private String[] drawerList = {"one", "two", "settings"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_playing);

        mErrorView = findViewById(R.id.cur_play_error);
        mCurPlayCard = findViewById(R.id.cur_play_card);
        mCurTitle = (TextView) findViewById(R.id.cur_play_title);
        mCurArtist = (TextView) findViewById(R.id.cur_play_artist);
        mCurAlbum = (TextView) findViewById(R.id.cur_play_album);
        mCurAlbumArt =  (ImageView) findViewById(R.id.cur_play_album_art);

        musicControllerSingleton = MusicControllerSingleton.getInstance(this);
        musicControllerSingleton.getSongList();

        setupController();
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(MusicControllerSingleton.ACTION_PLAY);
        mIntentFilter.addAction(MusicControllerSingleton.ACTION_PAUSE);
        mIntentFilter.addAction(MusicControllerSingleton.ACTION_NEXT);
        mIntentFilter.addAction(MusicControllerSingleton.ACTION_PREV);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case MusicControllerSingleton.ACTION_PLAY:
                        Song curSong = null;
                        curSong = (Song) intent.getSerializableExtra("curSong");
                        if (curSong == null) {
                            Log.w(TAG, "got broadcast notification that a song has began playing, but could " +
                                    "not get song from intent");
                        }
                        updateCurPlayCard(curSong);
                        break;
                    default:
                        Log.d(TAG, "got a broadcast notification with action type " +
                                intent.getAction() + " which is not yet supported");
                }
            }
        };
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setupController() {
        controller = (MusicController) findViewById(R.id.media_controls);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicControllerSingleton.playPrev();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicControllerSingleton.playNext();
            }
        });
        controller.setMediaPlayerController(MusicControllerSingleton.getInstance(this));
        controller.setEnabled(true);
    }

    private void updateCurPlayCard(Song curSong) {
        if (musicControllerSingleton == null || musicControllerSingleton.getSongListSize() == 0) {
            showErrorCard();
        } else {
            //if we weren't explicitly given the current song then we can try to get it from the
            //service
            if (curSong == null) {
                curSong = musicControllerSingleton.getCurSong();
            }

            if (curSong == null) {
                Log.e(TAG, "error getting current song");
                showErrorCard();
            } else {
                mCurPlayCard.setVisibility(View.VISIBLE);
                mErrorView.setVisibility(View.INVISIBLE);
                mCurTitle.setText(curSong.getTitle());
                mCurArtist.setText(curSong.getArtist());
                mCurAlbum.setText(curSong.getmAlbum());
                //TODO set background album art
                Bundle bundle = new Bundle();
                bundle.putLong("albumId", curSong.getmAlbumId());
                getLoaderManager().initLoader(ALBUM_ART_LOADER, bundle, this);
            }
        }
    }

    private void showErrorCard() {
        mCurPlayCard.setVisibility(View.INVISIBLE);
        mErrorView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause(){
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (musicControllerSingleton.getSongListSize() == 0) {
            mCurPlayCard.setVisibility(View.INVISIBLE);
            mErrorView.setVisibility(View.VISIBLE);
        } else {
            mCurPlayCard.setVisibility(View.VISIBLE);
            mErrorView.setVisibility(View.INVISIBLE);
        }

        controller.updateWidgets();
    }

    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, mIntentFilter);
        updateCurPlayCard();

    }

    private void updateCurPlayCard() {
        updateCurPlayCard(null);
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
                Toast.makeText(this, "no longer supported", Toast.LENGTH_SHORT).show();
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = null;
        switch (id) {
            case ALBUM_ART_LOADER:
                if (args == null) {
                    Log.d(TAG, "onCreateLoader() can not fetch album art without album id");
                } else {
                    long albumId = args.getLong("albumId", -1);
                    cursorLoader = new CursorLoader(
                            this,
                            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                            new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                            MediaStore.Audio.Albums._ID + "=?",
                            new String[]{String.valueOf(albumId)},
                            null);
                }
                break;
        }
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            try {
                String albumArtPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                Bitmap bitmap;
                if (albumArtPath == null) {
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_album_art);
                } else {
                    bitmap = BitmapFactory.decodeFile(albumArtPath);
                }
                mCurAlbumArt.setImageBitmap(bitmap);
            } catch (Exception e) {
                Log.e(TAG, "error getting album art uri from cursor: " + e.getMessage());
            }

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
