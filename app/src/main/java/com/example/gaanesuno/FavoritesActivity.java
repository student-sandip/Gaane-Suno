package com.example.gaanesuno;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.Serializable;
import java.util.ArrayList;

public class FavoritesActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayList<OnlineSong> originalFavoritesList;
    private ArrayList<OnlineSong> displayFavoritesList;
    private FavoritesAdapter adapter;
    private int currentPlayingIndex = -1;
    private SearchView searchView;
    private TextView noFavoritesText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        fixSearchHintColor();

        listView = findViewById(R.id.favoritesList);
        searchView = findViewById(R.id.searchView);
        noFavoritesText = findViewById(R.id.no_favorites_text);
        originalFavoritesList = FavoritesManager.getFavorites(this);
        displayFavoritesList = new ArrayList<>(originalFavoritesList);

        if (originalFavoritesList.isEmpty()) {
            listView.setVisibility(View.GONE);
            noFavoritesText.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            noFavoritesText.setVisibility(View.GONE);
        }

        adapter = new FavoritesAdapter(this, displayFavoritesList, new FavoritesAdapter.OnSongActionListener() {
            @Override
            public void onPlay(OnlineSong song) {
                int pos = displayFavoritesList.indexOf(song);
                if (pos == -1) pos = 0;

                currentPlayingIndex = pos;
                adapter.setHighlightedIndex(currentPlayingIndex);

                Intent intent = new Intent(FavoritesActivity.this, OnlinePlayerActivity.class);
                intent.putExtra("songsList", (Serializable) displayFavoritesList);
                intent.putExtra("position", pos);
                intent.putExtra("IS_FAVORITE_PLAY", true);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_up_fade, R.anim.slide_out_down_fade);
            }

            @Override
            public void onRemove(OnlineSong song) {
                FavoritesManager.removeFavorite(FavoritesActivity.this, song.getTrackId());
                originalFavoritesList.remove(song);
                displayFavoritesList.remove(song);
                adapter.notifyDataSetChanged();
                Toast.makeText(FavoritesActivity.this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                if (originalFavoritesList.isEmpty()) {
                    listView.setVisibility(View.GONE);
                    noFavoritesText.setVisibility(View.VISIBLE);
                }
            }
        });

        listView.setAdapter(adapter);

        setupSearch();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_favorite);

        bottomNavigationView.setOnItemSelectedListener(item -> {
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
                return true;
            }
            return false;
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterFavorites(newText);
                return true;
            }
        });
    }

    private void filterFavorites(String text) {
        displayFavoritesList.clear();
        if (text.isEmpty()) {
            displayFavoritesList.addAll(originalFavoritesList);
        } else {
            text = text.toLowerCase();
            for (OnlineSong song : originalFavoritesList) {
                if (song.getTitle().toLowerCase().contains(text)) {
                    displayFavoritesList.add(song);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();

        String currentUrl = getSharedPreferences("CURRENT_SONG", MODE_PRIVATE)
                .getString("url", null);

        currentPlayingIndex = -1;
        if (currentUrl != null) {
            for (int i = 0; i < displayFavoritesList.size(); i++) {
                if (displayFavoritesList.get(i).getPreviewUrl().equals(currentUrl)) {
                    currentPlayingIndex = i;
                    break;
                }
            }
        }
        adapter.setHighlightedIndex(currentPlayingIndex);
    }

    private void fixSearchHintColor() {
        SearchView searchView = findViewById(R.id.searchView);

        int nightModeFlags = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;

        if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO) {
            // Light mode only
            int hintColor = Color.GRAY;  // or Color.WHITE

            int id = searchView.getContext()
                    .getResources()
                    .getIdentifier("android:id/search_src_text", null, null);

            EditText searchEditText = searchView.findViewById(id);

            if (searchEditText != null) {
                searchEditText.setHintTextColor(hintColor);
            }
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
