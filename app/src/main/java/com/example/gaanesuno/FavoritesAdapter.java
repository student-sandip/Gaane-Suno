package com.example.gaanesuno;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import java.util.ArrayList;

public class FavoritesAdapter extends BaseAdapter {

    public interface OnSongActionListener {
        void onPlay(OnlineSong song);
        void onRemove(OnlineSong song);
    }

    private final Context context;
    private final ArrayList<OnlineSong> list;
    private final OnSongActionListener listener;
    private int highlightedIndex = -1;

    public FavoritesAdapter(Context context, ArrayList<OnlineSong> list, OnSongActionListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        ImageView songImage;
        TextView songTitle, songArtist;
        ImageButton removeBtn;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.favorite_item, parent, false);
            holder = new ViewHolder();
            holder.songImage = convertView.findViewById(R.id.songImage);
            holder.songTitle = convertView.findViewById(R.id.songTitle);
            holder.songArtist = convertView.findViewById(R.id.songArtist);
            holder.removeBtn = convertView.findViewById(R.id.removeBtn);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        OnlineSong song = list.get(position);

        // ✅ Load image safely
        if (song.getImageUrl() != null && !song.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(song.getImageUrl())
                    .placeholder(R.drawable.ic_album_placeholder)
                    .into(holder.songImage);
        } else {
            holder.songImage.setImageResource(R.drawable.ic_album_placeholder);
        }

        // ✅ Set text
        holder.songTitle.setText(song.getTitle());
        holder.songArtist.setText(song.getArtist());
        holder.songTitle.setSelected(true);
        holder.songArtist.setSelected(true);

        // ✅ Highlight current playing song (like offline)
        if (position == highlightedIndex) {
            holder.songTitle.setTextColor(Color.parseColor("#FF5722"));
            holder.songArtist.setTextColor(Color.parseColor("#FF5722"));
            holder.songTitle.setTypeface(null, Typeface.BOLD);
            holder.songArtist.setTypeface(null, Typeface.BOLD);
        } else {
            holder.songTitle.setTextColor(Color.parseColor("#FFFFFF"));
            holder.songArtist.setTextColor(Color.parseColor("#B3B3B3"));
            holder.songTitle.setTypeface(null, Typeface.NORMAL);
            holder.songArtist.setTypeface(null, Typeface.NORMAL);
        }

        // ✅ Play song on item click
        convertView.setOnClickListener(v -> listener.onPlay(song));

        // ✅ Remove favorite with confirmation
        holder.removeBtn.setImageResource(R.drawable.ic_heart_filled); // Initially filled heart
        holder.removeBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Remove Favorite")
                    .setMessage("Are you sure you want to remove this song from favorites?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        list.remove(position);
                        notifyDataSetChanged();
                        Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
                        holder.removeBtn.setImageResource(R.drawable.ic_heart_outline); // Empty heart
                        listener.onRemove(song);
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        return convertView;
    }

    // ✅ For highlighting current playing song
    public void setHighlightedIndex(int index) {
        this.highlightedIndex = index;
        notifyDataSetChanged();
    }
}
