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

package org.ciasaboark.canorum.playlist.provider.providers;

import android.content.Context;

import org.ciasaboark.canorum.playlist.provider.PROVIDER_TYPE;
import org.ciasaboark.canorum.playlist.provider.Provider;
import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.song.Artist;
import org.ciasaboark.canorum.song.Genre;
import org.ciasaboark.canorum.song.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonathan Nelson on 3/12/15.
 */
public class GooglePlayMusicProvider implements Provider {
    private static final String TAG = "GooglePlayMusicProvider";
    private List<Track> mTracks;

    private Context mContext;

    public GooglePlayMusicProvider(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
        mTracks = new ArrayList<Track>();
        init();
    }

    private void init() {
        //TODO
    }

    @Override
    public List<Track> getKnownTracks() {
        return mTracks;
    }

    @Override
    public List<Artist> getKnownArtists() {
        List<Artist> artists = new ArrayList<Artist>();
        for (Track track : mTracks) {
            Artist artist = track.getSong().getAlbum().getArtist();
            if (!artists.contains(artist)) {
                artists.add(artist);
            }
        }

        return artists;
    }

    @Override
    public List<Album> getKnownAlbums() {
        List<Album> albums = new ArrayList<Album>();
        for (Track track : mTracks) {
            Album album = track.getSong().getAlbum();
            if (!albums.contains(album)) {
                albums.add(album);
            }
        }

        return albums;
    }

    @Override
    public List<Genre> getKnownGenres() {
        return null;
    }

    @Override
    public boolean knowsTrack(Track track) {
        return mTracks.contains(track);
    }

    @Override
    public boolean knowsArtist(Artist artist) {
        boolean knowsArtist = false;
        for (Track track : mTracks) {
            if (track.getSong().getAlbum().getArtist().equals(artist)) {
                knowsArtist = true;
                break;
            }
        }

        return knowsArtist;
    }

    @Override
    public boolean knowsAlbum(Album album) {
        boolean knowsAlbum = false;
        for (Track track : mTracks) {
            if (track.getSong().getAlbum().equals((Album) album)) {
                knowsAlbum = true;
                break;
            }
        }

        return knowsAlbum;
    }

    @Override
    public boolean knowsGenre(Genre genre) {
        //TODO
        return false;
    }

    @Override
    public PROVIDER_TYPE getProviderType() {
        return PROVIDER_TYPE.LOCAL;
    }
}
