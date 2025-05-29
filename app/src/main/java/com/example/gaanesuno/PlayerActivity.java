package com.example.gaanesuno;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {

    private GestureDetector gestureDetector;

    private TextView tvTitle, tvArtist, currentTime, totalTime;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnNext, btnPrev, btnShuffle, btnRepeat, btnTimer;
    private ImageView albumArt;

    private ArrayList<Song> songs;
    private int position = 0;

    private boolean isShuffle = false;
    private boolean isRepeat = false;

    private MusicService musicService;
    private boolean isBound = false;
    private final Handler handler = new Handler();
    private CountDownTimer sleepTimer;
    private AudioManager audioManager;
    private SeekBar volumeSeekBar;

    private Song currentSong; // Declare currentSong globally

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            MusicService.MusicBinder musicBinder = (MusicService.MusicBinder) binder;
            musicService = musicBinder.getService();
            isBound = true;
            playSong();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initViews();

        songs = (ArrayList<Song>) getIntent().getSerializableExtra("songsList");
        position = getIntent().getIntExtra("position", 0);

        if (songs == null || songs.isEmpty()) {
            Toast.makeText(this, "No songs to play", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        ImageButton btnVolumeUp = findViewById(R.id.btnVolumeUp);
        ImageButton btnVolumeDown = findViewById(R.id.btnVolumeDown);

        ImageView playerAlbumArt = findViewById(R.id.playerAlbumArt);
        TextView lyricsView = findViewById(R.id.lyricsView);

        playerAlbumArt.setOnClickListener(new View.OnClickListener() {
            boolean showingLyrics = false;

            @Override
            public void onClick(View v) {
                if (showingLyrics) {
                    lyricsView.setVisibility(View.GONE);
                } else {
                    lyricsView.setVisibility(View.VISIBLE);
                    lyricsView.setText("Sample lyrics...\nYou can load actual lyrics here.");
                }
                showingLyrics = !showingLyrics;
            }
        });

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        volumeSeekBar.setMax(maxVolume);
        volumeSeekBar.setProgress(currentVolume);

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnVolumeUp.setOnClickListener(v -> {
            int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (current < maxVolume) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current + 1, AudioManager.FLAG_PLAY_SOUND);
                volumeSeekBar.setProgress(current + 1);
            }
        });

        btnVolumeDown.setOnClickListener(v -> {
            int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (current > 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current - 1, AudioManager.FLAG_PLAY_SOUND);
                volumeSeekBar.setProgress(current - 1);
            }
        });

        Intent serviceIntent = new Intent(this, MusicService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> playNextSong());
        btnPrev.setOnClickListener(v -> playPreviousSong());

        btnShuffle.setOnClickListener(v -> {
            isShuffle = !isShuffle;
            btnShuffle.setImageResource(isShuffle ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off);
        });

        final int[] repeatMode = {0};

        btnRepeat.setOnClickListener(v -> {
            repeatMode[0] = (repeatMode[0] + 1) % 3;

            switch (repeatMode[0]) {
                case 0:
                    isRepeat = false;
                    btnRepeat.setImageResource(R.drawable.ic_repeat_off);
                    Toast.makeText(this, "Repeat Off", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    isRepeat = true;
                    btnRepeat.setImageResource(R.drawable.ic_repeat_on);
                    Toast.makeText(this, "Repeat All", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    isRepeat = true;
                    btnRepeat.setImageResource(R.drawable.ic_repeat_one);
                    Toast.makeText(this, "Repeat One", Toast.LENGTH_SHORT).show();
                    break;
            }
        });


        btnTimer.setOnClickListener(v -> showTimerDialog());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && musicService != null) {
                    musicService.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        updateSeekBar();

        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        }


        ImageButton btnMore = findViewById(R.id.btnMore);
        btnMore.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(PlayerActivity.this, view);
            popupMenu.getMenuInflater().inflate(R.menu.more_menu, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.menu_delete) {
                    deleteCurrentSong();
                    return true;
                } else if (itemId == R.id.menu_settings) {
                    openSettings();
                    return true;
                }
                else {
                    return false;
                }

            });

            popupMenu.show();
        });
    }
    private void deleteCurrentSong() {
        if (currentSong == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Song")
                .setMessage("Are you sure you want to delete this song?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (musicService != null) musicService.stop();  // Stop playback first

                    File file = new File(currentSong.getPath());
                    Uri contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currentSong.getId());

                    Log.d("DeletePath", "Trying to delete: " + file.getAbsolutePath());

                    if (file.exists()) {
                        boolean deleted = file.delete();

                        if (deleted) {
                            // Remove from MediaStore
                            getContentResolver().delete(contentUri, null, null);
                            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            scanIntent.setData(Uri.fromFile(file));
                            sendBroadcast(scanIntent);
                            Toast.makeText(this, "Song deleted", Toast.LENGTH_SHORT).show();

                            // Pass result back to MainActivity
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("songDeleted", true);
                            resultIntent.putExtra("deletedSongPath", currentSong.getPath());
                            setResult(RESULT_OK, resultIntent);

                            finish(); // Exit the player
                        } else {
                            Toast.makeText(this, "Failed to delete. Try enabling All Files Access.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "File does not exist", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }


    private void initViews() {
        tvTitle = findViewById(R.id.playerSongTitle);
        tvArtist = findViewById(R.id.playerArtist);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);
        seekBar = findViewById(R.id.playerSeekBar);
        albumArt = findViewById(R.id.playerAlbumArt);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnRepeat = findViewById(R.id.btnRepeat);
        btnTimer = findViewById(R.id.btnTimer);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void playSong() {
        if (musicService == null || songs == null || songs.isEmpty()) return;

        currentSong = songs.get(position); // assign global
        MusicState.currentlyPlayingPosition = position;

        tvTitle.setSelected(true);
        tvTitle.setText(currentSong.getTitle());
        tvArtist.setText(currentSong.getArtist());

        String albumArtUri = currentSong.getAlbumArtUri();
        if (albumArtUri != null && !albumArtUri.isEmpty()) {
            albumArt.setImageURI(Uri.parse(albumArtUri));
        } else {
            albumArt.setImageResource(R.drawable.ic_album_placeholder);
        }

        if (!musicService.isPlaying() || !musicService.isSameSong(currentSong.getPath())) {
            musicService.playMedia(currentSong.getPath());
        }
        btnPlayPause.setImageResource(R.drawable.ic_pause);

        musicService.setOnCompletionListener(() -> {
            if (isRepeat) {
                playSong();
            } else {
                playNextSong();
            }
        });
    }

    private void playNextSong() {
        if (songs == null || songs.isEmpty()) return;
        position = isShuffle ? new Random().nextInt(songs.size()) : (position + 1) % songs.size();
        playSong();
    }

    private void playPreviousSong() {
        if (songs == null || songs.isEmpty()) return;
        position = isShuffle ? new Random().nextInt(songs.size()) : (position - 1 + songs.size()) % songs.size();
        playSong();
    }

    private void togglePlayPause() {
        if (musicService != null) {
            if (musicService.isPlaying()) {
                musicService.pause();
                btnPlayPause.setImageResource(R.drawable.ic_play);
            } else {
                musicService.resume();
                btnPlayPause.setImageResource(R.drawable.ic_pause);
            }
        }
    }

    private void updateSeekBar() {
        handler.postDelayed(() -> {
            if (musicService != null && musicService.isPlaying()) {
                int current = musicService.getCurrentPosition();
                int duration = musicService.getDuration();

                seekBar.setMax(duration);
                seekBar.setProgress(current);
                currentTime.setText(formatTime(current));
                totalTime.setText(formatTime(duration));
            }
            updateSeekBar();
        }, 500);
    }

    private String formatTime(int milliseconds) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milliseconds),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60);
    }

    private void showTimerDialog() {
        final String[] options = {"5 minutes", "15 minutes", "30 minutes", "45 minutes", "60 minutes", "120 minutes", "Cancel Timer"};
        final int[] times = {5, 15, 30, 45, 60, 120, 0};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Sleep Timer")
                .setItems(options, (dialog, which) -> {
                    if (times[which] == 0) {
                        if (sleepTimer != null) {
                            sleepTimer.cancel();
                            sleepTimer = null;
                            Toast.makeText(this, "Sleep timer canceled", Toast.LENGTH_SHORT).show();
                            btnTimer.setImageResource(R.drawable.ic_timer);
                        }
                    } else {
                        int minutes = times[which];
                        if (sleepTimer != null) sleepTimer.cancel();
                        sleepTimer = new CountDownTimer(minutes * 60 * 1000L, 1000) {
                            public void onTick(long millisUntilFinished) {}
                            public void onFinish() {
                                if (musicService != null && musicService.isPlaying()) {
                                    musicService.pause();
                                    btnPlayPause.setImageResource(R.drawable.ic_play);
                                    Toast.makeText(PlayerActivity.this, "Playback stopped by sleep timer", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }.start();

                        Toast.makeText(this, "Timer set for " + minutes + " minutes", Toast.LENGTH_SHORT).show();
                        btnTimer.setImageResource(R.drawable.ic_timer_on);
                    }
                });
        builder.show();
    }

    @Override
    protected void onDestroy() {
        if (sleepTimer != null) sleepTimer.cancel();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        if (sleepTimer != null) sleepTimer.cancel();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
