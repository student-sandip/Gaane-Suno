package com.example.gaanesuno;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.Serializable;
import java.util.ArrayList;

public class FavoritesActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayList<OnlineSong> favoritesList;
    private FavoritesAdapter adapter;
    private int currentPlayingIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        listView = findViewById(R.id.favoritesList);
        favoritesList = FavoritesManager.getFavorites(this);

        if (favoritesList.isEmpty()) {
            Toast.makeText(this, "No favorites yet!", Toast.LENGTH_SHORT).show();
        }

        // ✅ Setup adapter
        adapter = new FavoritesAdapter(this, favoritesList, new FavoritesAdapter.OnSongActionListener() {
            @Override
            public void onPlay(OnlineSong song) {
                int pos = favoritesList.indexOf(song);
                if (pos == -1) pos = 0;

                currentPlayingIndex = pos;
                adapter.setHighlightedIndex(currentPlayingIndex);

                // ✅ Start OnlinePlayerActivity with favorites list
                Intent intent = new Intent(FavoritesActivity.this, OnlinePlayerActivity.class);
                intent.putExtra("songsList", (Serializable) favoritesList);
                intent.putExtra("position", pos);
                intent.putExtra("IS_FAVORITE_PLAY", true); // 👈 Important flag
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_up_fade, R.anim.slide_out_down_fade);
            }

            @Override
            public void onRemove(OnlineSong song) {
                favoritesList.remove(song);
                adapter.notifyDataSetChanged();
                Toast.makeText(FavoritesActivity.this, "Removed from favorites", Toast.LENGTH_SHORT).show();
            }
        });

        listView.setAdapter(adapter);

        // ✅ Bottom Navigation setup with nice animation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_favorite);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_offline) {
                startActivity(new Intent(FavoritesActivity.this, MainActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_down_fade);
                finish();
                return true;
            } else if (id == R.id.nav_online) {
                startActivity(new Intent(FavoritesActivity.this, OnlineActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_down_fade);
                finish();
                return true;
            } else if (id == R.id.nav_favorite) {
                // Already here
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ✅ Highlight the currently playing song
        String currentUrl = getSharedPreferences("CURRENT_SONG", MODE_PRIVATE)
                .getString("url", null);

        if (currentUrl != null) {
            for (int i = 0; i < favoritesList.size(); i++) {
                if (favoritesList.get(i).getPreviewUrl().equals(currentUrl)) {
                    currentPlayingIndex = i;
                    adapter.setHighlightedIndex(i);
                    break;
                }
            }
        } else {
            adapter.setHighlightedIndex(-1);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        SharedPreferences prefs = getSharedPreferences("CURRENT_SONG", MODE_PRIVATE);
        String url = prefs.getString("url", null);

        if (url != null && !url.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finishAffinity();
        }
    }



}
