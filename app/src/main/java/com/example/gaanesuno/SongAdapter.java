package com.example.gaanesuno;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class SongAdapter extends BaseAdapter {

    private int currentlyPlayingPosition = -1;

    public void setCurrentlyPlayingPosition(int position) {
        this.currentlyPlayingPosition = position;
        notifyDataSetChanged();
    }

    private final Context context;
    private final ArrayList<Song> songs;
    private final LayoutInflater inflater;

    public SongAdapter(Context context, ArrayList<Song> songs) {
        this.context = context;
        this.songs = songs;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int position) {
        return songs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        TextView title;
        TextView artist;
        ImageView thumbnail;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.song_item, parent, false);
            holder = new ViewHolder();
            holder.title = convertView.findViewById(R.id.songTitle);
            holder.artist = convertView.findViewById(R.id.songArtist);
            holder.thumbnail = convertView.findViewById(R.id.albumArt);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Song song = songs.get(position);
        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());
        holder.title.setSelected(true); // Required for marquee to scroll
        holder.artist.setSelected(true);


        // Set album art (content:// URI or fallback)
        String albumArtUri = song.getAlbumArtUri();
        if (albumArtUri != null && !albumArtUri.isEmpty()) {
            Uri uri = Uri.parse(albumArtUri);
            holder.thumbnail.setImageURI(uri);
            if (holder.thumbnail.getDrawable() == null) {
                holder.thumbnail.setImageResource(R.drawable.ic_album_art);
            }
        } else {
            holder.thumbnail.setImageResource(R.drawable.ic_album_art);
        }

        // ✅ Highlight currently playing song
        if (position == currentlyPlayingPosition) {
            convertView.setBackgroundColor(Color.parseColor("#c71585")); // highlighted background
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT); // normal background
        }

        return convertView;
    }
}
