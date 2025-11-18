package com.example.gaanesuno;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class OnlineSongAdapter extends RecyclerView.Adapter<OnlineSongAdapter.OnlineSongViewHolder> {

    private final Context context;
    private final List<OnlineSong> songList;
    private final OnSongClickListener listener;
    private OnSongLongClickListener longClickListener;

    private int highlightedIndex = -1;

    public interface OnSongClickListener {
        void onSongClick(OnlineSong song);
    }

    public interface OnSongLongClickListener {
        void onSongLongClick(OnlineSong song);
    }

    public OnlineSongAdapter(Context context, List<OnlineSong> songList, OnSongClickListener listener) {
        this.context = context;
        this.songList = songList;
        this.listener = listener;
    }

    public void setOnSongLongClickListener(OnSongLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public OnlineSongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_online_song, parent, false);
        return new OnlineSongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OnlineSongViewHolder holder, int position) {
        OnlineSong song = songList.get(position);

        holder.songTitle.setText(song.getTitle());
        holder.songArtist.setText(song.getArtist());

        holder.songTitle.setSelected(true);
        holder.songArtist.setSelected(true);

        Glide.with(context)
                .load(song.getImageUrl())
                .placeholder(R.drawable.ic_album_placeholder)
                .into(holder.songImage);

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

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSongClick(song);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onSongLongClick(song);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    public void setHighlightedIndex(int index) {
        this.highlightedIndex = index;
        notifyDataSetChanged();
    }

    public void setHighlightedIndexDelayed(int index, long delayMillis) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            this.highlightedIndex = index;
            notifyDataSetChanged();
        }, delayMillis);
    }

    public static class OnlineSongViewHolder extends RecyclerView.ViewHolder {
        ImageView songImage;
        TextView songTitle, songArtist;

        public OnlineSongViewHolder(@NonNull View itemView) {
            super(itemView);
            songImage = itemView.findViewById(R.id.songImage);
            songTitle = itemView.findViewById(R.id.songTitle);
            songArtist = itemView.findViewById(R.id.songArtist);
        }
    }
}
