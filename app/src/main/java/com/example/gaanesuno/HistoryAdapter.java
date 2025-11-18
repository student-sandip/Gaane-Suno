package com.example.gaanesuno;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {

    public interface OnHistoryInteraction {
        void onHistoryClick(String item);
        void onArrowClick(String item);
    }

    private final Context ctx;
    private final List<String> items;
    private final OnHistoryInteraction listener;

    public HistoryAdapter(Context ctx, List<String> items, OnHistoryInteraction listener) {
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
        holder.itemView.setOnClickListener(v -> listener.onHistoryClick(q));
        holder.arrowBtn.setOnClickListener(v -> listener.onArrowClick(q));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tv;
        ImageButton arrowBtn;
        Holder(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.historyText);
            arrowBtn = itemView.findViewById(R.id.arrow_up);
        }
    }
}
