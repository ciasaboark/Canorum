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

package org.ciasaboark.canorum.song.shadow;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.ciasaboark.canorum.song.Artist;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by Jonathan Nelson on 2/28/15.
 */
public class ShadowLibraryFetcher {
    private static final String TAG = "ShadowLibraryFetcher";
    private final Context mContext;
    private Artist mArtist;
    private ShadowLibraryLoadedListener mListener;
    private List<String> mAlbumTitlesList = new ArrayList<String>();

    public ShadowLibraryFetcher(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Context can not be null");
        }
        mContext = ctx;
    }

    public ShadowLibraryFetcher setArtist(Artist artist) {
        mArtist = artist;
        return this;
    }

    public ShadowLibraryFetcher setShadowLibraryListener(ShadowLibraryLoadedListener listener) {
        mListener = listener;
        return this;
    }

    public ShadowLibraryFetcher loadInBackground() {
        if (mListener == null || mArtist == null) {
            Log.e(TAG, "will not begin load until both an artist and a listener have been given");
        } else {
            ShadowLibraryBackgroundLoader backgroundLoader = new ShadowLibraryBackgroundLoader();
            backgroundLoader.execute(mArtist);
        }

        return this;
    }

    private class ShadowLibraryBackgroundLoader extends AsyncTask<Artist, Void, List<ShadowAlbum>> {
        private static final String TOP_ALBUMS_URI = "http://ws.audioscrobbler.com/2.0/?method=artist.gettopalbums";
        private static final String ALBUM_INFO_URI = "http://ws.audioscrobbler.com/2.0/?method=album.getinfo";
        private static final String API_KEY = "7bd3f54a7adae5681fe949279609b391";

        @Override
        protected List<ShadowAlbum> doInBackground(Artist... params) {
            Artist artist = params[0];
            String artistName = artist.getArtistName();
            List<ShadowAlbum> shadowAlbums = new ArrayList<ShadowAlbum>();

            //get a list of albums
            String albumListUrl;
            try {
                albumListUrl = getAlbumsQueryUrl(artistName);
            } catch (UnsupportedEncodingException e) {
                return shadowAlbums;
            }

            String response = readFromUrl(albumListUrl);
            Log.d(TAG, "full artist album list response: " + response);

            if (response == null) {
                return shadowAlbums;
            }

            buildAlbumTitlesList(response);

            //now that we have a list of album titles to work with we can begin
            // building a shadow library
            if (mAlbumTitlesList.isEmpty()) {
                Log.d(TAG, "no albums were found while building the shadow library, this is " +
                        "probably an error");
            } else {
                for (String albumName : mAlbumTitlesList) {
                    ShadowAlbum shadowAlbum = null;
                    List<ShadowSong> albumSongs = getAlbumSongs(artistName, albumName);

                    if (albumSongs.size() != 0) {
                        shadowAlbum = new ShadowAlbum(mArtist, albumName, -1, albumSongs);
                    }
                    if (shadowAlbum != null) {
                        shadowAlbums.add(shadowAlbum);
                    }
                }
            }


            return shadowAlbums;
        }

        private String getAlbumsQueryUrl(String artistName) throws UnsupportedEncodingException {
            String queryUrl = null;
            artistName = URLEncoder.encode(artistName, "UTF-8").replace("+", "%20");
            queryUrl = TOP_ALBUMS_URI + "&artist=" + artistName +
                    "&api_key=" + API_KEY;

            return queryUrl;
        }

        private String readFromUrl(String urlString) {
            String fullResponse = null;
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                Log.d(TAG, "fetching album information from " + urlString);
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
                //if something goes wrong during the read we will just return null
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

        private void buildAlbumTitlesList(String xml) {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

                DocumentBuilder db = dbf.newDocumentBuilder();
                InputSource is = new InputSource(new StringReader(xml));
                Document document = db.parse(is);
                document.getDocumentElement().normalize();

                Element docElement = document.getDocumentElement();
                NodeList albumNodeList = docElement.getElementsByTagName("album");
                if (albumNodeList != null && albumNodeList.getLength() > 0) {
                    for (int i = 0; i < albumNodeList.getLength(); i++) {
                        //get the album element
                        Element albumElement = (Element) albumNodeList.item(i);
                        String albumName = getTextValue(albumElement, "name");
                        if (albumName != null)
                            mAlbumTitlesList.add(albumName);
                    }
                }
            } catch (ParserConfigurationException | IOException | SAXException | NullPointerException e) {

            }
        }

        private List<ShadowSong> getAlbumSongs(String artistName, String albumName) {
            List<ShadowSong> albumSongs = new ArrayList<ShadowSong>();
            try {
                String albumInfoUrl = getAlbumInfoUrl(artistName, albumName);
                String albumInfoResponse = readFromUrl(albumInfoUrl);
                if (albumInfoResponse == null) {
                    Log.e(TAG, "unable to read album info response from " + albumInfoUrl);
                } else {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

                    DocumentBuilder db = dbf.newDocumentBuilder();
                    InputSource is = new InputSource(new StringReader(albumInfoResponse));
                    Document document = db.parse(is);
                    document.getDocumentElement().normalize();

                    Element docElement = document.getDocumentElement();
                    NodeList tracksNodeList = docElement.getElementsByTagName("tracks");
                    if (tracksNodeList != null && tracksNodeList.getLength() > 0) {
                        //there _should_ only be one tracks node, but just in case
                        for (int i = 0; i < tracksNodeList.getLength(); i++) {
                            Element tracksElement = (Element) tracksNodeList.item(i);
                            NodeList songNodeList = tracksElement.getElementsByTagName("track");
                            if (songNodeList != null && songNodeList.getLength() > 0) {
                                for (int j = 0; j < songNodeList.getLength(); j++) {
                                    Element songNode = (Element) songNodeList.item(j);
                                    String songNum = songNode.getAttributes().getNamedItem("rank").getNodeValue();
                                    String songName = getTextValue(songNode, "name");
                                    String songDurationSec = getTextValue(songNode, "duration");
                                    try {
                                        ShadowSong shadowSong = new ShadowSong(songName, Integer.parseInt(songNum), Integer.parseInt(songDurationSec));
                                        albumSongs.add(shadowSong);
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "error parsing integer value: " + e.getMessage());
                                    }

                                }
                            }
                        }
                    }
                }
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "something went wrong getting album info url string: " + e.getMessage());
            } catch (ParserConfigurationException | IOException | SAXException | NullPointerException e) {
                Log.e(TAG, "error building shadow album for " + artistName + " - " + albumName);
            }

            return albumSongs;
        }

        /**
         * I take a xml element and the tag name, look for the tag and get
         * the text content
         * i.e for <employee><name>John</name></employee> xml snippet if
         * the Element points to employee node and tagName is 'name' I will return John
         */
        private String getTextValue(Element ele, String tagName) {
            String textVal = null;
            NodeList nl = ele.getElementsByTagName(tagName);
            if (nl != null && nl.getLength() > 0) {
                Element el = (Element) nl.item(0);
                textVal = el.getFirstChild().getNodeValue();
            }

            return textVal;
        }

        private String getAlbumInfoUrl(String artistName, String albumName) throws UnsupportedEncodingException {
            String queryUrl = null;
            artistName = URLEncoder.encode(artistName, "UTF-8").replace("+", "%20");
            albumName = URLEncoder.encode(albumName, "UTF-8").replace("+", "%20");
            queryUrl = ALBUM_INFO_URI + "&artist=" + artistName + "&album=" + albumName +
                    "&api_key=" + API_KEY;

            return queryUrl;
        }

        @Override
        protected void onPostExecute(List<ShadowAlbum> shadowLibrary) {
            mListener.OnShadowLibraryLoaded(shadowLibrary);
        }

        /**
         * Calls getTextValue and returns a int value
         */
        private int getIntValue(Element ele, String tagName) {
            //in production application you would catch the exception
            return Integer.parseInt(getTextValue(ele, tagName));
        }
    }


}
