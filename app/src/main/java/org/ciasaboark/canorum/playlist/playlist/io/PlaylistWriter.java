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

package org.ciasaboark.canorum.playlist.playlist.io;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import org.ciasaboark.canorum.playlist.playlist.StaticPlaylist;
import org.ciasaboark.canorum.song.Track;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Jonathan Nelson on 3/6/15.
 */
public class PlaylistWriter {
    private static final String TAG = "PlayQueueSaver";
    private static final String PLAYLIST_DIR = "/playlists";
    private final Context mContext;
    private PlaylistWriterListener mListener;
    private StaticPlaylist mPlaylist;

    public PlaylistWriter(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }

        mContext = ctx;
    }

    public static void writePlaylistToZip(StaticPlaylist playlist, File file) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));

        ZipEntry name = new ZipEntry("meta");
        zos.putNextEntry(name);
        ByteArrayOutputStream metaBos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(metaBos);
        oos.writeObject(playlist.getPlaylistMetadata());
        oos.close();
        byte[] metaBytes = metaBos.toByteArray();
        zos.write(metaBytes);
        zos.closeEntry();
        //not closing object output stream since that would also close the zip output stream

        ZipEntry data = new ZipEntry("data");
        zos.putNextEntry(data);
        ByteArrayOutputStream dataBos = new ByteArrayOutputStream();
        ObjectOutputStream oos2 = new ObjectOutputStream(dataBos);
        oos2.writeObject(playlist);
        oos2.close();
        byte[] dataBytes = dataBos.toByteArray();
        zos.write(dataBytes);
        zos.closeEntry();
        zos.close();
        //not closing object output stream since that would also close the zip output stream
    }

    public PlaylistWriter setListener(PlaylistWriterListener listener) {
        mListener = listener;
        return this;
    }

    public PlaylistWriter saveTrackList(List<Track> trackList) {
        if (trackList == null || trackList.isEmpty()) {
            Log.d(TAG, "will not write null or empty tracklist to disk");
            sendResult(false, "will not write empty list");
        } else {
            mPlaylist = new StaticPlaylist("temp name", System.currentTimeMillis());
            mPlaylist.setTracklist(trackList);
            beginPlaylistSave();
        }
        return this;
    }

    private void sendResult(boolean fileWritten, String message) {
        if (mListener != null) {
            mListener.onPlaylistWritten(fileWritten, message);
        }
    }

    private void beginPlaylistSave() {
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
                String playlistName = editText.getText().toString();
                if (playlistName != null && !playlistName.equals("")) {
                    mPlaylist.setName(playlistName);
                    beginWriteFile();
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

    private void beginWriteFile() {
        //TODO check for playlists with same name
        try {
            File playlistFolder = new File(mContext.getExternalFilesDir(null), PLAYLIST_DIR);
            if (!playlistFolder.exists()) {
                playlistFolder.mkdirs();
            }
            boolean foundUniqueFileName = false;
            File playlistFile = null;
            while (!foundUniqueFileName) {
                String filename = generateFileName();
                playlistFile = new File(playlistFolder, filename + ".plst");
                if (!playlistFile.exists()) {
                    foundUniqueFileName = true;
                }
            }

            writePlayListFile(playlistFile);
        } catch (Exception e) {
            String errorMessage = "error writing playlist " + mPlaylist.getName() + " to disk: " + e.getMessage();
            Log.e(TAG, errorMessage);
            sendResult(false, errorMessage);
        }
    }

    private String generateFileName() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

//    private void writePlayListFile(File file) {
//        try {
//            PlaylistWriter.writePlaylistToZip(mPlaylist, file);
//            sendResult(true, "file written");
//        } catch (Exception e) {
//            sendResult(false, "error writing file: " + e.getMessage());
//        }
//    }

    private void writePlayListFile(File file) {
        FileOutputStream outputStream = null;
        ObjectOutputStream objectOutputStream = null;

        try {
            outputStream = new FileOutputStream(file);
            objectOutputStream = new ObjectOutputStream(outputStream);
            //write the metadata first so that it can be retrieved first
            objectOutputStream.writeObject(mPlaylist.getPlaylistMetadata());
            objectOutputStream.writeObject(mPlaylist);

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
