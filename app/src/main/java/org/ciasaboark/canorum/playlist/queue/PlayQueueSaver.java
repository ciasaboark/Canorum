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
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import org.ciasaboark.canorum.song.Track;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * Created by Jonathan Nelson on 3/6/15.
 */
public class PlayQueueSaver {
    private static final String TAG = "PlayQueueSaver";
    private static final String PLAYLIST_DIR = "/playlists";
    private final Context mContext;
    private PlaylistWriterListener mListener;
    private List<Track> mTrackList;

    public PlayQueueSaver(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }

        mContext = ctx;
    }

    public PlayQueueSaver setListener(PlaylistWriterListener listener) {
        mListener = listener;
        return this;
    }

    public PlayQueueSaver saveQueue(List<Track> trackList) {
        if (trackList == null || trackList.isEmpty()) {
            Log.d(TAG, "will not write null or empty tracklist to disk");
            sendResult(false, "will not write empty list");
        } else {
            mTrackList = trackList;
            beginPlayListSave();
        }
        return this;
    }

    private void sendResult(boolean fileWritten, String message) {
        if (mListener != null) {
            mListener.onPlaylistWritten(fileWritten, message);
        }
    }

    private void beginPlayListSave() {
        String result = null;

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setCancelable(false);
        builder.setTitle("Save as");
        final EditText editText = new EditText(mContext);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(editText);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String filename = editText.getText().toString();
                if (filename != null && !filename.equals("")) {
                    beginWriteFile(filename);
                } else {
                    sendResult(false, "empty file name can not be written");
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendResult(false, "user canceled");
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void beginWriteFile(String filename) {
        try {
            File playlistFolder = new File(mContext.getExternalFilesDir(null), PLAYLIST_DIR);
            if (!playlistFolder.exists()) {
                playlistFolder.mkdirs();
            }
            final File playListFile = new File(playlistFolder, filename + ".plst");
            if (playListFile.exists()) {
                //the playlist already exists, prompt if the user wants to overwrite
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setCancelable(false);
                builder.setTitle("Save as");
                builder.setMessage("A playlist named " + filename + " already exists, overwrite?");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        writePlayListFile(playListFile);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendResult(false, "user canceled");
                        dialog.cancel();
                    }
                });
                builder.show();
            } else {
                writePlayListFile(playListFile);
            }
        } catch (Exception e) {
            String errorMessage = "error writing file " + filename + " to disk: " + e.getMessage();
            Log.e(TAG, errorMessage);
            sendResult(false, errorMessage);
        }
    }

    private void writePlayListFile(File file) {
        FileOutputStream outputStream = null;
        ObjectOutputStream objectOutputStream = null;

        try {
            outputStream = new FileOutputStream(file);
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(mTrackList);

            sendResult(true, "file successfully written");
            objectOutputStream.close();
            outputStream.close();
        } catch (Exception e) {
            String errorMessage = "error writing file " + file + " to disk: " + e.getMessage();
            Log.e(TAG, errorMessage);
            sendResult(false, errorMessage);
        } finally {
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "error closing object output stream, the close guard should clean this up");
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "error closing output stream, the close guard should clean this up");
                }
            }
        }
    }

    public interface PlaylistWriterListener {
        public void onPlaylistWritten(boolean playListWritten, String message);
    }
}
