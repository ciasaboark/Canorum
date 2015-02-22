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

package org.ciasaboark.canorum.info;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.ciasaboark.canorum.info.fetcher.LastFmArtistArticleFetcher;
import org.ciasaboark.canorum.info.fetcher.WikipediaJsonArticleFetcher;
import org.ciasaboark.canorum.song.Artist;
import org.ciasaboark.canorum.song.extended.ExtendedAlbum;

/**
 * Created by Jonathan Nelson on 2/6/15.
 */
public class ArticleFetcher {
    private static final String TAG = "ArticleFetcher";
    private final Activity mContext;
    private ArticleLoadedWatcher mWatcher;
    private Artist mArticleArtist;
    private ExtendedAlbum mArticleAlbum;
    private Boolean mLoadArtistArticle;

    public ArticleFetcher(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        if (!(ctx instanceof Activity)) {
            throw new IllegalArgumentException(TAG + " must be called with an activity context");
        }
        mContext = (Activity) ctx;
    }

    public ArticleFetcher setArticleSource(Artist artist) {
        mArticleArtist = artist;
        mLoadArtistArticle = true;
        return this;
    }

    public ArticleFetcher setArticleSource(ExtendedAlbum album) {
        mArticleAlbum = album;
        mLoadArtistArticle = false;
        return this;
    }

    public ArticleFetcher setArticleLoadedWatcher(ArticleLoadedWatcher watcher) {
        mWatcher = watcher;
        return this;
    }


    public ArticleFetcher loadInBackground() {
        if (mWatcher == null) {
            Log.e(TAG, "will not fetch article without watcher given");
        } else if (mLoadArtistArticle == null) {
            Log.e(TAG, "will not fetch article without article source given");
        } else {
            if (mLoadArtistArticle) {
                FetchArtistArticleTask artistFetcher = new FetchArtistArticleTask();
                artistFetcher.execute(mArticleArtist);
            } else {
                FetchAlbumArticleTask albumFetcher = new FetchAlbumArticleTask();
                albumFetcher.execute(mArticleAlbum);
            }
        }

        return this;
    }

    public void sendArticle(final Article article) {
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWatcher.onArticleLoaded(article);
            }
        });
    }

    private class FetchArtistArticleTask extends AsyncTask<Artist, Void, Article> {
        private String urlSource;

        @Override
        protected Article doInBackground(Artist... artists) {
            Artist artist = artists[0];
            LastFmArtistArticleFetcher artistFetcher = new LastFmArtistArticleFetcher();
            artistFetcher.setArticleSource(artist);

            Article article = artistFetcher.fetchArticle();
            if (article == null) {
                WikipediaJsonArticleFetcher wikipediaArticleFetcher = new WikipediaJsonArticleFetcher();
                article = wikipediaArticleFetcher.fetchArticle(artist.getArtistName());
            }
            return article;
        }

        @Override
        protected void onPostExecute(Article article) {
            sendArticle(article);
        }

    }

    private class FetchAlbumArticleTask extends AsyncTask<ExtendedAlbum, Void, Article> {
        private String urlSource;

        @Override
        protected Article doInBackground(ExtendedAlbum... albums) {
            ExtendedAlbum album = albums[0];
            WikipediaJsonArticleFetcher wikipediaArticleFetcher = new WikipediaJsonArticleFetcher();
            Article article = wikipediaArticleFetcher.fetchArticle(album.getAlbumName());
            return article;
        }

        @Override
        protected void onPostExecute(Article article) {
            sendArticle(article);
        }

    }
}
