package com.example.gaanesuno;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private ImageButton btnBack, btnClear;
    private RecyclerView resultsRv, historyRv;
    private OnlineSongAdapter resultsAdapter;
    private HistoryAdapter historyAdapter;

    private ArrayList<OnlineSong> searchResults = new ArrayList<>();
    private ArrayList<String> searchHistory = new ArrayList<>();

    private final Handler debounceHandler = new Handler();
    private Runnable debounceRunnable;

    private SharedPreferences prefs;
    private static final String PREF_NAME = "search_prefs_v2";
    private static final String KEY_HISTORY_MAP = "search_history_map";
    private static final int DEBOUNCE_MS = 400;
    private static final long EXPIRY_MS = 2 * 24 * 60 * 60 * 1000L; // 2 days

    private static final String APP_NAME = "GaaneSuno"; // sent to Audius as app_name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        etSearch = findViewById(R.id.etSearch);
        btnBack = findViewById(R.id.btnBack);
        btnClear = findViewById(R.id.btnClear);
        resultsRv = findViewById(R.id.searchRecyclerView);
        historyRv = findViewById(R.id.historyRecyclerView);

        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Recycler setup
        resultsRv.setLayoutManager(new LinearLayoutManager(this));
        historyRv.setLayoutManager(new LinearLayoutManager(this));

        resultsAdapter = new OnlineSongAdapter(this, searchResults, song -> {
            saveQueryToHistory(etSearch.getText().toString().trim());
            Intent intent = new Intent(SearchActivity.this, OnlinePlayerActivity.class);
            intent.putExtra("songsList", searchResults);
            intent.putExtra("position", searchResults.indexOf(song));
            startActivity(intent);
        });
        resultsRv.setAdapter(resultsAdapter);

        historyAdapter = new HistoryAdapter(this, searchHistory, new HistoryAdapter.OnHistoryInteraction() {
            @Override
            public void onHistoryClick(String item) {
                etSearch.setText(item);
                etSearch.setSelection(item.length());
                performSearch(item);
            }

            @Override
            public void onArrowClick(String item) {
                etSearch.setText(item);
                etSearch.setSelection(item.length());
            }
        });
        historyRv.setAdapter(historyAdapter);

        btnBack.setOnClickListener(v -> finish());
        btnClear.setOnClickListener(v -> {
            clearHistory();
            Toast.makeText(this, "Search history cleared", Toast.LENGTH_SHORT).show();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String q = s.toString().trim();
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> {
                    if (q.isEmpty()) showHistoryView();
                    else performSearch(q);
                };
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_MS);
            }
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String q = etSearch.getText().toString().trim();
                if (!q.isEmpty()) {
                    performSearch(q);
                    saveQueryToHistory(q);
                }
                return true;
            }
            return false;
        });

        // load & clean expired history
        loadHistory();
        cleanExpiredHistory();
    }

    private void showHistoryView() {
        historyRv.setVisibility(View.VISIBLE);
        resultsRv.setVisibility(View.GONE);
    }

    private void showResultsView() {
        historyRv.setVisibility(View.GONE);
        resultsRv.setVisibility(View.VISIBLE);
    }

    // ---------- iTunes Search ----------
    private void performSearch(String query) {
        showResultsView();
        new Thread(() -> {
            try {
                String q = query.trim().toLowerCase(Locale.ENGLISH);

                String urlStr = String.format(Locale.ENGLISH,
                        "https://itunes.apple.com/search?term=%s&entity=song&limit=30",
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

                ArrayList<OnlineSong> temp = new ArrayList<>();
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.getJSONObject(i);
                        long trackId = obj.optLong("trackId");
                        String title = obj.optString("trackName", "Unknown");
                        String artist = obj.optString("artistName", "Unknown");
                        String imageUrl = obj.optString("artworkUrl100", "");
                        String audioUrl = obj.optString("previewUrl", ""); // 30–90s preview

                        if (!audioUrl.isEmpty()) {
                            temp.add(new OnlineSong(trackId, title, artist, imageUrl, audioUrl));
                        }
                    }
                }

                runOnUiThread(() -> {
                    searchResults.clear();
                    searchResults.addAll(temp);
                    resultsAdapter.notifyDataSetChanged();
                    if (searchResults.isEmpty())
                        Toast.makeText(SearchActivity.this, "No results found", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(SearchActivity.this, "Search failed.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    // ---------- History management ----------
    private void loadHistory() {
        searchHistory.clear();
        String data = prefs.getString(KEY_HISTORY_MAP, "{}");
        try {
            JSONObject obj = new JSONObject(data);
            Iterator<String> keys = obj.keys();
            long now = System.currentTimeMillis();
            while (keys.hasNext()) {
                String query = keys.next();
                long time = obj.getLong(query);
                if (now - time < EXPIRY_MS) {
                    searchHistory.add(0, query); // newest first
                }
            }
        } catch (Exception ignored) {}
        historyAdapter.notifyDataSetChanged();
    }

    private void saveQueryToHistory(@NonNull String query) {
        if (query.trim().isEmpty()) return;

        try {
            String data = prefs.getString(KEY_HISTORY_MAP, "{}");
            JSONObject obj = new JSONObject(data);
            obj.put(query, System.currentTimeMillis());
            prefs.edit().putString(KEY_HISTORY_MAP, obj.toString()).apply();
        } catch (Exception ignored) {}

        loadHistory();
    }

    private void clearHistory() {
        prefs.edit().remove(KEY_HISTORY_MAP).apply();
        searchHistory.clear();
        historyAdapter.notifyDataSetChanged();
    }

    private void cleanExpiredHistory() {
        try {
            String data = prefs.getString(KEY_HISTORY_MAP, "{}");
            JSONObject obj = new JSONObject(data);
            JSONObject updated = new JSONObject();
            long now = System.currentTimeMillis();
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String query = keys.next();
                long time = obj.getLong(query);
                if (now - time < EXPIRY_MS) {
                    updated.put(query, time);
                }
            }
            prefs.edit().putString(KEY_HISTORY_MAP, updated.toString()).apply();
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
    }
}
