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

package org.ciasaboark.canorum.artwork.artist.fetcher;

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
import org.ciasaboark.canorum.song.Artist;
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
    private static final String queryServer = "http://ws.audioscrobbler.com/2.0/?method=artist.getInfo";
    private static String api_key = "7bd3f54a7adae5681fe949279609b391";
    // Please provide your consumer secret here
    private static String consumer_secret = "";
    private final Context mContext;
    private Bitmap bestKnownBitmap = null;
    private LoadingWatcher mWatcher;
    private Artist mArtist;


    public LastFmImageFetcher(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
    }

    public LastFmImageFetcher setArtist(Artist artist) {
        mArtist = artist;
        return this;
    }

    public LastFmImageFetcher setLoadingWatcher(LoadingWatcher watcher) {
        mWatcher = watcher;
        return this;
    }

    public LastFmImageFetcher loadInBackground() {
        Log.d(TAG, "(" + mArtist + ") beginning last.fm artist art fetch");
        if (mArtist != null && mWatcher != null) {
            checkSongAndBeginConnection();
        } else {
            Log.e(TAG, "will not begin search for artwork without both a song and watcher given");
        }
        return this;
    }

    private void checkSongAndBeginConnection() {
        checkConnectionAndLoadArtwork();
    }

    private void checkConnectionAndLoadArtwork() {
        if (Util.isConnectedToNetwork(mContext)) {
            Log.d(TAG, "(" + mArtist + ") internet connection appears valid, fetching artwork list");
            GetArtworkListTask getArtworkListTask = new GetArtworkListTask();
            getArtworkListTask.execute(mArtist);
        } else {
            Log.e(TAG, "(" + mArtist + ") network does not appear to be connected, can not search for album art");
            mWatcher.onLoadFinished(null, null);
        }
    }

//    private boolean isBitmapMostlySquare(Bitmap bitmap) {
//        boolean isBitmapSquare = false;
//        if (bitmap != null) {
//            int width = bitmap.getWidth();
//            int height = bitmap.getHeight();
//            float ratio = width / height;
//            float lowerRatio = 0.8f;
//            float upperRatio = 1.25f;
//            if (lowerRatio <= ratio && ratio <= upperRatio) {
//                isBitmapSquare = true;
//            }
//        }
//
//        return isBitmapSquare;
//    }

    public void processBitmap(Bitmap bitmap, String bitmapSource) {
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

    private enum IMAGE_SIZE {
        UNDEFINED("undefined", -1),
        SMALL("small", 0),
        MEDIUM("medium", 1),
        LARGE("large", 0),
        EXTRALARGE("extralarge", 1),
        MEGA("mega", 2);

        public final String stringVal;
        public final int val;

        IMAGE_SIZE(String s, int v) {
            this.stringVal = s;
            this.val = v;
        }

    }

    private class GetArtworkListTask extends AsyncTask<Artist, Void, Void> {
        private String bestUrlString = null;
        private IMAGE_SIZE bestImageSize = IMAGE_SIZE.UNDEFINED;

        @Override
        protected Void doInBackground(Artist... artists) {

            // params comes from the execute() call: params[0] is the url.
            Artist artist = artists[0];

            try {
                String artistName = artist.getArtistName();
                artistName = URLEncoder.encode(artistName, "UTF-8").replace("+", "%20");

                String queryUrl = queryServer + "&artist=" + artistName +
                        "&api_key=" + api_key;
                Log.d(TAG, "(" + mArtist + ") fetching artist information from " + queryUrl);
                URL url = new URL(queryUrl);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.addRequestProperty("Referer", "https://github.com/ciasaboark/Canorum");

                String line;
                StringBuilder builder = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }

                Log.d(TAG, "(" + mArtist + ") full xml response: " + builder.toString());
                connection.disconnect();
                reader = null;

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                InputSource is = new InputSource(new StringReader(builder.toString()));
                Document document = db.parse(is);
                document.getDocumentElement().normalize();

                Element docElement = document.getDocumentElement();
                NodeList nodeList = docElement.getElementsByTagName("artist");
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
                                        Log.d(TAG, "(" + mArtist + ") considering image with size " + imageSizeString + ": " + imageUrlLocation);
                                        bestUrlString = imageUrlLocation;
                                        bestImageSize = imageSize;
                                    }
                                } catch (IllegalArgumentException e) {
                                    //this will generate an exception for all but large, extralarge, and mega images
                                    Log.d(TAG, "(" + mArtist + ") skipping image with size: " + imageSizeString);
                                }
                            }
                        }
                    }
                }

                if (bestUrlString == null) {
                    Log.d(TAG, "(" + mArtist + ") unable to find an artist image that is at least large size");
                } else {
                    Log.d(TAG, "(" + mArtist + ") fetching image with size " + bestImageSize + " from " + bestUrlString);
                    URL imageUrl = new URL(bestUrlString);
                    DownloadArtworkTask downloadArtworkTask = new DownloadArtworkTask();
                    downloadArtworkTask.execute(imageUrl);
                }
            } catch (Exception e) {
                //TODO
                Log.d(TAG, "(" + mArtist + ") something went wrong getting artist art from last.fm " + e.getMessage());
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
                Log.e(TAG, "(" + mArtist + ") caught an exception while trying to load artist artwork from " +
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
