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

package org.ciasaboark.canorum.details;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.ciasaboark.canorum.details.article.Article;
import org.ciasaboark.canorum.details.fetcher.WikipediaJsonArticleFetcher;
import org.ciasaboark.canorum.details.fetcher.artist.LastFmArtistArticleFetcher;
import org.ciasaboark.canorum.details.foo.AlbumDetails;
import org.ciasaboark.canorum.details.foo.ArtistDetails;
import org.ciasaboark.canorum.details.foo.Details;
import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.song.Artist;
import org.ciasaboark.canorum.song.Genre;

import java.util.ArrayList;

/**
 * Created by Jonathan Nelson on 2/6/15.
 */
public class DetailsFetcher {
    private static final String TAG = "DetailsFetcher";
    private final Activity mContext;
    private DetailsLoadedWatcher mWatcher;
    private Artist mArticleArtist;
    private Album mArticleAlbum;
    private Boolean mLoadArtistArticle;

    public DetailsFetcher(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        if (!(ctx instanceof Activity)) {
            throw new IllegalArgumentException(TAG + " must be called with an activity context");
        }
        mContext = (Activity) ctx;
    }

    public DetailsFetcher setArticleSource(Artist artist) {
        mArticleArtist = artist;
        mLoadArtistArticle = true;
        return this;
    }

    public DetailsFetcher setArticleSource(Album album) {
        mArticleAlbum = album;
        mLoadArtistArticle = false;
        return this;
    }

    public DetailsFetcher setArticleLoadedWatcher(DetailsLoadedWatcher watcher) {
        mWatcher = watcher;
        return this;
    }


    public DetailsFetcher loadInBackground() {
        if (mWatcher == null) {
            Log.e(TAG, "will not fetch article without watcher given");
        } else if (mLoadArtistArticle == null) {
            Log.e(TAG, "will not fetch article without article source given");
        } else {
            if (mLoadArtistArticle) {
                FetchArtistDetailsTask artistFetcher = new FetchArtistDetailsTask();
                artistFetcher.execute(mArticleArtist);
            } else {
                FetchAlbumDetailsTask albumFetcher = new FetchAlbumDetailsTask();
                albumFetcher.execute(mArticleAlbum);
            }
        }

        return this;
    }

    public void sendArticle(final Details details) {
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWatcher.onDetailsLoaded(details);
            }
        });
    }

    private class FetchArtistDetailsTask extends AsyncTask<Artist, Void, ArtistDetails> {
        private String urlSource;

        @Override
        protected ArtistDetails doInBackground(Artist... artists) {
            ArtistDetails artistDetails = null;

            Artist artist = artists[0];
            LastFmArtistArticleFetcher artistFetcher = new LastFmArtistArticleFetcher();
            artistFetcher.setArticleSource(artist);

            artistDetails = artistFetcher.fetchArticle();
            if (artistDetails == null) {
                WikipediaJsonArticleFetcher wikipediaArticleFetcher = new WikipediaJsonArticleFetcher();
//                artistDetails = wikipediaArticleFetcher.fetchArticle(artist.getArtistName());
            }

            if (artistDetails == null) {
                artistDetails = new ArtistDetails(
                        new Article(null, "No information found", Article.SOURCE.UNKNOWN),
                        new ArrayList<Artist>(),
                        new ArrayList<Genre>());

            }

            return artistDetails;
        }

        @Override
        protected void onPostExecute(ArtistDetails details) {
            sendArticle(details);
        }

    }

    private class FetchAlbumDetailsTask extends AsyncTask<Album, Void, Details> {
        private String urlSource;

        @Override
        protected Details doInBackground(Album... albums) {
            AlbumDetails albumDetails = null;

            Album album = albums[0];
            WikipediaJsonArticleFetcher wikipediaArticleFetcher = new WikipediaJsonArticleFetcher();
            Article article = wikipediaArticleFetcher.fetchArticle(album.getAlbumName());
            if (article != null) {
                albumDetails = new AlbumDetails(article);
            }
            return albumDetails;
        }

        @Override
        protected void onPostExecute(Details details) {
            sendArticle(details);
        }

    }
}
