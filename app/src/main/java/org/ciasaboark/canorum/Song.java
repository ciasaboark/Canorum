package org.ciasaboark.canorum;

/**
 * Created by Jonathan Nelson on 1/16/15.
 */
public class Song {
    private long id;

    public String getTitle() {
        return title;
    }

    public long getId() {
        return id;
    }

    public String getArtist() {
        return artist;
    }

    private String title;
    private String artist;

    public Song(long id, String title, String artist) {
        if (title == null || artist == null) {
            throw new IllegalArgumentException("title and artist can not be null");
        }
        this.id = id;
        this.title = title;
        this.artist = artist;
    }


}
