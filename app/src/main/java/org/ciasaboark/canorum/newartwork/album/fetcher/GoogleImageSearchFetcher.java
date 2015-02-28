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

package org.ciasaboark.canorum.newartwork.album.fetcher;

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
import org.ciasaboark.canorum.newartwork.Util;
import org.ciasaboark.canorum.newartwork.watcher.LoadingWatcher;
import org.ciasaboark.canorum.prefs.RatingsPrefs;
import org.ciasaboark.canorum.song.Album;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by Jonathan Nelson on 1/29/15.
 */
public class GoogleImageSearchFetcher {
    private static final String TAG = "GoogleImageSearchFetcher";
    private final RatingsPrefs ratingsPrefs;
    private final Context mContext;
    private Bitmap bestKnownBitmap = null;
    private LoadingWatcher mWatcher;
    private Album mAlbum;
    private SEARCH_LEVEL mLevel = SEARCH_LEVEL.HIGH;
    private IMAGE_SIZE mSize = IMAGE_SIZE.MEDIUM;

    public GoogleImageSearchFetcher(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
        ratingsPrefs = new RatingsPrefs(mContext);
    }

    public GoogleImageSearchFetcher setAlbum(Album album) {
        mAlbum = album;
        return this;
    }

    public GoogleImageSearchFetcher setLoadingWatcher(LoadingWatcher watcher) {
        mWatcher = watcher;
        return this;
    }

    public GoogleImageSearchFetcher setSafeSearchLevel(SEARCH_LEVEL level) {
        mLevel = level;
        return this;
    }

    public GoogleImageSearchFetcher setImageSize(IMAGE_SIZE size) {
        mSize = size;
        return this;
    }

    public GoogleImageSearchFetcher loadInBackground() {
        if (mAlbum != null && mWatcher != null) {
            Log.d(TAG, "beginning google image album art fetch");
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

    public enum SEARCH_LEVEL {
        OFF,
        MODERATE,
        HIGH;
    }

    public enum IMAGE_SIZE {
        MEDIUM,
        LARGE,
        XLARGE;
    }

    private class GetArtworkListTask extends AsyncTask<Album, Void, Void> {
        @Override
        protected Void doInBackground(Album... albums) {

            // params comes from the execute() call: params[0] is the url.
            Album album = albums[0];
            String safeSearch = "&safe=";
            if (mLevel == null) {
                safeSearch = "";
            } else {
                switch (mLevel) {
                    case OFF:
                        safeSearch += "off";
                        break;
                    case MODERATE:
                        safeSearch += "moderate";
                        break;
                    case HIGH:
                        safeSearch += "active";
                        break;
                }
            }

            String imageSize = "&imgsz=";
            if (mSize == null) {
                imageSize = "";
            } else {
                switch (mSize) {
                    case LARGE:
                        imageSize += "xxlarge";
                        break;
                    case XLARGE:
                        imageSize += "huge";
                        break;
                    case MEDIUM:
                        imageSize += "medium";
                        break;
                }
            }

            String resultsPerPage = "&rsz=8";
            try {
                String albumQuery = album.getAlbumName() + " " + album.getAlbumName() + " album";
                albumQuery = URLEncoder.encode(albumQuery, "UTF-8").replace("+", "%20");

                String queryUrl = "https://ajax.googleapis.com/ajax/services/search/images?" +
                        "v=1.0&q=" + albumQuery + "&ip=INSERT-USER-IP" + safeSearch + imageSize +
                        resultsPerPage;
                Log.d(TAG, "fetching list of images from " + queryUrl);
                URL url = new URL(queryUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.addRequestProperty("Referer", "https://github.com/ciasaboark/Canorum");

                String line;
                StringBuilder builder = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                connection.disconnect();
                reader = null;


                JSONObject jsonObject = new JSONObject(builder.toString());
                JSONObject responseData = jsonObject.getJSONObject("responseData");
                JSONArray jsonArray = responseData.getJSONArray("results");
                //try the first three results
                int bestDimensions = 0; //height * width
                //only try the first few results
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsObject = (JSONObject) jsonArray.get(i);
                    String artUrlLocation = (String) jsObject.get("url");
                    Log.d(TAG, "found artwork at " + artUrlLocation);
                    URL artworkURL = new URL(artUrlLocation);
                    DownloadArtworkTask downloadArtworkTask = new DownloadArtworkTask();
                    downloadArtworkTask.execute(artworkURL);

                }
            } catch (MalformedURLException e) {
                //TODO
                Log.e(TAG, e.getMessage());
            } catch (IOException e) {
                //TODO
                Log.e(TAG, e.getMessage());
            } catch (JSONException e) {
                //TODO
                Log.e(TAG, e.getMessage());
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
                BufferedHttpEntity bufferedHttpEntity = new BufferedHttpEntity(entity);
                InputStream input = bufferedHttpEntity.getContent();

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
