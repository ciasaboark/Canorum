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

import java.io.Serializable;

/**
 * Created by Jonathan Nelson on 1/16/15.
 */
public class Song implements Serializable {
    private final long mId;
    private final String mTitle;
    private final int mTrackNum;
    private final int mDuration;
    private final Album mAlbum;

    public Song(long id, String title, int trackNum, int duration, Album album) {
        if (title == null) {
            throw new IllegalArgumentException("title can not be null");
        }

        if (album == null) {
            throw new IllegalArgumentException("album can not be null, use newBlankSong()");
        }

        mId = id;
        mTitle = title;
        mTrackNum = trackNum;
        mDuration = duration;
        mAlbum = album;
    }

    public static Song newBlankSong() {
        //TODO
        return null;
    }

    public static Song newSimpleSong(String songTitle, String albumTitle, String artistName) {
        Album album = Album.newSimpleAlbum(albumTitle, artistName);
        return new Song(-1, songTitle, 0, 0, album);
    }

    public long getId() {
        return mId;
    }

    public Album getAlbum() {
        return mAlbum;
    }

    public String getTitle() {
        return mTitle;
    }

    public int getRawTrackNum() {
        return mTrackNum;
    }

    public int getDiskNum() {
        return mTrackNum / 1000;
    }

    public int getFormattedTrackNum() {
        //track  number incodes both track number and disk number in the format of DTTT
        if (mTrackNum < 1000) {
            return mTrackNum;
        } else {
            return mTrackNum % 1000;
        }
    }


    public int getDuration() {
        return mDuration;
    }


    @Override
    public String toString() {
        return mTitle;
    }


}
