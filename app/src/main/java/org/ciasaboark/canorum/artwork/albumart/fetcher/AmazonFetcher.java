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
//package org.ciasaboark.canorum.artwork.albumart.fetcher;
//
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.graphics.drawable.BitmapDrawable;
//import android.os.AsyncTask;
//import android.util.Log;
//
//import com.amazonaws.AmazonWebServiceClient;
//
//import org.ciasaboark.canorum.song.Song;
//
//import java.net.URL;
//import java.util.Arrays;
//
///**
// * Created by Jonathan Nelson on 1/30/15.
// */
//public class AmazonFetcher {
//    private static final String TAG = "AmazonFetcher";
//    private final Context mContext;
//    private Song mSong;
//    private LoadingWatcher mWatcher;
//
//    public AmazonFetcher(Context ctx) {
//        if (ctx == null) {
//            throw new IllegalArgumentException("context can not be null");
//        }
//        mContext = ctx;
//    }
//
//    public AmazonFetcher setArtist(Song song) {
//        mSong = song;
//        return this;
//    }
//
//    public AmazonFetcher setArtLoadedWatcher(LoadingWatcher watcher) {
//        mWatcher = watcher;
//        return this;
//    }
//
//    public AmazonFetcher loadInBackground() {
//        //TODO
//        return this;
//    }
//
//    private class DownloadArtworkTask extends AsyncTask<Song, Void, Bitmap> {
//
//        @Override
//        protected Bitmap doInBackground(Song... songs) {
//            Song song = songs[0];
//
//            //TODO
//            // Set the service:
//
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Bitmap result) {
//            BitmapDrawable bitmapDrawable = null;
//            if (result != null)
//                bitmapDrawable = new BitmapDrawable(result);
//            mWatcher.onLoadFinished(bitmapDrawable, "amazon image search");
//        }
//    }
//}
