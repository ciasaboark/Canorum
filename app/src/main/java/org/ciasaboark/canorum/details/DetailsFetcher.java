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
import org.ciasaboark.canorum.details.types.AlbumDetails;
import org.ciasaboark.canorum.details.types.ArtistDetails;
import org.ciasaboark.canorum.details.types.Details;
import org.ciasaboark.canorum.details.types.GenreDetails;
import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.song.Artist;
import org.ciasaboark.canorum.song.Genre;

import java.util.ArrayList;

/**
 * Created by Jonathan Nelson on 2/6/15.
 */
public class DetailsFetcher {
    private static final String TAG = "DetailsFetcher";
    private final Context mContext;
    private DetailsLoadedWatcher mWatcher;
    private Artist mArticleArtist;
    private Album mArticleAlbum;
    private Genre mArticleGenre;

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
        mArticleAlbum = null;
        mArticleGenre = null;
        return this;
    }

    public DetailsFetcher setArticleSource(Album album) {
        mArticleArtist = null;
        mArticleAlbum = album;
        mArticleGenre = null;
        return this;
    }

    public DetailsFetcher setArticleSource(Genre genre) {
        mArticleArtist = null;
        mArticleAlbum = null;
        mArticleGenre = genre;
        return this;
    }

    public DetailsFetcher setArticleLoadedWatcher(DetailsLoadedWatcher watcher) {
        mWatcher = watcher;
        return this;
    }


    public DetailsFetcher loadInBackground() {
        if (mWatcher == null) {
            Log.e(TAG, "will not fetch article without watcher given");
        }

        if (mArticleArtist != null) {
            FetchArtistDetailsTask artistFetcher = new FetchArtistDetailsTask();
            artistFetcher.execute(mArticleArtist);
        } else if (mArticleAlbum != null) {
            FetchAlbumDetailsTask albumFetcher = new FetchAlbumDetailsTask();
            albumFetcher.execute(mArticleAlbum);
        } else if (mArticleGenre != null) {
            FetchGenreDetailsTask genreFetcher = new FetchGenreDetailsTask();
            genreFetcher.execute(mArticleGenre);
        } else {
            Log.e(TAG, "will not fetch article without article source given");
        }

        return this;
    }

    private void sendArticle(final Details details) {
        if (mContext instanceof Activity) {
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mWatcher.onDetailsLoaded(details);
                }
            });
        } else {
            mWatcher.onDetailsLoaded(details);
        }
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
                Article article = wikipediaArticleFetcher.fetchArticle(artist.getArtistName());
                artistDetails = new ArtistDetails(article, null, null);
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

    private class FetchGenreDetailsTask extends AsyncTask<Genre, Void, Details> {
        private String urlSource;

        @Override
        protected Details doInBackground(Genre... genres) {
            GenreDetails genreDetails = null;

            Genre genre = genres[0];
            WikipediaJsonArticleFetcher wikipediaArticleFetcher = new WikipediaJsonArticleFetcher();
            Article article = wikipediaArticleFetcher.fetchArticle(genre.getGenre());
            if (article != null) {
                genreDetails = new GenreDetails(article);
            }
            return genreDetails;
        }

        @Override
        protected void onPostExecute(Details details) {
            sendArticle(details);
        }

    }
}
