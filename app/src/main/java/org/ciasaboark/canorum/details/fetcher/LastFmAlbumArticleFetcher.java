///*
// * Copyright (c) 2015, Jonathan Nelson
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
// * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
// * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
// * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//
//package org.ciasaboark.canorum.details.fetcher;
//
//import android.util.Log;
//
//import org.ciasaboark.canorum.details.info.Article;
//import org.ciasaboark.canorum.details.info.ArticleFetcher;
//import org.ciasaboark.canorum.song.Artist;
//import org.ciasaboark.canorum.song.extended.ExtendedAlbum;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;
//import org.xml.sax.InputSource;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.io.StringReader;
//import java.io.UnsupportedEncodingException;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.net.URLEncoder;
//
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//
///**
// * Created by Jonathan Nelson on 2/13/15.
// */
//public class LastFmAlbumArticleFetcher {
//    public static final String TAG = "LastFmAlbumArticleFetcher";
//
//    private static final String ALBUM_QUERY_SERVER = "http://ws.audioscrobbler.com/2.0/?method=album.getInfo";
//    private static String API_KEY = "7bd3f54a7adae5681fe949279609b391";
//    private ExtendedAlbum mAlbum;
//
//    public void setArticleSource(ExtendedAlbum articleSource) {
//        mAlbum = articleSource;
//    }
//
//    public Article fetchArticle() {
//        Article article = null;
//         if (mAlbum == null) {
//            Log.e(TAG, "will not load article from last.fm without album set");
//        } else {
//            try {
//                String artist = URLEncoder.encode(((ExtendedAlbum) mAlbum).getArtistName(), "UTF-8").replace("+", "%20");
//                String album = URLEncoder.encode(((ExtendedAlbum) mAlbum).getAlbumName(), "UTF-8").replace("+", "%20");
//
//                String queryUrl = ALBUM_QUERY_SERVER + "&artist=" + artist + "&album=" + album +
//                        "&api_key=" + API_KEY;
//                Log.d(TAG, "fetching album information from " + queryUrl);
//                URL url = new URL(queryUrl);
//
//                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                connection.addRequestProperty("Referer", "https://github.com/ciasaboark/Canorum");
//
//                String line;
//                StringBuilder builder = new StringBuilder();
//                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                while ((line = reader.readLine()) != null) {
//                    builder.append(line);
//                }
//
//                Log.d(TAG, "full xml response: " + builder.toString());
//                connection.disconnect();
//                reader = null;
//
//                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//                DocumentBuilder db = dbf.newDocumentBuilder();
//                InputSource is = new InputSource(new StringReader(builder.toString()));
//                Document document = db.parse(is);
//                document.getDocumentElement().normalize();
//
//                Element docElement = document.getDocumentElement();
//                NodeList nodeList = docElement.getElementsByTagName("album");
//                for (int i = 0; i < nodeList.getLength(); i++) {
//                    Node node = nodeList.item(i);
//                    if (node.getNodeType() == Node.ELEMENT_NODE) {
//                        Element element = (Element) node;
//                        NodeList imageNodes = element.getElementsByTagName("image");
//                        for (int j = 0; j < imageNodes.getLength(); j++) {
//                            Node imageNode = imageNodes.item(j);
//                            if (imageNode.getNodeType() == Node.ELEMENT_NODE) {
//                                Element imageElement = (Element) imageNode;
//                                String imageSizeString = imageElement.getAttribute("size");
//                                IMAGE_SIZE imageSize;
//                                try {
//                                    imageSize = IMAGE_SIZE.valueOf(imageSizeString.toUpperCase());
//                                    if (imageSize.val > bestImageSize.val) {
//                                        String imageUrlLocation = imageElement.getTextContent();
//                                        Log.d(TAG, "considering image with size " + imageSizeString + ": " + imageUrlLocation);
//                                        bestUrlString = imageUrlLocation;
//                                        bestImageSize = imageSize;
//                                    }
//                                } catch (IllegalArgumentException e) {
//                                    //this will generate an exception for all but large, extralarge, and mega images
//                                    Log.d(TAG, "skipping image with size: " + imageSizeString);
//                                }
//                            }
//                        }
//                    }
//                }
//
//            } catch (Exception e) {
//                Log.e(TAG, "exception caught fetching album article " + e.getMessage());
//            }
//        }
//        return article;
//    }
//
//}
