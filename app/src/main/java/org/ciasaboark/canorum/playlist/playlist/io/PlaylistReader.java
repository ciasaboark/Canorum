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
import android.util.Log;

import org.ciasaboark.canorum.playlist.playlist.Playlist;
import org.ciasaboark.canorum.playlist.playlist.StaticPlaylist;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jonathan Nelson on 3/6/15.
 */
public class PlaylistReader {
    private static final String TAG = "PlayQueueReader";
    private static final String PLAYLIST_DIR = "/playlists";
    private final Context mContext;
    private PlaylistReaderListener mListener;

//    public static StaticPlaylist readPlaylistFromFile(File file) throws IOException {
//        ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
//        StaticPlaylist staticPlaylist = null;
//        Playlist.PlaylistMetadata metadata = null;
//        ObjectInputStream metaObjectInputStream = null;
//        ObjectInputStream dataObjectInputStream = null;
//        ZipEntry entry;
//        try {
//            while ((entry = zis.getNextEntry()) != null) {
//                switch (entry.getName()) {
//                    case "meta":
//                        metaObjectInputStream = new ObjectInputStream(zis);
//                        metadata = (Playlist.PlaylistMetadata) metaObjectInputStream.readObject();
//                        break;
//                    case "data":
//                        dataObjectInputStream = new ObjectInputStream(zis);
//                        staticPlaylist = (StaticPlaylist) dataObjectInputStream.readObject();
//                        break;
//                    default:
//                        Log.e(TAG, "unknown field in the zip file: " + entry.getName());
//                }
//            }
//        } catch (ClassNotFoundException e) {
//            //nothing to do here, we will catch the error below when some fields are still null
//        } finally {
//            if (metaObjectInputStream != null) {
//                metaObjectInputStream.close();
//            }
//            if (dataObjectInputStream != null) {
//                dataObjectInputStream.close();
//            }
//            if (zis != null) {
//                zis.close();
//            }
//        }
//
//        if (staticPlaylist == null || metadata == null) {
//            throw new IOException("unknown file format, missing one of metadata or list data");
//        }
//
//        staticPlaylist.setPlaylistMetadata(metadata);
//        return staticPlaylist;
//    }

    public PlaylistReader(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        mContext = ctx;
    }

    public static File getPlayListDirectory(Context ctx) {
        return new File(ctx.getExternalFilesDir(null), PLAYLIST_DIR);
    }

    public PlaylistReader setPlayListReaderListener(PlaylistReaderListener listener) {
        mListener = listener;
        return this;
    }

    public PlaylistReader showOpenDialog() {
        Map<Playlist.PlaylistMetadata, File> playlistFiles = findAvailablePlaylists();
        if (playlistFiles.isEmpty()) {
            showNoPlaylistAvailableDialog();
        } else {
            buildAndDisplayOpenDialog(playlistFiles);
        }

        return this;
    }

