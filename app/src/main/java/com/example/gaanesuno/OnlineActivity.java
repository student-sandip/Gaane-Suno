package com.example.gaanesuno;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class OnlineActivity extends AppCompatActivity {

    RecyclerView onlineRecyclerView;
    OnlineSongAdapter adapter;
    ArrayList<OnlineSong> onlineSongsList = new ArrayList<>();

    BottomNavigationView bottomNav;
    ImageButton btnSettings, btnSearch;
    FloatingActionButton fabNowPlayingOnline;
    private ProgressBar loadingIndicator;

    // ✅ Track current playing position
    private int currentlyPlayingIndex = -1;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_music);

        onlineRecyclerView = findViewById(R.id.onlineRecyclerView);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        onlineRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new OnlineSongAdapter(this, onlineSongsList, song -> {
            if (song.getPreviewUrl() == null || song.getPreviewUrl().isEmpty()) {
                Toast.makeText(this, "No audio available", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("OnlineActivity", "Playing song: " + song.getTitle());

            currentlyPlayingIndex = onlineSongsList.indexOf(song); // ✅ Track current song

            SharedPreferences prefs = getSharedPreferences("CURRENT_SONG", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("title", song.getTitle());
            editor.putString("artist", song.getArtist());
            editor.putString("url", song.getPreviewUrl());
            editor.putString("image", song.getImageUrl());
            editor.putInt("bitrate", song.getBitrate());
            editor.apply();

            // ✅ Highlight immediately
            if (adapter != null) {
                adapter.setHighlightedIndex(currentlyPlayingIndex);
            }

            Intent intent = new Intent(OnlineActivity.this, OnlinePlayerActivity.class);
            intent.putExtra("songsList", (Serializable) onlineSongsList);
            intent.putExtra("position", currentlyPlayingIndex);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_up_fade, R.anim.slide_out_down_fade);
        });

        onlineRecyclerView.setAdapter(adapter);

        // ✅ Load songs initially
        fetchOnlineSongs();

        // ✅ Bottom Navigation setup
        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_online);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_offline) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(R.anim.slide_in_up_fade, R.anim.slide_out_down_fade);
                finish();
                return true;
            } else if (id == R.id.nav_online) {
                return true;
            } else if (id == R.id.nav_favorite) {
                Intent favIntent = new Intent(this, FavoritesActivity.class);
                startActivity(favIntent);
                overridePendingTransition(R.anim.slide_in_up_fade, R.anim.slide_out_down_fade);
                return true;
            }
            return false;
        });

        btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v -> {
            startActivity(new Intent(this, SearchActivity.class));
            overridePendingTransition(R.anim.slide_in_up_fade, R.anim.slide_out_down_fade);
        });

        fabNowPlayingOnline = findViewById(R.id.fabNowPlayingOnline);
        fabNowPlayingOnline.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("CURRENT_SONG", MODE_PRIVATE);
            String title = prefs.getString("title", null);
            String artist = prefs.getString("artist", null);
            String url = prefs.getString("url", null);
            String image = prefs.getString("image", null);
            int bitrate = prefs.getInt("bitrate", 128);

            if (url != null) {
                int pos = -1;
                for (int i = 0; i < onlineSongsList.size(); i++) {
                    if (onlineSongsList.get(i).getPreviewUrl().equals(url)) {
                        pos = i;
                        break;
                    }
                }
                if (pos == -1) pos = 0;

                Intent intent = new Intent(this, OnlinePlayerActivity.class);
                intent.putExtra("songsList", (Serializable) onlineSongsList);
                intent.putExtra("position", pos);
                intent.putExtra("title", title);
                intent.putExtra("artist", artist);
                intent.putExtra("url", url);
                intent.putExtra("image", image);
                intent.putExtra("bitrate", bitrate);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_up_fade, R.anim.slide_out_down_fade);
            } else {
                Toast.makeText(this, "No song is currently playing!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ Fetch songs from iTunes API
    private void fetchOnlineSongs() {
        loadingIndicator.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                String[] keywords = {"bollywood", "bengali", "hindi", "english", "romantic", "pop"};
                ArrayList<OnlineSong> tempList = new ArrayList<>();

                for (String q : keywords) {
                    String urlStr = String.format(Locale.ENGLISH,
                            "https://itunes.apple.com/search?term=%s&entity=song&limit=25",
                            q.replace(" ", "+"));
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(9000);
                    conn.setReadTimeout(9000);

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    conn.disconnect();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray data = json.optJSONArray("results");

                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            String title = obj.optString("trackName", "Unknown");
                            String artist = obj.optString("artistName", "Unknown");
                            String imageUrl = obj.optString("artworkUrl100", "");
                            String audioUrl = obj.optString("previewUrl", "");
                            int bitrate = 128;

                            if (!audioUrl.isEmpty()) {
                                tempList.add(new OnlineSong(title, artist, imageUrl, audioUrl));
                            }
                        }
                    }
                }

                Collections.shuffle(tempList);

                onlineSongsList.clear();
                onlineSongsList.addAll(tempList);

                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    loadingIndicator.setVisibility(View.GONE);

                    // ✅ Keep highlight even after refresh (match by title)
                    SharedPreferences prefs = getSharedPreferences("CURRENT_SONG", MODE_PRIVATE);
                    String currentTitle = prefs.getString("title", null);

                    if (currentTitle != null) {
                        int highlightPos = -1;
                        for (int i = 0; i < onlineSongsList.size(); i++) {
                            if (onlineSongsList.get(i).getTitle().equalsIgnoreCase(currentTitle)) {
                                highlightPos = i;
                                break;
                            }
                        }
                        if (highlightPos != -1) {
                            currentlyPlayingIndex = highlightPos;
                            adapter.setHighlightedIndex(highlightPos);
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to fetch songs", Toast.LENGTH_SHORT).show();
                    loadingIndicator.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Delay refresh by 5 seconds when coming back to Online tab
        handler.postDelayed(this::fetchOnlineSongs, 5000);
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
