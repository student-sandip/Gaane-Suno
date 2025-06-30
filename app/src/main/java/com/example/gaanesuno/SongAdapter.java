package com.example.gaanesuno;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SongAdapter extends BaseAdapter {

    private final Context context;
    private final ArrayList<Song> songs;
    private final LayoutInflater inflater;
    private int currentlyPlayingPosition = -1;
    private final Set<Integer> selectedItems = new HashSet<>();
    private boolean isSelectionMode = false;

    public SongAdapter(Context context, ArrayList<Song> songs) {
        this.context = context;
        this.songs = songs;
        this.inflater = LayoutInflater.from(context);
    }

    public void setCurrentlyPlayingPosition(int position) {
        this.currentlyPlayingPosition = position;
        notifyDataSetChanged();
    }

    public void setSelectedItems(Set<Integer> selected) {
        selectedItems.clear();
        selectedItems.addAll(selected);
        isSelectionMode = !selectedItems.isEmpty();
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

    private static class ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView artist;
        CheckBox checkbox;
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
            holder.checkbox = convertView.findViewById(R.id.songCheckbox);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Song song = songs.get(position);
        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());

        holder.title.setSelected(true);
        holder.artist.setSelected(true);

        String albumArtUri = song.getAlbumArtUri();
        if (albumArtUri != null && !albumArtUri.isEmpty()) {
            Glide.with(context)
                    .load(Uri.parse(albumArtUri))
                    .placeholder(R.drawable.ic_album_art)
                    .error(R.drawable.ic_album_art)
                    .into(holder.thumbnail);
        } else {
            holder.thumbnail.setImageResource(R.drawable.ic_album_art);
        }

        if (position == currentlyPlayingPosition) {
            holder.title.setTextColor(Color.parseColor("#FF5722"));
            holder.artist.setTextColor(Color.parseColor("#FF5722"));
            holder.title.setTypeface(null, Typeface.BOLD);
            holder.artist.setTypeface(null, Typeface.BOLD);
            holder.title.setPaintFlags(holder.title.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            holder.artist.setPaintFlags(holder.artist.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            convertView.setBackgroundColor(ContextCompat.getColor(context, R.color.songPlayingBackground));
        } else {
            holder.title.setTextColor(Color.WHITE);
            holder.artist.setTextColor(Color.GRAY);
            holder.title.setTypeface(null, Typeface.NORMAL);
            holder.artist.setTypeface(null, Typeface.NORMAL);
            holder.title.setPaintFlags(holder.title.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
            holder.artist.setPaintFlags(holder.artist.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }

        if (isSelectionMode) {
            holder.checkbox.setVisibility(View.VISIBLE);
            holder.checkbox.setChecked(selectedItems.contains(position));
        } else {
            holder.checkbox.setVisibility(View.GONE);
        }


        return convertView;
    }
}
