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

package org.ciasaboark.canorum.song;

import org.ciasaboark.canorum.song.shadow.ShadowAlbum;

import java.io.Serializable;

/**
 * Created by Jonathan Nelson on 2/5/15.
 */
public class Album implements Serializable {
    private final long mAlbumId;
    private final String mAlbumName;
    private final int mYear;
    private final int mNumSongs;
    private final Artist mArtist;

    public Album(long albumId, String albumName, int year, int numSongs, Artist artist) {
        if (albumName == null || albumName.equals("")) {
            throw new IllegalArgumentException("album name can not be null or blank");
        }

        if (artist == null) {
            throw new IllegalArgumentException("artist can not be null, consider using newBlankAlbum()");
        }

        mAlbumId = albumId;
        mAlbumName = albumName;
        mYear = year;
        mNumSongs = numSongs;
        mArtist = artist;
    }

    private Album() {
        mAlbumId = -1;
        mAlbumName = "";
        mYear = 0;
        mNumSongs = 0;
        mArtist = Artist.newBlankArtist();
    }

    public static Album newBlankAlbum() {
        return new Album();
    }

    public static Album newSimpleAlbum(String albumTitle, String artistName) {
        Artist artist = new Artist(-1, artistName);
        Album album = new Album(-1, albumTitle, 0, 0, artist);
        return album;
    }

    public long getAlbumId() {
        return mAlbumId;
    }

    public Artist getArtist() {
        return mArtist;
    }

    public int getYear() {
        return mYear;
    }

    public int getNumSongs() {
        return mNumSongs;
    }

    @Override
    public boolean equals(Object o) {
        boolean objectsEqual = false;
        if (o instanceof Album) {
            objectsEqual = mAlbumName.equals(((Album) o).getAlbumName());
        }

        if (o instanceof ShadowAlbum) {
            objectsEqual = mAlbumName.equals(((ShadowAlbum) o).getAlbumName());
        }
        return objectsEqual;
    }

    public String getAlbumName() {
        return mAlbumName;
    }

    @Override
    public String toString() {
        return mAlbumName;
    }
}
