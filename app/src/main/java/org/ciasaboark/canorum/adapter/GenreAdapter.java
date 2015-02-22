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

package org.ciasaboark.canorum.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.song.Genre;

import java.util.List;

/**
 * Created by Jonathan Nelson on 2/19/15.
 */
public class GenreAdapter extends ArrayAdapter<Genre> implements FilterableAdapter<Genre> {
    private static final String TAG = "GenreAdapter";
    private Context mContext;
    private List<Genre> mData;

    public GenreAdapter(Context context, int resource, List<Genre> data) {
        super(context, resource, data);
        mData = data;
        mContext = context;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        GenreHolder holder = null;
        final Genre genre = getItem(pos);

        if (convertView != null) {
            holder = (GenreHolder) convertView.getTag();
        } else {
            holder = new GenreHolder();
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_genre, null);
            holder.genreImage = (ImageView) convertView.findViewById(R.id.genre_image);
            holder.genreText = (TextView) convertView.findViewById(R.id.genre_text);
            convertView.setTag(holder);
        }

        holder.position = pos;
        holder.genreText.setText(genre.getGenre());


        return convertView;
    }

    @Override
    public List<Genre> getFilteredList() {
        return mData;
    }

    private class GenreHolder {
        public ImageView genreImage;
        public TextView genreText;
        public int position;
    }
}
