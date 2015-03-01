package org.ciasaboark.canorum.details.fetcher;/*
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

import android.util.Log;

import org.ciasaboark.canorum.details.article.Article;
import org.jsoup.Jsoup;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

/**
 * Created by Jonathan Nelson on 2/6/15.
 */
public class WikipediaJsonArticleFetcher {
    private static final String TAG = "WikipediaJsonFetcher";
    private static final String baseApiURL = "https://en.wikipedia.org/w/api.php?format=xml&action=query&prop=extracts&exintro=&explaintext=&redirects=true";
    private static final String baseArticleURL = "http://en.wikipedia.org/wiki/";


    public Article fetchArticle(String articleTitle) {
        Article article = null;
        try {
            String encodedArticle = URLEncoder.encode(articleTitle, "UTF-8").replace("+", "%20");
            String urlString = baseApiURL + "&titles=" + encodedArticle;
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("Referer", "https://github.com/ciasaboark/Canorum");

            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            InputSource inputSource = new InputSource(connection.getInputStream());
            XPathExpression summaryExpression =
                    xPath.compile("/api/query/pages/page/extract");
            Node rootNode = (Node) xPath.evaluate("/", inputSource, XPathConstants.NODE);
            String summary = summaryExpression.evaluate(rootNode);
            String articleURL = baseArticleURL + encodedArticle;
            if (summary != null && !summary.isEmpty() && articleTitle != null && !articleTitle.isEmpty()) {
                article = new Article(articleURL, Jsoup.parse(summary).text(), Article.SOURCE.WIKIPEDIA);
            }

        } catch (Exception e) {
            Log.e(TAG, "could not get content for article: " + articleTitle + ": " + e.getMessage());
        }
        return article;
    }

}
