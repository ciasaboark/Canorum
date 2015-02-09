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

package org.ciasaboark.canorum.playlist;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import org.ciasaboark.canorum.Album;
import org.ciasaboark.canorum.Artist;
import org.ciasaboark.canorum.Song;
import org.ciasaboark.canorum.database.ratings.DatabaseWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonathan Nelson on 2/5/15.
 */
public class SystemLibrary {
    private static final String TAG = "SystemLibrary";
    private final Context mContext;


    public SystemLibrary(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
    }

    public List<Song> getSongList() {
        Log.d(TAG, "getSongList()");
        ContentResolver musicResolver = mContext.getContentResolver();
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = null;
        List<Song> songs = new ArrayList<Song>();

        try {
            musicCursor = musicResolver.query(musicUri, null, selection, null, null);
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
                    //TODO try to speed this up a bit
                    Song song = new Song(songId, songTitle, songArtist, songAlbum, albumId);
                    DatabaseWrapper databaseWrapper = DatabaseWrapper.getInstance(mContext);
                    int rating = databaseWrapper.getRatingForSong(song);
                    song.setRating(rating);
                    songs.add(song);
                } while (musicCursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (musicCursor != null) {
                musicCursor.close();
            }
        }
        return songs;
    }

    public List<Artist> getArtistList() {
        List<Artist> artists = new ArrayList<Artist>();

        Log.d(TAG, "getArtistList()");
        ContentResolver musicResolver = mContext.getContentResolver();
//        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        Uri artistsUri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = null;

        try {
            musicCursor = musicResolver.query(artistsUri, null, null, null, null);
            if (musicCursor != null && musicCursor.moveToFirst()) {
                do {
                    String artistName = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
                    long artistId = musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.Artists._ID));
                    int artistAlbumCount = musicCursor.getInt(musicCursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS));

                    //TODO try to speed this up a bit
                    Artist artist = new Artist(artistName);
                    artist.setAlbumCount(artistAlbumCount);
                    artists.add(artist);
                } while (musicCursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (musicCursor != null) {
                musicCursor.close();
            }
        }
        return artists;

    }

    public List<Album> getAlbumList() {
        Log.d(TAG, "getAlbumList()");
        List<Album> albums = new ArrayList<Album>();

        ContentResolver musicResolver = mContext.getContentResolver();
//        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        Uri artistsUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = null;

        try {
            musicCursor = musicResolver.query(artistsUri, null, null, null, null);
            if (musicCursor != null && musicCursor.moveToFirst()) {
                do {
                    String artistName = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                    String albumName = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                    long albumId = musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.Albums._ID));

                    //TODO try to speed this up a bit
                    Album album = new Album(albumId, artistName, albumName);
                    albums.add(album);
                } while (musicCursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (musicCursor != null) {
                musicCursor.close();
            }
        }
        return albums;

    }


    public List<Song> getSongsForArtist(Artist artist) {
        Log.d(TAG, "getSongListForArtist()");
        ContentResolver musicResolver = mContext.getContentResolver();
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != ? AND " + MediaStore.Audio.Media.ARTIST + " = ?";
        String[] selectionArgs = {
                "0",
                artist.getArtistName()
        };
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = null;
        List<Song> songs = new ArrayList<Song>();

        try {
            musicCursor = musicResolver.query(musicUri, null, selection, selectionArgs, null);
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
                    //TODO try to speed this up a bit
                    Song song = new Song(songId, songTitle, songArtist, songAlbum, albumId);
                    DatabaseWrapper databaseWrapper = DatabaseWrapper.getInstance(mContext);
                    int rating = databaseWrapper.getRatingForSong(song);
                    song.setRating(rating);
                    songs.add(song);
                } while (musicCursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (musicCursor != null) {
                musicCursor.close();
            }
        }
        return songs;
    }

    public List<Album> getAlbumsForArtist(Artist artist) {
        List<Album> albums = new ArrayList<Album>();
        ContentResolver musicResolver = mContext.getContentResolver();
        String selection = MediaStore.Audio.Albums.ARTIST + " = ? ";
        String[] selectionArgs = {artist.getArtistName()};
        Uri musicUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        Cursor albumsCursor = null;

        try {
            albumsCursor = musicResolver.query(musicUri, null, selection, selectionArgs, null);
            if (albumsCursor != null && albumsCursor.moveToFirst()) {
                int albumColumn = albumsCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
                int firstYearColumn = albumsCursor.getColumnIndex(MediaStore.Audio.Albums.FIRST_YEAR);
                int lastYearColumn = albumsCursor.getColumnIndex(MediaStore.Audio.Albums.FIRST_YEAR);
                int albumIdColumn = albumsCursor.getColumnIndex(MediaStore.Audio.Albums._ID);

                do {
                    long albumId = albumsCursor.getLong(albumIdColumn);
                    String albumTitle = albumsCursor.getString(albumColumn);
                    String albumYear = albumsCursor.getString(firstYearColumn);
                    if (albumYear == null) {
                        albumYear = albumsCursor.getString(lastYearColumn);
                    }
                    Album a = new Album(-1, artist.getArtistName(), albumTitle);
                    a.setYear(albumYear);
                    albums.add(a);
                } while (albumsCursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, "caught exception while reading in list of albums for artist: " +
                    artist + ": " + e.getMessage());
        }

        return albums;
    }

    public List<Song> getSongsForAlbum(Album album) {
        Log.d(TAG, "getSongListForArtist()");
        ContentResolver musicResolver = mContext.getContentResolver();
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != ? AND " +
                MediaStore.Audio.Media.ARTIST + " = ? AND " +
                MediaStore.Audio.Media.ALBUM + " = ? ";
        String[] selectionArgs = {
                "0",
                album.getArtistName(),
                album.getAlbumName()
        };
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = null;
        List<Song> songs = new ArrayList<Song>();

        try {
            musicCursor = musicResolver.query(musicUri, null, selection, selectionArgs, null);
            if (musicCursor != null && musicCursor.moveToFirst()) {
                int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
                int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int albumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                int yearColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.YEAR);
                int albumIdColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
                int trackNumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
                int durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

                do {
                    long songId = musicCursor.getLong(idColumn);
                    String songTitle = musicCursor.getString(titleColumn);
                    String songArtist = musicCursor.getString(artistColumn);
                    String songAlbum = musicCursor.getString(albumColumn);
                    long albumId = musicCursor.getLong(albumIdColumn);
                    int trackNum = musicCursor.getInt(trackNumColumn);
                    long durationMs = musicCursor.getLong(durationColumn);
                    int durationS = (int) (durationMs / 1000l);

                    //TODO try to speed this up a bit
                    Song song = new Song(songId, songTitle, songArtist, songAlbum, albumId);
                    DatabaseWrapper databaseWrapper = DatabaseWrapper.getInstance(mContext);
                    int rating = databaseWrapper.getRatingForSong(song);
                    song.setRating(rating);
                    song.setDuration(durationS);
                    song.setmTrackNum(trackNum);
                    songs.add(song);
                } while (musicCursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (musicCursor != null) {
                musicCursor.close();
            }
        }
        return songs;
    }
}
