package com.example.gaanesuno;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SongAdapter extends BaseAdapter {

    private final Context context;
    private final ArrayList<Song> songs;
    private final LayoutInflater inflater;
    private int currentlyPlayingPosition = -1;
    private Set<Song> selectedItems = new HashSet<>();
    private boolean isSelectionMode = false;

    public SongAdapter(Context context, ArrayList<Song> songs, Set<Song> selectedItems) {
        this.context = context;
        this.songs = songs;
        this.selectedItems = selectedItems;
        this.inflater = LayoutInflater.from(context);
    }

    public void setCurrentlyPlayingPosition(int position) {
        this.currentlyPlayingPosition = position;
        notifyDataSetChanged();
    }

    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        notifyDataSetChanged();
    }

    public void setSelectedItems(Set<Song> selected) {
        this.selectedItems = selected;
        notifyDataSetChanged();
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

    @Override
    public boolean hasStableIds() {
        return false;
    }

    private static class ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView artist;
        ImageView checkmark;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.song_item, parent, false);
            holder = new ViewHolder();
            holder.thumbnail = convertView.findViewById(R.id.albumArt);
            holder.title = convertView.findViewById(R.id.songTitle);
            holder.artist = convertView.findViewById(R.id.songArtist);
            holder.checkmark = convertView.findViewById(R.id.checkmark);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Song song = songs.get(position);

        holder.title.setText(song.getTitle() != null ? song.getTitle() : "Unknown Title");
        holder.artist.setText(song.getArtist() != null ? song.getArtist() : "Unknown Artist");

        holder.title.setSelected(true);
        holder.artist.setSelected(true);

        String albumArtUri = song.getAlbumArtUri();
        Glide.with(context)
                .load(albumArtUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_album_placeholder)
                .error(R.drawable.ic_album_placeholder)
                .into(holder.thumbnail);

        int originalIndex = -1;
        if (MusicState.songList != null) {
            originalIndex = MusicState.songList.indexOf(song);
        }


        if (originalIndex != -1 && originalIndex == MusicState.currentlyPlayingPosition) {
             holder.title.setTextColor(ContextCompat.getColor(context, R.color.orange_accent));
             holder.artist.setTextColor(ContextCompat.getColor(context, R.color.orange_accent));
        } else {
            holder.title.setTextColor(Color.WHITE);
            holder.artist.setTextColor(Color.GRAY);
        }

        if (isSelectionMode && selectedItems.contains(song)) {
            holder.checkmark.setVisibility(View.VISIBLE);
            holder.checkmark.setColorFilter(ContextCompat.getColor(context, R.color.orange_accent), PorterDuff.Mode.SRC_IN);
        } else {
            holder.checkmark.setVisibility(View.GONE);
        }

        return convertView;
    }
}
