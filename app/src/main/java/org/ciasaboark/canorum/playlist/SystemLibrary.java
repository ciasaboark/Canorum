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

import org.ciasaboark.canorum.database.ratings.DatabaseWrapper;
import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.song.Artist;
import org.ciasaboark.canorum.song.Song;
import org.ciasaboark.canorum.song.Track;

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

    public List<Track> getTrackList() {
        Log.d(TAG, "getTrackList()");
        List<Track> tracks = new ArrayList<Track>();
        DatabaseWrapper databaseWrapper = DatabaseWrapper.getInstance(mContext);

        ContentResolver musicResolver = mContext.getContentResolver();
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = null;
        //start off by getting a list of songs, then grab the artist and album info later

        try {
            musicCursor = musicResolver.query(musicUri, null, selection, null, null);
            if (musicCursor != null && musicCursor.moveToFirst()) {
                int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
                int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int trackColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
                int durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

                int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int albumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                int yearColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.YEAR);
                int albumIdColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

                do {
                    long songId = musicCursor.getLong(idColumn);
                    String songTitle = musicCursor.getString(titleColumn);
                    long durationMs = musicCursor.getLong(durationColumn);
                    int duration = (int) (durationMs / 1000);
                    int trackNum = musicCursor.getInt(trackColumn);
                    Song song = new Song(songId, songTitle, trackNum, duration);

                    //get the artist
                    String songArtist = musicCursor.getString(artistColumn);
                    Artist artist = getArtist(songArtist);
                    //get the album
                    String songAlbum = musicCursor.getString(albumColumn);
                    Album album = getAlbum(artist, songAlbum);

                    Track track = new Track(artist, album, song);
                    int oldRating = databaseWrapper.getRatingForTrack(track);
                    song.setRating(oldRating);

                    long albumId = musicCursor.getLong(albumIdColumn);
                    //TODO try to speed this up a bit

                    tracks.add(track);
                } while (musicCursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (musicCursor != null) {
                musicCursor.close();
            }
        }
        return tracks;
    }

    public Artist getArtist(String artistName) {
        ContentResolver musicResolver = mContext.getContentResolver();
        String selection = MediaStore.Audio.Artists.ARTIST + " = ?";
        String[] selectionArgs = {
                artistName
        };
        Uri musicUri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        Cursor artistCursor = null;
        Artist artist = null;

        try {
            artistCursor = musicResolver.query(musicUri, null, selection, selectionArgs, null);
            if (artistCursor != null && artistCursor.moveToFirst()) {
                long artistId = artistCursor.getLong(artistCursor.getColumnIndex(MediaStore.Audio.Artists._ID));
                artist = new Artist(artistId, artistName);
            }
        } catch (Exception e) {
            Log.e(TAG, "error getting artist with name: " + artistName);
        } finally {
            if (artistCursor != null) {
                artistCursor.close();
            }
        }

        return artist;
    }

    public Album getAlbum(Artist artist, String albumName) {
        ContentResolver musicResolver = mContext.getContentResolver();
        String selection = MediaStore.Audio.Albums.ARTIST + " = ? AND" +
                MediaStore.Audio.Albums.ALBUM + " = ?";
        String[] selectionArgs = {
                artist.getArtistName(),
                albumName
        };
        Uri musicUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        Cursor albumCursor = null;
        Album album = null;

        try {
            albumCursor = musicResolver.query(musicUri, null, selection, selectionArgs, null);
            if (albumCursor != null && albumCursor.moveToFirst()) {
                long albumId = albumCursor.getLong(albumCursor.getColumnIndex(MediaStore.Audio.Albums._ID));
                int albumYear = albumCursor.getInt(albumCursor.getColumnIndex(MediaStore.Audio.Albums.FIRST_YEAR));
                int numSongs = albumCursor.getInt(albumCursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS));

                album = new Album(albumId, albumName, albumYear, numSongs);
            }
        } catch (Exception e) {
            Log.e(TAG, "error getting album with name: " + albumName + " by artist " + artist);
        } finally {
            if (albumCursor != null) {
                albumCursor.close();
            }
        }

        return album;
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
                    long artistId = musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.Artists._ID));
                    String artistName = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST));

                    Artist artist = new Artist(artistId, artistName);
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
        Uri artistsUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = null;

        try {
            musicCursor = musicResolver.query(artistsUri, null, null, null, null);
            if (musicCursor != null && musicCursor.moveToFirst()) {
                do {
                    long albumId = musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.Albums._ID));
                    String albumName = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                    int albumYear = musicCursor.getInt(musicCursor.getColumnIndex(MediaStore.Audio.Albums.FIRST_YEAR));
                    int numSongs = musicCursor.getInt(musicCursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS));

                    Album album = new Album(albumId, albumName, albumYear, numSongs);
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


    public List<Track> getTracksForArtist(Artist artist) {
        if (artist == null) {
            return new ArrayList<Track>();
        }

        Log.d(TAG, "getSongListForArtist()");
        ContentResolver musicResolver = mContext.getContentResolver();
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != ? AND " + MediaStore.Audio.Media.ARTIST + " = ?";
        String[] selectionArgs = {
                "0",
                artist.getArtistName()
        };
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        DatabaseWrapper databaseWrapper = DatabaseWrapper.getInstance(mContext);
        Cursor musicCursor = null;
        List<Track> tracks = new ArrayList<Track>();

        try {
            musicCursor = musicResolver.query(musicUri, null, selection, selectionArgs, null);
            if (musicCursor != null && musicCursor.moveToFirst()) {
                int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
                int trackNumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
                int durationColum = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                int albumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);

                do {
                    long songId = musicCursor.getLong(idColumn);
                    String songTitle = musicCursor.getString(titleColumn);
                    int trackNum = musicCursor.getInt(trackNumColumn);
                    long durationMs = musicCursor.getLong(durationColum);
                    int duration = (int) (durationMs / 1000);

                    Song song = new Song(songId, songTitle, trackNum, duration);
                    song.setRating(databaseWrapper.getRatingForTrack(song));

                    String albumName = musicCursor.getString(albumColumn);
                    Album album = getAlbum(artist, albumName);

                    Track track = new Track(artist, album, song);

                    tracks.add(track);
                } while (musicCursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (musicCursor != null) {
                musicCursor.close();
            }
        }
        return tracks;
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
                int albumIdColumn = albumsCursor.getColumnIndex(MediaStore.Audio.Albums._ID);
                int albumColumn = albumsCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
                int firstYearColumn = albumsCursor.getColumnIndex(MediaStore.Audio.Albums.FIRST_YEAR);
                int numSongsColumn = albumsCursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS);

                do {
                    long albumId = albumsCursor.getLong(albumIdColumn);
                    String albumTitle = albumsCursor.getString(albumColumn);
                    int albumYear = albumsCursor.getInt(firstYearColumn);
                    int numSongs = albumsCursor.getInt(numSongsColumn);

                    Album a = new Album(albumId, albumTitle, albumYear, numSongs);
                    albums.add(a);
                } while (albumsCursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, "caught exception while reading in list of albums for artist: " +
                    artist + ": " + e.getMessage());
        }

        return albums;
    }

    public List<Song> getSongsForAlbum(String artistName, Album album) {
        Log.d(TAG, "getSongListForArtist()");
        ContentResolver musicResolver = mContext.getContentResolver();
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != ? AND " +
                MediaStore.Audio.Media.ARTIST + " = ? AND " +
                MediaStore.Audio.Media.ALBUM + " = ? ";
        String[] selectionArgs = {
                "0",
                artistName,
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
                int yearColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.YEAR);
                int albumIdColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
                int trackNumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
                int durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

                do {
                    long songId = musicCursor.getLong(idColumn);
                    String songTitle = musicCursor.getString(titleColumn);
                    long albumId = musicCursor.getLong(albumIdColumn);
                    int trackNum = musicCursor.getInt(trackNumColumn);
                    long durationMs = musicCursor.getLong(durationColumn);
                    int durationS = (int) (durationMs / 1000l);

                    //TODO try to speed this up a bit
                    Song song = new Song(songId, songTitle, trackNum, durationS);
                    DatabaseWrapper databaseWrapper = DatabaseWrapper.getInstance(mContext);
                    int rating = databaseWrapper.getRatingForTrack(song);
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
}
