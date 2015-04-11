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

package org.ciasaboark.canorum.details.fetcher.artist;

import android.util.Log;

import org.ciasaboark.canorum.details.article.Article;
import org.ciasaboark.canorum.details.types.ArtistDetails;
import org.ciasaboark.canorum.song.Artist;
import org.ciasaboark.canorum.song.Genre;
import org.ciasaboark.canorum.vending.KeySet;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

/**
 * Created by Jonathan Nelson on 2/13/15.
 */
public class LastFmArtistArticleFetcher {
    public static final String TAG = "LastFmArtistArticleFtch";
    private static final String ARTIST_QUERY_SERVER = "http://ws.audioscrobbler.com/2.0/?method=artist.getInfo";
    private static String API_KEY = KeySet.LAST_FM_API_KEY;
    private Artist mArtist;

    public void setArticleSource(Artist articleSource) {
        mArtist = articleSource;
    }

    public ArtistDetails fetchArticle() {
        ArtistDetails details = null;
        Article article = null;
        List<Genre> genres = new ArrayList<Genre>();
        List<Artist> similarArtists = new ArrayList<Artist>();

        if (mArtist == null) {
            Log.e(TAG, "will not load article from last.fm without album set");
        } else {
            try {
                String artist = URLEncoder.encode(mArtist.getArtistName(), "UTF-8").replace("+", "%20");

                String queryUrl = ARTIST_QUERY_SERVER + "&artist=" + artist +
                        "&api_key=" + API_KEY;
                Log.d(TAG, "fetching artist information from " + queryUrl);
                URL url = new URL(queryUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.addRequestProperty("Referer", "https://github.com/ciasaboark/Canorum");

                XPathFactory factory = XPathFactory.newInstance();
                XPath xPath = factory.newXPath();
                InputSource inputSource = new InputSource(connection.getInputStream());
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                Document doc = documentBuilder.parse(inputSource);

                XPathExpression summaryExpression =
                        xPath.compile("/lfm/artist/bio/summary");
//                Node rootNode = (Node) xPath.evaluate("/", inputSource, XPathConstants.NODE);
                String summary = summaryExpression.evaluate(doc);

                XPathExpression urlExpression = xPath.compile("/lfm/artist/url");
                String readMoreUrl = urlExpression.evaluate(doc);

                XPathExpression similarArtistsExpression = xPath.compile("//lfm/artist/similar/artist/name/text()");
                NodeList similarArtistNodes = (NodeList) similarArtistsExpression.evaluate(doc, XPathConstants.NODESET);
                for (int i = 0; i < similarArtistNodes.getLength(); i++) {
                    Node node = similarArtistNodes.item(i);
                    String artistName = node.getNodeValue();
                    try {
                        Artist similarArtist = new Artist(-1, artistName);
                        similarArtists.add(similarArtist);
                    } catch (IllegalArgumentException e) {
                        //creation of the artist will fail if the name is blank or null, just skip
                        //adding this one to the list
                    }
                }

                if (summary != null && !summary.isEmpty() && readMoreUrl != null && !readMoreUrl.isEmpty()) {
                    article = new Article(readMoreUrl, Jsoup.parse(summary).text(), Article.SOURCE.LASTFM);
                }

            } catch (Exception e) {
                Log.e(TAG, "exception caught fetching album article " + e.getMessage());
            }
        }

        details = new ArtistDetails(article, similarArtists, genres);

        return details;
    }
}
