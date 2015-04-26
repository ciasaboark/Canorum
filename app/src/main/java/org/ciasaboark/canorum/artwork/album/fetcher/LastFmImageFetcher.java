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

package org.ciasaboark.canorum.artwork.album.fetcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.ciasaboark.canorum.artwork.Util;
import org.ciasaboark.canorum.artwork.exception.ArtworkNotFoundException;
import org.ciasaboark.canorum.prefs.RatingsPrefs;
import org.ciasaboark.canorum.song.Album;
import org.ciasaboark.canorum.vending.KeySet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by Jonathan Nelson on 1/29/15.
 */
public class LastFmImageFetcher {
    private static final String TAG = "LastFmImageFetcher";
    private static final String queryServer = "http://ws.audioscrobbler.com/2.0/?method=album.getInfo";
    private static String api_key = KeySet.LAST_FM_API_KEY;
    // Please provide your consumer secret here
    private static String consumer_secret = "";
    private final RatingsPrefs ratingsPrefs;
    private final Context mContext;
    private Bitmap bestKnownBitmap = null;
    private Album mAlbum;

    private String mBestUrlString = null;
    private IMAGE_SIZE mBestImageSize = IMAGE_SIZE.UNDEFINED;


    public LastFmImageFetcher(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
        ratingsPrefs = new RatingsPrefs(mContext);
    }

    public LastFmImageFetcher setAlbum(Album album) {
        mAlbum = album;
        return this;
    }

    public BitmapDrawable loadArtwork() throws ArtworkNotFoundException {
//        Log.d(TAG, "beginning last.fm album art fetch");
        BitmapDrawable artwork = null;
        String exceptionMessage = "";


        if (!Util.isConnectedToNetwork(mContext)) {
            throw new ArtworkNotFoundException("Can not fetch artwork, network is down");
        } else if (!Util.isAlbumValid(mAlbum)) {
            throw new ArtworkNotFoundException("Can not fetch artwork for invalid album");
        } else {
            String queryUrl;
            try {
                queryUrl = getQueryUrl();
            } catch (UnsupportedEncodingException e) {
                throw new ArtworkNotFoundException("encoding exception caught " + e.getMessage());
            }

            String response = readFromUrl(queryUrl);
//            Log.d(TAG, "full xml response: " + response);

            if (response == null) {
                throw new ArtworkNotFoundException("no response from the server");
            }
            processResponse(response);

            if (mBestImageSize == null) {
                exceptionMessage = "unable to find an album image that is at least large size";
            } else {
//                Log.d(TAG, "fetching image with size " + mBestImageSize + " from " + mBestUrlString);
                Bitmap bitmap = downloadArtwork(mBestUrlString);
                if (bitmap == null) {
                    exceptionMessage = "error downloading artwork from " + mBestUrlString;
                } else {
                    artwork = new BitmapDrawable(bitmap);
                }
            }
        }

        if (artwork == null) {
            throw new ArtworkNotFoundException(exceptionMessage);
        }
        return artwork;
    }

    private String getQueryUrl() throws UnsupportedEncodingException {
        String queryUrl = null;
        String artist = mAlbum.getArtist().getArtistName();
        artist = URLEncoder.encode(artist, "UTF-8").replace("+", "%20");

        String album = mAlbum.getAlbumName();
        album = URLEncoder.encode(album, "UTF-8").replace("+", "%20");


        queryUrl = queryServer + "&artist=" + artist + "&album=" + album +
                "&api_key=" + api_key;

        return queryUrl;
    }

    private String readFromUrl(String urlString) {
        String fullResponse = null;
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
//            Log.d(TAG, "fetching album information from " + urlString);
            URL url = new URL(urlString);

            connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("Referer", "https://github.com/ciasaboark/Canorum");

            String line;
            StringBuilder builder = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            fullResponse = builder.toString();

        } catch (IOException e) {

        } finally {
            if (connection != null) connection.disconnect();
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {

                }
                reader = null;
            }
        }

        return fullResponse;
    }

    private void processResponse(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            Document document = db.parse(is);
            document.getDocumentElement().normalize();

            Element docElement = document.getDocumentElement();
            NodeList nodeList = docElement.getElementsByTagName("album");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    NodeList imageNodes = element.getElementsByTagName("image");
                    for (int j = 0; j < imageNodes.getLength(); j++) {
                        Node imageNode = imageNodes.item(j);
                        if (imageNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element imageElement = (Element) imageNode;
                            String imageSizeString = imageElement.getAttribute("size");
                            IMAGE_SIZE imageSize;
                            try {
                                imageSize = IMAGE_SIZE.valueOf(imageSizeString.toUpperCase());
                                if (imageSize.val > mBestImageSize.val) {
                                    String imageUrlLocation = imageElement.getTextContent();
//                                    Log.d(TAG, "considering image with size " + imageSizeString + ": " + imageUrlLocation);
                                    mBestUrlString = imageUrlLocation;
                                    mBestImageSize = imageSize;
                                }
                            } catch (IllegalArgumentException e) {
                                //this will generate an exception for all but large, extralarge, and mega images
//                                Log.d(TAG, "skipping image with size: " + imageSizeString);
                            }
                        }
                    }
                }
            }
        } catch (ParserConfigurationException | IOException | SAXException | NullPointerException e) {

        }
    }

    private Bitmap downloadArtwork(String url) {
        if (url == null) {
            return null;
        }

        Bitmap bitmap = null;
        URL imageUrl;
        try {
            imageUrl = new URL(url);

            String urlSource = imageUrl.toString();

            HttpGet httpRequest = null;

            httpRequest = new HttpGet(imageUrl.toURI());

            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);

            HttpEntity entity = response.getEntity();
            BufferedHttpEntity bufferedHttpEntity = new BufferedHttpEntity(entity);
            InputStream input = bufferedHttpEntity.getContent();

            bitmap = BitmapFactory.decodeStream(input);
            input.close();

        } catch (Exception e) {
            Log.e(TAG, "caught an exception while trying to download artwork from " +
                    url + " " + e.getMessage());
        }
        return bitmap;
    }

    private enum IMAGE_SIZE {
        UNDEFINED("undefined", -1),
        LARGE("small", 0),
        EXTRALARGE("extralarge", 1),
        MEGA("mega", 2);

        public final String stringVal;
        public final int val;

        IMAGE_SIZE(String s, int v) {
            this.stringVal = s;
            this.val = v;
        }
    }
}
