package org.ciasaboark.canorum;

import java.io.Serializable;

/**
 * Created by Jonathan Nelson on 1/16/15.
 */
public class Song implements Serializable {
    private long mId;
    private String mTitle = "";
    private String mArtist = "";
    private String mAlbum = "";
    private long mAlbumId;

    public Song(long id) {
        this(id, null);
    }

    public Song(long id, String title) {
        this(id, title, null);
    }

    public Song(long id, String title, String artist) {
        this(id, title, artist, null);
    }

    public Song (long id, String title, String artist, String album) {
        this (id, title, artist, album, -1);
    }
    public Song(long id, String title, String artist, String album, long albumId) {
        mId = id;
        mTitle = title == null ? "" : title;
        mArtist = artist == null ? "" : artist;
        mAlbum = album == null ? "" : album;
        mAlbumId = albumId;
    }

    public String getTitle() {
        return mTitle;
    }

    public long getId() {
        return mId;
    }

    public String getArtist() {
        return mArtist;
    }

    public String getmAlbum() {
        return mAlbum;
    }

    public long getmAlbumId() {
        return mAlbumId;
    }


}