    private Map<Playlist.PlaylistMetadata, File> findAvailablePlaylists() {
        Map<Playlist.PlaylistMetadata, File> availablePlaylists = new HashMap<Playlist.PlaylistMetadata, File>();

        File playListFolder = PlaylistReader.getPlayListDirectory(mContext);
        File[] files = playListFolder.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                if (file.getName().endsWith(".plst")) {
                    FileInputStream fis = null;
                    ObjectInputStream ois = null;
                    try {
                        fis = new FileInputStream(file);
                        ois = new ObjectInputStream(fis);
                        Playlist.PlaylistMetadata metadata = (Playlist.PlaylistMetadata) ois.readObject();
                        ois.close();
                        fis.close();
                        availablePlaylists.put(metadata, file);
                    } catch (Exception e) {
                        Log.e(TAG, "can not read proper metadata header from file: " + file);
                    }
                }
            }
        }
        return availablePlaylists;
    }

    private void showNoPlaylistAvailableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Select playlist");
        builder.setMessage("No playlists could be found");

        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void buildAndDisplayOpenDialog(final Map<Playlist.PlaylistMetadata, File> playlistFiles) {
        //TODO this is a ridiculous way to get a hashmap in an alertdialog
        final List<Playlist.PlaylistMetadata> metadataList = new ArrayList<Playlist.PlaylistMetadata>(playlistFiles.keySet());
        //sort by playlist creation date
        Collections.sort(metadataList, new Comparator<Playlist.PlaylistMetadata>() {
            @Override
            public int compare(Playlist.PlaylistMetadata lhs, Playlist.PlaylistMetadata rhs) {
                return ((Long) lhs.getCreationTimeStamp()).compareTo(rhs.getCreationTimeStamp());
            }
        });

        final List<String> availablePlaylists = new ArrayList<String>();
        for (Playlist.PlaylistMetadata metadata : metadataList) {
            availablePlaylists.add(metadata.toString());
        }
        final String[] playlists = availablePlaylists.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Select playlist");
//        builder.setMessage("Open which playlist?");
        builder.setSingleChoiceItems(playlists, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Playlist.PlaylistMetadata metadata = metadataList.get(which);
                File playlistFile = playlistFiles.get(metadata);
                readPlayList(playlistFile);
            }
        });
        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                sendError("user canceled");
            }
        });
        builder.show();
    }

    public PlaylistReader readPlayList(File playlistFile) {
        if (playlistFile == null || playlistFile.equals("")) {
            sendError("can not load playlist with null or empty filename");
        } else {
            beginReadPlaylist(playlistFile);
        }
        return this;
    }

    private void sendError(String message) {
        if (mListener != null) {
            mListener.onPlayListReadError(message);
        }
    }

    private void beginReadPlaylist(File playlistFile) {
        try {
//            File playlistFolder = getPlayListDirectory();
//            File playlistFile = new File(playlistFolder, playlistFile + ".plst");
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

    private void deletePlaylistFile(File playlistFile) {
        playlistFile.delete();
    }

//    private void readPlayListFile(File playlistFile) {
//        try {
//            Playlist playlist = PlaylistReader.readPlaylistFromFile(playlistFile);
//            sendPlaylist(playlist);
//        } catch (Exception e) {
//            sendError("error reading from playlist file: " + e.getMessage());
//            showReadFailedDialog(playlistFile);
//        }
//    }

    private void showReadFailedDialog(final File playlistFile) {
        AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setTitle("Invalid format")
                .setMessage("The selected playlist could not be opened, its probably from an older version or the data has become corrupted.\nWould you like to delete this file?")
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        showOpenDialog();
                    }
                })
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deletePlaylistFile(playlistFile);
                        dialog.dismiss();
                        showOpenDialog();
                    }
                })
                .show();
    }

    private void readPlayListFile(final File playlistFile) {
        try {
            FileInputStream is = new FileInputStream(playlistFile);
            ObjectInputStream ois = new ObjectInputStream(is);
            Playlist.PlaylistMetadata metadata = (Playlist.PlaylistMetadata) ois.readObject();
            StaticPlaylist playlist = (StaticPlaylist) ois.readObject();
            playlist.setPlaylistMetadata(metadata);
            sendPlaylist(playlist);
        } catch (Exception e) {
            String errorMessage = "error opening playlist file: " + e.getMessage();
            Log.e(TAG, errorMessage);
            sendError(errorMessage);
            AlertDialog dialog = new AlertDialog.Builder(mContext)
                    .setTitle("Invalid format")
                    .setMessage("The selected playlist could not be opened, its probably from an older version or the data has become corrupted.\nWould you like to delete this file?")
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            showOpenDialog();
                        }
                    })
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            deletePlaylistFile(playlistFile);
                            showOpenDialog();
                        }
                    })
                    .show();
        }
    }

    private void sendPlaylist(Playlist playlist) {
        if (mListener != null) {
            mListener.onPlayListReadSuccess(playlist);
        }
    }

    public interface PlaylistReaderListener {
        public void onPlayListReadSuccess(Playlist playlist);

        public void onPlayListReadError(String message);
    }

}
