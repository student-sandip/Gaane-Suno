package com.example.gaanesuno;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class OnlinePlayerActivity extends AppCompatActivity {

    // UI elements
    private ImageView albumArt;
    private ProgressBar bufferingProgress;
    private TextView songTitle, artistName, currentTime, totalTime, streamInfo;
    private SeekBar seekBar;
    private ImageButton btnPrev, btnPlayPause, btnNext, btnShuffle, btnRepeat, btnTimer, btnBack, btnMore;

    // Data & state
    private ArrayList<OnlineSong> songs = new ArrayList<>();
    private int position = 0;
    private OnlineSong currentSong;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private boolean isShuffle = false;
    private boolean isRepeatOne = false;
    private boolean userSeeking = false;

    // Timer & vibrator
    private CountDownTimer sleepTimer;
    private Vibrator vibrator;
    private SharedPreferences playerPrefs;

    // Receiver to get song info & progress from OnlineMusicService
    private final BroadcastReceiver playerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            if (intent.hasExtra("BUFFERING_STATUS")) {
                boolean isBuffering = intent.getBooleanExtra("BUFFERING_STATUS", false);
                bufferingProgress.setVisibility(isBuffering ? View.VISIBLE : View.GONE);
            }

            long trackId = intent.getLongExtra("SONG_TRACK_ID", -1);
            String title = intent.getStringExtra("SONG_TITLE");
            String artist = intent.getStringExtra("ARTIST_NAME");
            String image = intent.getStringExtra("SONG_IMAGE");
            String url = intent.getStringExtra("SONG_URL");
            long duration = intent.getLongExtra("SONG_DURATION", 0);

            if (trackId != -1) {
                boolean isDifferent = currentSong == null || trackId != currentSong.getTrackId();
                currentSong = new OnlineSong(trackId, title != null ? title : "Unknown",
                        artist != null ? artist : "",
                        image != null ? image : "",
                        url != null ? url : "", duration);

                if (title != null) {
                    songTitle.setText(title);
                    songTitle.setSelected(true);
                }
                if (artist != null) {
                    artistName.setText(artist);
                    artistName.setSelected(true);
                }
                streamInfo.setText("");
                if (image != null && !image.isEmpty()) {
                    Glide.with(OnlinePlayerActivity.this)
                            .load(image)
                            .placeholder(R.drawable.ic_album_placeholder)
                            .into(albumArt);
                } else {
                    albumArt.setImageResource(R.drawable.ic_album_placeholder);
                }

                if (isDifferent && songs != null && !songs.isEmpty()) {
                    int found = -1;
                    for (int i = 0; i < songs.size(); i++) {
                        if (songs.get(i) != null && songs.get(i).getTrackId() == trackId) {
                            found = i;
                            break;
                        }
                    }
                    if (found >= 0) {
                        position = found;
                    }
                }
            } else {
                if (title != null) {
                    songTitle.setText(title);
                    songTitle.setSelected(true);
                }
                if (artist != null) {
                    artistName.setText(artist);
                    artistName.setSelected(true);
                }
                if (image != null && !image.isEmpty()) {
                    Glide.with(OnlinePlayerActivity.this)
                            .load(image)
                            .placeholder(R.drawable.ic_album_placeholder)
                            .into(albumArt);
                }
                streamInfo.setText("");
            }

            if (intent.hasExtra("CURRENT_POSITION") || intent.hasExtra("DURATION")) {
                long pos = intent.getIntExtra("CURRENT_POSITION", 0);
                long dur = intent.getIntExtra("DURATION", 0);
                boolean isPlaying = intent.getBooleanExtra("IS_PLAYING", false);

                if (dur > 0) {
                    bufferingProgress.setVisibility(View.GONE);
                    seekBar.setMax((int) dur);
                    totalTime.setText(formatTime(dur));
                }
                if (!userSeeking) {
                    seekBar.setProgress((int) pos);
                }
                currentTime.setText(formatTime(pos));
                btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_player);

        initViews();
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        playerPrefs = getSharedPreferences("PLAYER_SETTINGS", MODE_PRIVATE);

        loadPlayerPreferences();

        Intent in = getIntent();
        if (in != null) {
            if (in.hasExtra("trackId")) {
                OnlineSong s = new OnlineSong(in.getLongExtra("trackId", -1), in.getStringExtra("title"), in.getStringExtra("artist"), in.getStringExtra("image"), in.getStringExtra("url"), in.getLongExtra("duration", 0));
                songs.clear();
                songs.add(s);
                position = 0;
                playSong(position);
            } else if (in.hasExtra("songsList")) {
                try {
                    songs = (ArrayList<OnlineSong>) in.getSerializableExtra("songsList");
                    position = in.getIntExtra("position", 0);

                    if (songs != null && !songs.isEmpty()) {
                        playSong(position);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

        } else if (in.hasExtra("query")) {
                fetchItunesAndPlay(in.getStringExtra("query"));
                return;
            }
        }

        if (songs == null || songs.isEmpty()) {
            loadLastPlayedSong();
        }

        setButtonListeners();
        IntentFilter filter = new IntentFilter("ONLINE_PLAYER_UPDATE");
        ContextCompat.registerReceiver(this, playerUpdateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private void initViews() {
        albumArt = findViewById(R.id.albumArt);
        bufferingProgress = findViewById(R.id.bufferingProgress);
        songTitle = findViewById(R.id.songTitle);
        artistName = findViewById(R.id.artistName);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);
        streamInfo = findViewById(R.id.qualityText);
        seekBar = findViewById(R.id.seekBar);
        btnPrev = findViewById(R.id.btnPrev);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnRepeat = findViewById(R.id.btnRepeat);
        btnTimer = findViewById(R.id.btnTimer);
        btnBack = findViewById(R.id.btnBack);
        btnMore = findViewById(R.id.btnMore);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                if (fromUser) currentTime.setText(formatTime(p));
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                userSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                userSeeking = false;
                Intent i = new Intent("ONLINE_PLAYER_CONTROL").setPackage(getPackageName());
                i.putExtra("ACTION", "SEEK_TO").putExtra("POSITION", (long) sb.getProgress());
                sendBroadcast(i);
            }
        });

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void setButtonListeners() {
        btnPlayPause.setOnClickListener(v -> {
            vibrateShort();
            Intent i = new Intent("ONLINE_PLAYER_CONTROL").setPackage(getPackageName());
            i.putExtra("ACTION", "TOGGLE_PLAY_PAUSE");
            sendBroadcast(i);
        });

        btnNext.setOnClickListener(v -> {
            vibrateShort();
            Intent i = new Intent("ONLINE_PLAYER_CONTROL").setPackage(getPackageName());
            i.putExtra("ACTION", "NEXT_SONG");
            sendBroadcast(i);
        });

        btnPrev.setOnClickListener(v -> {
            vibrateShort();
            Intent i = new Intent("ONLINE_PLAYER_CONTROL").setPackage(getPackageName());
            i.putExtra("ACTION", "PREVIOUS_SONG");
            sendBroadcast(i);
        });

        btnShuffle.setOnClickListener(v -> {
            vibrateShort();
            isShuffle = !isShuffle;
            playerPrefs.edit().putBoolean("shuffle", isShuffle).apply();
            btnShuffle.setImageResource(isShuffle ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off);
            Toast.makeText(this, isShuffle ? "Shuffle On" : "Shuffle Off", Toast.LENGTH_SHORT).show();
            Intent i = new Intent("ONLINE_PLAYER_CONTROL").setPackage(getPackageName());
            i.putExtra("ACTION", "SET_SHUFFLE").putExtra("SHUFFLE", isShuffle);
            sendBroadcast(i);
        });

        btnRepeat.setOnClickListener(v -> {
            vibrateShort();
            isRepeatOne = !isRepeatOne;
            playerPrefs.edit().putBoolean("repeat", isRepeatOne).apply();
            btnRepeat.setImageResource(isRepeatOne ? R.drawable.ic_repeat_one : R.drawable.ic_repeat_off);
            Toast.makeText(this, isRepeatOne ? "Repeat One" : "Repeat Off", Toast.LENGTH_SHORT).show();
            Intent i = new Intent("ONLINE_PLAYER_CONTROL").setPackage(getPackageName());
            i.putExtra("ACTION", "SET_REPEAT").putExtra("REPEAT", isRepeatOne);
            sendBroadcast(i);
        });

        btnTimer.setOnClickListener(v -> showTimerOptions());
        if (btnMore != null) btnMore.setOnClickListener(this::showPopupMenu);
    }

    private void playSong(int pos) {
        if (songs == null || pos < 0 || pos >= songs.size()) return;
        position = pos;
        currentSong = songs.get(position);

        songTitle.setText(currentSong.getTitle());
        songTitle.setSelected(true);
        artistName.setText(currentSong.getArtist());
        artistName.setSelected(true);
        streamInfo.setText("");
        currentTime.setText("00:00");
        totalTime.setText(currentSong.getDuration());
        seekBar.setProgress(0);
        bufferingProgress.setVisibility(View.VISIBLE);

        if (currentSong.getImageUrl() != null && !currentSong.getImageUrl().isEmpty()) {
            Glide.with(this).load(currentSong.getImageUrl()).placeholder(R.drawable.ic_album_placeholder).into(albumArt);
        } else {
            albumArt.setImageResource(R.drawable.ic_album_placeholder);
        }
        startOnlineServiceWithSong(currentSong);
    }

    private void startOnlineServiceWithSong(OnlineSong s) {
        if (s == null) return;
        Intent intent = new Intent(this, OnlineMusicService.class);
        intent.putExtra("SONG_TRACK_ID", s.getTrackId());
        intent.putExtra("SONG_URL", s.getPreviewUrl());
        intent.putExtra("SONG_TITLE", s.getTitle());
        intent.putExtra("ARTIST_NAME", s.getArtist());
        intent.putExtra("SONG_IMAGE", s.getImageUrl());
        intent.putExtra("SONG_DURATION", s.getDurationMillis());

        boolean isFavoritePlay = getIntent().getSerializableExtra("songsList") != null;
        intent.putExtra("IS_FAVORITE_PLAY", isFavoritePlay);

        ContextCompat.startForegroundService(this, intent);
    }


    private void showTimerOptions() {
        if (sleepTimer != null) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Sleep Timer Active")
                    .setMessage("A timer is currently running.")
                    .setPositiveButton("Cancel Timer", (d, w) -> cancelSleepTimer())
                    .setNegativeButton("Close", null)
                    .show();
        } else {
            final String[] options = {"5 min", "15 min", "30 min", "45 min", "60 min"};
            final int[] minutes = {5, 15, 30, 45, 60};

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Set Sleep Timer")
                    .setItems(options, (d, which) -> setSleepTimer(minutes[which]))
                    .show();
        }
    }

    private void setSleepTimer(int minutes) {
        if (sleepTimer != null) sleepTimer.cancel();
        long now = System.currentTimeMillis();
        long timerEndTime = now + (minutes * 60 * 1000L);
        playerPrefs.edit().putLong("sleepTimerEnd", timerEndTime).apply();
        startSleepTimer(timerEndTime - now);
        Toast.makeText(this, "Timer set: " + minutes + " minutes", Toast.LENGTH_SHORT).show();
    }

    private void startSleepTimer(long duration) {
        if (duration <= 0) {
            cancelSleepTimer();
            return;
        }
        btnTimer.setImageResource(R.drawable.ic_timer_on);
        sleepTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long untilFinished) {
            }

            @Override
            public void onFinish() {
                Intent i = new Intent("ONLINE_PLAYER_CONTROL").setPackage(getPackageName());
                i.putExtra("ACTION", "TOGGLE_PLAY_PAUSE");
                sendBroadcast(i);
                btnPlayPause.setImageResource(R.drawable.ic_play);
                cancelSleepTimer();
                Toast.makeText(OnlinePlayerActivity.this, "Timer finished — playback paused", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void cancelSleepTimer() {
        if (sleepTimer != null) sleepTimer.cancel();
        sleepTimer = null;
        playerPrefs.edit().remove("sleepTimerEnd").apply();
        btnTimer.setImageResource(R.drawable.ic_timer);
    }

    private void loadPlayerPreferences() {
        isShuffle = playerPrefs.getBoolean("shuffle", false);
        isRepeatOne = playerPrefs.getBoolean("repeat", false);
        btnShuffle.setImageResource(isShuffle ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off);
        btnRepeat.setImageResource(isRepeatOne ? R.drawable.ic_repeat_one : R.drawable.ic_repeat_off);

        long timerEndTime = playerPrefs.getLong("sleepTimerEnd", 0);
        if (timerEndTime > 0) {
            long remaining = timerEndTime - System.currentTimeMillis();
            if (remaining > 0) {
                startSleepTimer(remaining);
            }
        }
    }

    private void loadLastPlayedSong() {
        SharedPreferences prefs = getSharedPreferences("CURRENT_SONG", MODE_PRIVATE);
        long trackId = prefs.getLong("trackId", -1);
        String savedTitle = prefs.getString("title", null);
        String savedArtist = prefs.getString("artist", null);
        String savedImage = prefs.getString("image", null);
        String savedUrl = prefs.getString("url", null);
        long duration = prefs.getLong("duration", 0);

        if (trackId != -1) {
            currentSong = new OnlineSong(trackId, savedTitle, savedArtist, savedImage, savedUrl, duration);
            songTitle.setText(savedTitle != null ? savedTitle : "Unknown");
            songTitle.setSelected(true);
            artistName.setText(savedArtist != null ? savedArtist : "");
            artistName.setSelected(true);
            streamInfo.setText("");
            totalTime.setText(currentSong.getDuration());
            if (savedImage != null && !savedImage.isEmpty()) {
                Glide.with(this).load(savedImage).placeholder(R.drawable.ic_album_placeholder).into(albumArt);
            }
        } else {
            Toast.makeText(this, "No songs to play", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);

        boolean isFav = FavoritesManager.isFavorite(this, currentSong);
        popup.getMenu().add(0, 1, 0, isFav ? "Already in Favorites ❤️" : "Add to Favorites")
                .setEnabled(!isFav);
        popup.getMenu().add(0, 2, 1, "Share");
        popup.getMenu().add(0, 3, 2, "Info");

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == 1) {
                FavoritesManager.addFavorite(this, currentSong);
                Toast.makeText(this, "Added to Favorites ❤️", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == 2) {
                if (currentSong != null) {
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(
                            Intent.EXTRA_TEXT,
                            currentSong.getTitle() + " — " +
                                    currentSong.getArtist() + " " +
                                    currentSong.getPreviewUrl()
                    );
                    startActivity(Intent.createChooser(share, "Share via"));
                }
                return true;
            } else if (id == 3) {
                if (currentSong != null) {
                    String msg = "Title: " + currentSong.getTitle() + "\n\nArtist: " + currentSong.getArtist() + "\n\nDuration: " + currentSong.getDuration();

                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Song Info")
                            .setMessage(msg)
                            .setPositiveButton("OK", null)
                            .show();
                }
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void vibrateShort() {
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
            else vibrator.vibrate(40);
        }
    }

    private String formatTime(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sleepTimer != null) sleepTimer.cancel();
        try {
            unregisterReceiver(playerUpdateReceiver);
        } catch (Exception ignored) {
        }
    }

    private void fetchItunesAndPlay(String query) {
        bufferingProgress.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
                String apiUrl = "https://itunes.apple.com/search?term=" + encodedQuery + "&entity=song&limit=15";

                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();
                conn.disconnect();

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray results = jsonResponse.optJSONArray("results");

                if (results == null || results.length() == 0) {
                    runOnUiThread(() -> {
                        bufferingProgress.setVisibility(View.GONE);
                        Toast.makeText(OnlinePlayerActivity.this, "No results found!", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                ArrayList<OnlineSong> tempList = new ArrayList<>();
                for (int i = 0; i < results.length(); i++) {
                    JSONObject obj = results.getJSONObject(i);
                    long trackId = obj.optLong("trackId");
                    String title = obj.optString("trackName", "Unknown");
                    String artist = obj.optString("artistName", "Unknown");
                    String imageUrl = obj.optString("artworkUrl100", "");
                    String previewUrl = obj.optString("previewUrl", "");
                    long duration = obj.optLong("trackTimeMillis", 0);

                    if (previewUrl != null && !previewUrl.isEmpty()) {
                        tempList.add(new OnlineSong(trackId, title, artist, imageUrl, previewUrl, duration));
                    }
                }

                if (!tempList.isEmpty()) {
                    songs = tempList;
                    runOnUiThread(() -> playSong(0));
                } else {
                    runOnUiThread(() -> {
                        bufferingProgress.setVisibility(View.GONE);
                        Toast.makeText(OnlinePlayerActivity.this, "No playable results found!", Toast.LENGTH_SHORT).show();
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    bufferingProgress.setVisibility(View.GONE);
                    Toast.makeText(OnlinePlayerActivity.this, "Failed to fetch songs", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
