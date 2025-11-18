package com.example.gaanesuno;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
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
    private SwipeRefreshLayout swipeRefreshLayout;

    private int currentlyPlayingIndex = -1;
    private BroadcastReceiver networkChangeReceiver;
    private AlertDialog networkDialog;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_music);

        onlineRecyclerView = findViewById(R.id.onlineRecyclerView);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        onlineRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new OnlineSongAdapter(this, onlineSongsList, song -> {
            if (song.getPreviewUrl() == null || song.getPreviewUrl().isEmpty()) {
                Toast.makeText(this, "No audio available", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("OnlineActivity", "Playing song: " + song.getTitle());

            currentlyPlayingIndex = onlineSongsList.indexOf(song);

            SharedPreferences prefs = getSharedPreferences("CURRENT_SONG", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("trackId", song.getTrackId());
            editor.putString("title", song.getTitle());
            editor.putString("artist", song.getArtist());
            editor.putString("url", song.getPreviewUrl());
            editor.putString("image", song.getImageUrl());
            editor.putLong("duration", song.getDurationMillis());
            editor.apply();

            if (adapter != null) {
                adapter.setHighlightedIndex(currentlyPlayingIndex);
            }

            Intent intent = new Intent(OnlineActivity.this, OnlinePlayerActivity.class);
            intent.putExtra("songsList", (Serializable) onlineSongsList);
            intent.putExtra("position", currentlyPlayingIndex);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_up_fade, R.anim.slide_out_down_fade);
        });

        adapter.setOnSongLongClickListener(this::showMoreOptions);

        onlineRecyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(() -> fetchOnlineSongs(true));

        bottomNav = findViewById(R.id.bottom_navigation);
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
                finish();
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
            long trackId = prefs.getLong("trackId", -1);

            if (trackId != -1) {
                int pos = -1;
                for (int i = 0; i < onlineSongsList.size(); i++) {
                    if (onlineSongsList.get(i).getTrackId() == trackId) {
                        pos = i;
                        break;
                    }
                }
                if (pos == -1) pos = 0;

                Intent intent = new Intent(this, OnlinePlayerActivity.class);
                intent.putExtra("songsList", (Serializable) onlineSongsList);
                intent.putExtra("position", pos);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_up_fade, R.anim.slide_out_down_fade);
            } else {
                Toast.makeText(this, "No song is currently playing!", Toast.LENGTH_SHORT).show();
            }
        });

        networkChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isNetworkAvailable()) {
                    if (networkDialog != null && networkDialog.isShowing()) {
                        networkDialog.dismiss();
                    }
                    if (onlineSongsList.isEmpty() && !isLoading) {
                        fetchOnlineSongs(false);
                    }
                } else {
                    showNetworkDialog();
                }
            }
        };
        registerReceiver(networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private void showMoreOptions(OnlineSong song) {
        boolean isFavorite = FavoritesManager.isFavorite(this, song);
        String[] items = {isFavorite ? "Remove from Favorites" : "Add to Favorites", "Info"};

        new MaterialAlertDialogBuilder(this)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        if (isFavorite) {
                            FavoritesManager.removeFavorite(this, song.getTrackId());
                            Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
                        } else {
                            FavoritesManager.addFavorite(this, song);
                            Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show();
                        }
                    } else if (which == 1) {
                        showSongInfoDialog(song);
                    }
                })
                .show();
    }

    private void showSongInfoDialog(OnlineSong song) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Song Details")
                .setMessage(
                        "Title: " + song.getTitle() + "\n\n" +
                                "Artist: " + song.getArtist() + "\n\n"+
                                "Duration: " + song.getDuration()
                )
                .setPositiveButton("OK", null)
                .show();
    }

    private void showNetworkDialog() {
        if (networkDialog != null && networkDialog.isShowing()) {
            return;
        }
        networkDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("No Internet Connection")
                .setMessage("You need to be connected to the internet to listen to online songs.")
                .setPositiveButton("Retry", (dialog, which) -> {
                    dialog.dismiss();
                    fetchOnlineSongs(false);
                })
                .setNegativeButton("Exit", (dialog, which) -> {
                    startActivity(new Intent(OnlineActivity.this, MainActivity.class));
                    finish();
                })
                .setNeutralButton("Settings", (dialog, which) -> startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)))
                .setCancelable(false)
                .show();
    }

    private void fetchOnlineSongs(boolean isRefresh) {
        if (isLoading) return;

        if (!isNetworkAvailable()) {
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            showNetworkDialog();
            return;
        }

        isLoading = true;
        if (!isRefresh) {
            loadingIndicator.setVisibility(View.VISIBLE);
        }

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
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

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
                            long trackId = obj.optLong("trackId");
                            String title = obj.optString("trackName", "Unknown");
                            String artist = obj.optString("artistName", "Unknown");
                            String imageUrl = obj.optString("artworkUrl100", "");
                            String audioUrl = obj.optString("previewUrl", "");
                            long duration = obj.optLong("trackTimeMillis", 0);

                            if (!audioUrl.isEmpty()) {
                                tempList.add(new OnlineSong(trackId, title, artist, imageUrl, audioUrl, duration));
                            }
                        }
                    }
                }

                Collections.shuffle(tempList);

                runOnUiThread(() -> {
                    isLoading = false;
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    loadingIndicator.setVisibility(View.GONE);

                    SharedPreferences prefs = getSharedPreferences("CURRENT_SONG", MODE_PRIVATE);
                    long currentTrackId = prefs.getLong("trackId", -1);
                    OnlineSong playingSong = null;

                    if (currentTrackId != -1) {
                        for (OnlineSong song : onlineSongsList) {
                            if (song.getTrackId() == currentTrackId) {
                                playingSong = song;
                                break;
                            }
                        }
                        if (playingSong == null) {
                            String title = prefs.getString("title", "Unknown");
                            String artist = prefs.getString("artist", "Unknown");
                            String imageUrl = prefs.getString("image", "");
                            String url = prefs.getString("url", "");
                            long duration = prefs.getLong("duration", 0);
                            if (!url.isEmpty()) {
                                playingSong = new OnlineSong(currentTrackId, title, artist, imageUrl, url, duration);
                            }
                        }
                    }

                    onlineSongsList.clear();
                    if (playingSong != null) {
                        OnlineSong finalPlayingSong = playingSong;
                        tempList.removeIf(s -> s.getTrackId() == finalPlayingSong.getTrackId());
                        onlineSongsList.add(playingSong);
                    }
                    onlineSongsList.addAll(tempList);

                    adapter.notifyDataSetChanged();
                    if (playingSong != null) {
                        adapter.setHighlightedIndex(0);
                        onlineRecyclerView.smoothScrollToPosition(0);
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    isLoading = false;
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    loadingIndicator.setVisibility(View.GONE);
                    if (isNetworkAvailable()) {
                        Toast.makeText(OnlineActivity.this, "Failed to fetch songs. Please try again later.", Toast.LENGTH_SHORT).show();
                    } else {
                        showNetworkDialog();
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    isLoading = false;
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    loadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(OnlineActivity.this, "Failed to parse song data.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (onlineSongsList.isEmpty()) {
            fetchOnlineSongs(false);
        } else {
            updatePlayingSong();
        }
    }

    private void updatePlayingSong() {
        SharedPreferences prefs = getSharedPreferences("CURRENT_SONG", MODE_PRIVATE);
        long currentTrackId = prefs.getLong("trackId", -1);
        if (currentTrackId != -1) {
            int currentIndex = -1;
            for (int i = 0; i < onlineSongsList.size(); i++) {
                if (onlineSongsList.get(i).getTrackId() == currentTrackId) {
                    currentIndex = i;
                    break;
                }
            }
            if (currentIndex != -1) {
                OnlineSong playingSong = onlineSongsList.get(currentIndex);
                if (currentIndex != 0) {
                    onlineSongsList.remove(currentIndex);
                    onlineSongsList.add(0, playingSong);
                    adapter.notifyDataSetChanged();
                }
                adapter.setHighlightedIndex(0);
                onlineRecyclerView.smoothScrollToPosition(0);
            } else {
                String title = prefs.getString("title", "Unknown");
                String artist = prefs.getString("artist", "Unknown");
                String imageUrl = prefs.getString("image", "");
                String url = prefs.getString("url", "");
                long duration = prefs.getLong("duration", 0);
                if (!url.isEmpty()) {
                    OnlineSong playingSong = new OnlineSong(currentTrackId, title, artist, imageUrl, url, duration);
                    onlineSongsList.add(0, playingSong);
                    adapter.setHighlightedIndex(0);
                    adapter.notifyDataSetChanged();
                    onlineRecyclerView.smoothScrollToPosition(0);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);
        }
        if (networkDialog != null && networkDialog.isShowing()) {
            networkDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
}
