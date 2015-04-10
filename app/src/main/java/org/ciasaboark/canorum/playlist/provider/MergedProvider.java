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

package org.ciasaboark.canorum.playlist.provider;

import android.content.Context;
import android.util.Log;

import org.ciasaboark.canorum.playlist.provider.providers.SystemLibrary;
import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.song.Artist;
import org.ciasaboark.canorum.song.Genre;
import org.ciasaboark.canorum.song.Track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Jonathan Nelson on 2/27/15.
 */
public class MergedProvider {
    private static final String TAG = "MergedProvider";
    private static MergedProvider sInstance = null;
    private final Context mContext;
    private final HashMap<String, Album> mKnownAlbums;
    private final HashMap<String, Artist> mKnownArtists;
    private List<Track> mAllTracks;
    private SystemLibrary mSystemLibrary;
    private List<Provider> mKnownProviders;

    private MergedProvider(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
        mKnownAlbums = new HashMap<String, Album>();
        mKnownArtists = new HashMap<String, Artist>();
        mAllTracks = new ArrayList<Track>();
        initProviders();
    }

    private void initProviders() {
        long startTimeMs = System.currentTimeMillis();
        mKnownProviders = new ArrayList<Provider>();
        mSystemLibrary = new SystemLibrary(mContext);
        //TODO init other providers here
        mKnownProviders.add(mSystemLibrary);
        for (Provider provider : mKnownProviders) {
            List<Track> providerTracks = provider.getKnownTracks();
            //TODO do a better merge here so that no duplicates are added, and local tracks have precidence over remote tracks
            mAllTracks.addAll(providerTracks);
        }

        long endTimeMs = System.currentTimeMillis();
        Log.d(TAG, "initProviders() finished in " + String.valueOf(endTimeMs - startTimeMs) + " ms");
    }

    public static MergedProvider getInstance(Context ctx) {
        if (sInstance == null) {
            sInstance = new MergedProvider(ctx);
        }
        return sInstance;
    }

    public List<Track> getTrackList() {
        return new ArrayList<Track>(mAllTracks);
    }


    public List<Track> getTracksForArtist(Artist artist) {
        List<Track> tracks = new ArrayList<Track>();
        for (Track track : mAllTracks) {
            if (track.getSong().getAlbum().getArtist().equals(artist)) {
                tracks.add(track);
            }
        }
        return tracks;
    }

    public List<Album> getAlbumsForArtist(Artist artist) {
        List<Album> albums = new ArrayList<Album>();
        for (Track track : mAllTracks) {
            if (track.getSong().getAlbum().getArtist().equals(artist)) {
                Album album = track.getSong().getAlbum();
                if (!albums.contains(album)) {
                    albums.add(album);
                }
            }
        }

        return albums;
    }

    public List<Track> getTracksForAlbum(String artistName, Album album) {
        List<Track> tracks = new ArrayList<Track>();
        for (Track track : mAllTracks) {
            if (track.getSong().getAlbum().equals(album) && track.getSong().getAlbum().getArtist().toString().equals(artistName)) {
                tracks.add(track);
            }
        }
        return tracks;
    }

    public List<Track> getTracksForGenre(Genre genre) {
        List<Track> tracks = new ArrayList<Track>();
        if (genre != null) {
            for (Track track : mAllTracks) {
                Genre trackGenre = track.getGenre();
                if (trackGenre != null && trackGenre.equals(genre)) {
                    tracks.add(track);
                }
            }
        }
        return tracks;
    }

    public List<Genre> getKnownGenres() {
        List<Genre> knownGenres = new ArrayList<Genre>();
        for (Provider provider : mKnownProviders) {
            knownGenres.addAll(provider.getKnownGenres());
        }
        return knownGenres;
    }


    public List<Artist> getKnownArtists() {
        List<Artist> knownArtists = new ArrayList<Artist>();
        for (Provider provider : mKnownProviders) {
            knownArtists.addAll(provider.getKnownArtists());
        }
        return knownArtists;
    }

    public List<Album> getKnownAlbums() {
        List<Album> knownArtists = new ArrayList<Album>();
        for (Provider provider : mKnownProviders) {
            knownArtists.addAll(provider.getKnownAlbums());
        }
        return knownArtists;
    }
}
