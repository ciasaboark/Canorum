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

package org.ciasaboark.canorum.playlist.playlist;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.ciasaboark.canorum.song.Track;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Jonathan Nelson on 3/6/15.
 */
public abstract class Playlist implements Serializable {
    private static final long serialVersionUID = -661761194117934500L;
    private transient PlaylistMetadata mMetadata;

    public Playlist(String name, long creationTimeStamp) {
        mMetadata = new PlaylistMetadata();
        mMetadata.mName = name;
        mMetadata.mCreationTimeStamp = creationTimeStamp;
    }

    public PlaylistMetadata getPlaylistMetadata() {
        return mMetadata;
    }

    public void setPlaylistMetadata(PlaylistMetadata metadata) {
        mMetadata = metadata;
    }

    public Bitmap getIcon() {
        return mMetadata.mIcon.getBitmap();
    }

    public void setIcon(Bitmap bitmap) {
        mMetadata.mIcon = new SerializedBitmap(bitmap);
    }

    public String getName() {
        return mMetadata.mName;
    }

    public void setName(String name) {
        mMetadata.mName = name;
    }

    public long getCreationTimeStamp() {
        return mMetadata.mCreationTimeStamp;
    }

    public void setCreationTimeStamp(long timestamp) {
        mMetadata.mCreationTimeStamp = timestamp;
    }

    public abstract List<Track> getTrackList();


    public class SerializedBitmap implements Serializable {
        private static final long serialVersionUID = 6424782270263717774L;
        public Bitmap mBitmap;

        public SerializedBitmap(Bitmap bitmap) {
            mBitmap = bitmap;
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }

        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.PNG, 0, byteStream);
            byte bitmapBytes[] = byteStream.toByteArray();
            out.write(bitmapBytes, 0, bitmapBytes.length);
        }

        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            int intByte;
            intByte = in.read();

            while (intByte != -1) {
                byteStream.write(intByte);
                intByte = in.read();
            }

            byte bitmapBytes[] = byteStream.toByteArray();
            mBitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
        }
    }

    public class PlaylistMetadata implements Serializable {
        private static final long serialVersionUID = -8145635129142000052L;
        private String mName = "";
        private long mCreationTimeStamp = -1;
        private SerializedBitmap mIcon = null;

        @Override
        public String toString() {
            Date date = new Date(mCreationTimeStamp);
            DateFormat formatter = DateFormat.getDateTimeInstance();
            return mName + " - " + formatter.format(date);
        }

        public String getName() {
            return mName;
        }

        public long getCreationTimeStamp() {
            return mCreationTimeStamp;
        }

        public Bitmap getIcon() {
            return mIcon.getBitmap();
        }
    }

}
