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

package org.ciasaboark.canorum.song.shadow;

import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.song.Artist;

import java.util.List;

/**
 * Created by Jonathan Nelson on 2/28/15.
 */
public class ShadowAlbum {
    private final Artist mArtist;
    private final String mAlbumName;
    private final int mYear;
    private final List<ShadowSong> mSongs;

    public ShadowAlbum(Artist artist, String albumName, int year, List<ShadowSong> songs) {
        if (artist == null) {
            throw new IllegalArgumentException("artist can not be null");
        }

        if (albumName == null || albumName.equals("")) {
            throw new IllegalArgumentException("album name can not be null or blank");
        }

        mArtist = artist;
        mAlbumName = albumName;
        mYear = year;
        mSongs = songs;
    }

    public String getAlbumName() {
        return mAlbumName;
    }

    public int getYear() {
        return mYear;
    }

    public List<ShadowSong> getSongs() {
        return mSongs;
    }

    @Override
    public boolean equals(Object o) {
        boolean objectsEqual = false;
        if (o instanceof ShadowAlbum) {
            objectsEqual = (mAlbumName.equals(((ShadowAlbum) o).mAlbumName) &&
                    mArtist.equals(((ShadowAlbum) o).getArtist()));

        }

        if (o instanceof Album) {
            objectsEqual = mAlbumName.equals(((Album) o).getAlbumName());
        }

        return objectsEqual;
    }

    public Artist getArtist() {
        return mArtist;
    }

    @Override
    public String toString() {
        return mAlbumName;
    }
}
