package com.example.gaanesuno;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {

    public interface OnHistoryClick { void onClick(String item); }

    private final Context ctx;
    private final List<String> items;
    private final OnHistoryClick listener;

    public HistoryAdapter(Context ctx, List<String> items, OnHistoryClick listener) {
        this.ctx = ctx;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_history, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        String q = items.get(position);
        holder.tv.setText(q);
        holder.itemView.setOnClickListener(v -> listener.onClick(q));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tv;
        Holder(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.historyText);
        }
    }
}
