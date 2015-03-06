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

package org.ciasaboark.canorum.playlist.queue;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import org.apache.commons.io.FilenameUtils;
import org.ciasaboark.canorum.song.Track;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonathan Nelson on 3/6/15.
 */
public class PlayQueueReader {
    private static final String TAG = "PlayQueueReader";
    private static final String PLAYLIST_DIR = "/playlists";
    private final Context mContext;
    private PlaylistReaderListener mListener;

    public PlayQueueReader(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
    }

    public PlayQueueReader setPlayListReaderListener(PlaylistReaderListener listener) {
        mListener = listener;
        return this;
    }

    public PlayQueueReader showOpenDialog() {
        List<String> availablePlaylists = findAvailablePlaylists();
        if (availablePlaylists.isEmpty()) {
            showNoPlaylistAvailableDialog();
        } else {
            buildAndDisplayOpenDialog(availablePlaylists);
        }

        return this;
    }

    private List<String> findAvailablePlaylists() {
        List<String> availablePlaylists = new ArrayList<String>();
        File playListFolder = getPlayListDirectory();
        File[] files = playListFolder.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                if (file.getName().endsWith(".plst")) {
                    String nameSansExtension = FilenameUtils.removeExtension(file.getName());
                    availablePlaylists.add(nameSansExtension);
                }
            }
        }
        return availablePlaylists;
    }

    private void showNoPlaylistAvailableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setCancelable(false);
        builder.setTitle("Select playlist");
        builder.setMessage("No playlists could be found");

        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void buildAndDisplayOpenDialog(List<String> playListFiles) {
        final String[] playlists = playListFiles.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setCancelable(false);
        builder.setTitle("Select playlist");
//        builder.setMessage("Open which playlist?");
        builder.setSingleChoiceItems(playlists, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String selectedPlaylist = playlists[which];
                readPlayList(selectedPlaylist);
                dialog.cancel();
            }
        });
        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                sendError("user canceled");
                dialog.cancel();
            }
        });
        builder.show();
    }

    private File getPlayListDirectory() {
        return new File(mContext.getExternalFilesDir(null), PLAYLIST_DIR);
    }

    public PlayQueueReader readPlayList(String playlistFileName) {
        if (playlistFileName == null || playlistFileName.equals("")) {
            sendError("can not load playlist with null or empty filename");
        } else {
            beginReadPlaylist(playlistFileName);
        }
        return this;
    }

    private void sendError(String message) {
        if (mListener != null) {
            mListener.onPlayListReadError(message);
        }
    }

    private void beginReadPlaylist(String playlistFileName) {
        try {
            File playlistFolder = getPlayListDirectory();
            File playlistFile = new File(playlistFolder, playlistFileName + ".plst");
            if (!playlistFile.exists()) {
                sendError("file does not exists");
            } else {
                readPlayListFile(playlistFile);
            }
        } catch (Exception e) {
            String errorMessage = "error opening playlist file: " + e.getMessage();
            Log.e(TAG, errorMessage);
            sendError(errorMessage);
        }
    }

    private void readPlayListFile(File playlistFile) {
        try {
            FileInputStream is = new FileInputStream(playlistFile);
            ObjectInputStream ois = new ObjectInputStream(is);
            List<Track> trackList = (List<Track>) ois.readObject();
            sendPlaylist(trackList);
        } catch (Exception e) {
            String errorMessage = "error opening playlist file: " + e.getMessage();
            Log.e(TAG, errorMessage);
            sendError(errorMessage);
        }
    }

    private void sendPlaylist(List<Track> playlist) {
        if (mListener != null) {
            mListener.onPlayListReadSuccess(playlist);
        }
    }

    public interface PlaylistReaderListener {
        public void onPlayListReadSuccess(List<Track> playlist);

        public void onPlayListReadError(String message);
    }

}
