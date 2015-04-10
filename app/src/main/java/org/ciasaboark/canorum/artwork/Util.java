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

package org.ciasaboark.canorum.artwork;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.song.Artist;
import org.ciasaboark.canorum.song.Track;

/**
 * Created by Jonathan Nelson on 1/30/15.
 */
public class Util {
    public static boolean isConnectedToNetwork(Context mContext) {
        boolean isNetworkConnected = false;

        ConnectivityManager connMgr = (ConnectivityManager) mContext.
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            isNetworkConnected = true;
        }
        return isNetworkConnected;
    }

    public static boolean isTrackValid(Track track) {
        boolean songIsValid = true;
        String album = track.getSong().getAlbum().getAlbumName();
        String artist = track.getSong().getAlbum().getArtist().getArtistName();
        if (album.equals("") || artist.equals(""))
            songIsValid = false;
        if (album.equalsIgnoreCase("<unknown>"))
            songIsValid = false;
        if (artist.equalsIgnoreCase("<unknown>"))
            songIsValid = false;

        return songIsValid;
    }

    public static boolean isAlbumValid(Album album) {   //TODO only take ExtendedAlbum reference?
        boolean albumIsValid = true;
        if (album == null) {
            return false;
        }

        String albumName = album.getAlbumName();
        if (albumName.equals("") || albumName.equals("<unknown>") || albumName.equals("[non-album tracks]")) {
            albumIsValid = false;
        }

        return albumIsValid;
    }

    public static boolean isArtistValid(Artist artist) {
        boolean artistIsValid = true;
        if (artist == null) {
            return false;
        }

        String artistName = artist.getArtistName();
        //Android system uses "Music" as artist name for tracks with unknown artist
        if (artistName.equals("") || artistName.equals("<unknown>")) {
            artistIsValid = false;
        }

        return artistIsValid;
    }
}
