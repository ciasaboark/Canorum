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

package org.ciasaboark.canorum.artwork.albumart.fetcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.ciasaboark.canorum.artwork.Util;
import org.ciasaboark.canorum.artwork.watcher.LoadingWatcher;
import org.ciasaboark.canorum.prefs.RatingsPrefs;
import org.ciasaboark.canorum.song.Album;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by Jonathan Nelson on 1/29/15.
 */
public class LastFmImageFetcher {
    private static final String TAG = "LastFmImageFetcher";
    private static final String queryServer = "http://ws.audioscrobbler.com/2.0/?method=album.getInfo";
    private static String api_key = "7bd3f54a7adae5681fe949279609b391";
    // Please provide your consumer secret here
    private static String consumer_secret = "";
    private final RatingsPrefs ratingsPrefs;
    private final Context mContext;
    private Bitmap bestKnownBitmap = null;
    private LoadingWatcher mWatcher;
    private Album mAlbum;


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

    public LastFmImageFetcher setLoadingWatcher(LoadingWatcher watcher) {
        mWatcher = watcher;
        return this;
    }

    public LastFmImageFetcher loadInBackground() {
        Log.d(TAG, "beginning last.fm album art fetch");
        if (mAlbum != null && mWatcher != null) {
            checkAlbumAndBeginConnection();
        } else {
            Log.e(TAG, "will not begin search for artwork without both a album and watcher given");
        }
        return this;
    }

    private void checkAlbumAndBeginConnection() {
        if (Util.isAlbumValid(mAlbum)) {
            Log.d(TAG, "album appears valid, checking internet connection");
            checkConnectionAndLoadArtwork();
        } else {
            Log.d(TAG, "aborting search for album art, album '" + mAlbum + "' does not have " +
                    "proper artist and/or album field");
            mWatcher.onLoadFinished(null, null);
        }
    }

    private void checkConnectionAndLoadArtwork() {
        if (Util.isConnectedToNetwork(mContext)) {
            Log.d(TAG, "internet connection appears valid, fetching artwork list");
            GetArtworkListTask getArtworkListTask = new GetArtworkListTask();
            getArtworkListTask.execute(mAlbum);
        } else {
            Log.e(TAG, "network does not appear to be connected, can not search for album art");
            mWatcher.onLoadFinished(null, null);
        }
    }

    public void processBitmap(Bitmap bitmap, String bitmapSource) {
        if (!isBitmapMostlySquare(bitmap)) {
            Log.d(TAG, "provided bitmap is not (mostly) square, discarding");
        } else {
            if (bestKnownBitmap == null) {
                bestKnownBitmap = bitmap;
                BitmapDrawable bitmapDrawable = new BitmapDrawable(bestKnownBitmap);
                mWatcher.onLoadFinished(bitmapDrawable, bitmapSource);
            } else if (bitmap != null && bestKnownBitmap != null) {
                int dimensions = bitmap.getWidth() * bitmap.getHeight();
                int bestDimensions = bestKnownBitmap.getWidth() * bestKnownBitmap.getHeight();

                if (dimensions > bestDimensions) {
                    bestKnownBitmap = bitmap;
                    BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
                    mWatcher.onLoadFinished(bitmapDrawable, bitmapSource);
                }
            }
        }
    }

    private boolean isBitmapMostlySquare(Bitmap bitmap) {
        boolean isBitmapSquare = false;
        if (bitmap != null) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float ratio = width / height;
            float lowerRatio = 0.8f;
            float upperRatio = 1.25f;
            if (lowerRatio <= ratio && ratio <= upperRatio) {
                isBitmapSquare = true;
            }
        }

        return isBitmapSquare;
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

    private class GetArtworkListTask extends AsyncTask<Album, Void, Void> {
        private String bestUrlString = null;
        private IMAGE_SIZE bestImageSize = IMAGE_SIZE.UNDEFINED;

        @Override
        protected Void doInBackground(Album... albums) {

            // params comes from the execute() call: params[0] is the url.
            Album a = albums[0];

            try {
                String artist = a.getArtistName();
                artist = URLEncoder.encode(artist, "UTF-8").replace("+", "%20");

                String album = a.getAlbumName();
                album = URLEncoder.encode(album, "UTF-8").replace("+", "%20");


                String queryUrl = queryServer + "&artist=" + artist + "&album=" + album +
                        "&api_key=" + api_key;
                Log.d(TAG, "fetching album information from " + queryUrl);
                URL url = new URL(queryUrl);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.addRequestProperty("Referer", "https://github.com/ciasaboark/Canorum");

                String line;
                StringBuilder builder = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }

                Log.d(TAG, "full xml response: " + builder.toString());
                connection.disconnect();
                reader = null;

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                InputSource is = new InputSource(new StringReader(builder.toString()));
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
                                    if (imageSize.val > bestImageSize.val) {
                                        String imageUrlLocation = imageElement.getTextContent();
                                        Log.d(TAG, "considering image with size " + imageSizeString + ": " + imageUrlLocation);
                                        bestUrlString = imageUrlLocation;
                                        bestImageSize = imageSize;
                                    }
                                } catch (IllegalArgumentException e) {
                                    //this will generate an exception for all but large, extralarge, and mega images
                                    Log.d(TAG, "skipping image with size: " + imageSizeString);
                                }
                            }
                        }
                    }
                }

                if (bestUrlString == null) {
                    Log.d(TAG, "unable to find an album image that is at least large size");
                } else {
                    Log.d(TAG, "fetching image with size " + bestImageSize + " from " + bestUrlString);
                    URL imageUrl = new URL(bestUrlString);
                    DownloadArtworkTask downloadArtworkTask = new DownloadArtworkTask();
                    downloadArtworkTask.execute(imageUrl);
                }
            } catch (Exception e) {
                //TODO
                Log.e(TAG, "something went wrong getting album art from last.fm " + e.getMessage());
                e.printStackTrace();
                //try to let the watcher know that nothing will be returned
                mWatcher.onLoadFinished(null, null);
            }

            return null;
        }


        @Override
        protected void onPostExecute(Void result) {
        }
    }

    private class DownloadArtworkTask extends AsyncTask<URL, Void, Bitmap> {
        private String urlSource;

        @Override
        protected Bitmap doInBackground(URL... urls) {
            Bitmap bitmap = null;
            URL artUrl = urls[0];
            urlSource = artUrl.toString();

            try {
                HttpGet httpRequest = null;

                httpRequest = new HttpGet(artUrl.toURI());

                HttpClient httpclient = new DefaultHttpClient();
                HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);

                HttpEntity entity = response.getEntity();
                BufferedHttpEntity b_entity = new BufferedHttpEntity(entity);
                InputStream input = b_entity.getContent();

                bitmap = BitmapFactory.decodeStream(input);
                input.close();

            } catch (Exception e) {
                Log.e(TAG, "caught an exception while trying to load artwork from " +
                        artUrl + " " + e.getMessage());
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            processBitmap(result, urlSource);
        }

    }
}
