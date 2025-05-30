package com.example.gaanesuno;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {

    private TextView tvTitle, tvArtist, currentTime, totalTime, lyricsView;
    private SeekBar seekBar, volumeSeekBar;
    private ImageButton btnPlayPause, btnNext, btnPrev, btnShuffle, btnRepeat, btnTimer;
    private ImageView albumArt;

    private ArrayList<Song> songs;
    private int position = 0;
    private boolean isShuffle = false;
    private boolean isRepeat = false;
    private boolean showingLyrics = false;

    private MusicService musicService;
    private boolean isBound = false;
    private final Handler handler = new Handler();
    private CountDownTimer sleepTimer;
    private AudioManager audioManager;
    private Song currentSong;

    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case "com.example.gaanesuno.ACTION_NEXT": playNextSong(); break;
                case "com.example.gaanesuno.ACTION_PREV": playPreviousSong(); break;
                case "com.example.gaanesuno.ACTION_CLOSE": finishAffinity(); break;
            }
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            MusicService.MusicBinder musicBinder = (MusicService.MusicBinder) binder;
            musicService = musicBinder.getService();
            isBound = true;
            playSong();
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initViews();

        // Notification receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.gaanesuno.ACTION_NEXT");
        filter.addAction("com.example.gaanesuno.ACTION_PREV");
        filter.addAction("com.example.gaanesuno.ACTION_CLOSE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }

        // POST_NOTIFICATION permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }

        // Get song list and position
        songs = (ArrayList<Song>) getIntent().getSerializableExtra("songsList");
        position = getIntent().getIntExtra("position", 0);
        if (songs == null || songs.isEmpty()) {
            Toast.makeText(this, "No songs found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                !Environment.isExternalStorageManager()) {
            startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
        }

        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        // Volume control
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeSeekBar.setMax(maxVolume);
        volumeSeekBar.setProgress(currentVolume);

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        findViewById(R.id.btnVolumeUp).setOnClickListener(v -> {
            int vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (vol < maxVolume) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol + 1, AudioManager.FLAG_PLAY_SOUND);
                volumeSeekBar.setProgress(vol + 1);
            }
        });

        findViewById(R.id.btnVolumeDown).setOnClickListener(v -> {
            int vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (vol > 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol - 1, AudioManager.FLAG_PLAY_SOUND);
                volumeSeekBar.setProgress(vol - 1);
            }
        });

        // Bind service
        Intent serviceIntent = new Intent(this, MusicService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Listeners
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> playNextSong());
        btnPrev.setOnClickListener(v -> playPreviousSong());

        btnShuffle.setOnClickListener(v -> {
            isShuffle = !isShuffle;
            btnShuffle.setImageResource(isShuffle ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off);
        });

        btnRepeat.setOnClickListener(v -> {
            isRepeat = !isRepeat;
            btnRepeat.setImageResource(isRepeat ? R.drawable.ic_repeat_one : R.drawable.ic_repeat_off);
            Toast.makeText(this, isRepeat ? "Repeat On" : "Repeat Off", Toast.LENGTH_SHORT).show();
        });

        btnTimer.setOnClickListener(v -> showTimerDialog());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && musicService != null) musicService.seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        albumArt.setOnClickListener(v -> {
            showingLyrics = !showingLyrics;
            lyricsView.setVisibility(showingLyrics ? View.VISIBLE : View.GONE);
            lyricsView.setText("Sample lyrics...\nYou can load actual lyrics here.");
        });

        findViewById(R.id.btnMore).setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(this, view);
            popup.getMenuInflater().inflate(R.menu.more_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_delete) {
                    deleteCurrentSong();
                    return true;
                } else if (id == R.id.menu_settings) {
                    openSettings();
                    return true;
                } else if (id == R.id.menu_info) {
                    Intent intent = new Intent(this, SongInfoActivity.class);
                    intent.putExtra("song", currentSong);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
            popup.show();
        });

        updateSeekBar();
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
        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        lyricsView = findViewById(R.id.lyricsView);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void playSong() {
        if (songs == null || songs.isEmpty()) return;
        currentSong = songs.get(position);
        MusicState.currentlyPlayingPosition = position;

        tvTitle.setSelected(true);
        tvTitle.setText(currentSong.getTitle());
        tvArtist.setText(currentSong.getArtist());

        if (currentSong.getAlbumArtUri() != null && !currentSong.getAlbumArtUri().isEmpty()) {
            albumArt.setImageURI(Uri.parse(currentSong.getAlbumArtUri()));
        } else {
            albumArt.setImageResource(R.drawable.ic_album_placeholder);
        }

        if (!musicService.isPlaying() || !musicService.isSameSong(currentSong.getPath())) {
            musicService.playMedia(currentSong);
        }
        btnPlayPause.setImageResource(R.drawable.ic_pause);

        musicService.setOnCompletionListener(() -> {
            if (isRepeat) playSong();
            else playNextSong();
        });
    }

    void playNextSong() {
        position = isShuffle ? new Random().nextInt(songs.size()) : (position + 1) % songs.size();
        playSong();
    }

    void playPreviousSong() {
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

        new AlertDialog.Builder(this)
                .setTitle("Set Sleep Timer")
                .setItems(options, (dialog, which) -> {
                    if (times[which] == 0) {
                        if (sleepTimer != null) {
                            sleepTimer.cancel();
                            sleepTimer = null;
                            btnTimer.setImageResource(R.drawable.ic_timer);
                            Toast.makeText(this, "Sleep timer canceled", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        int minutes = times[which];
                        if (sleepTimer != null) sleepTimer.cancel();
                        sleepTimer = new CountDownTimer(minutes * 60000L, 1000) {
                            public void onTick(long millisUntilFinished) {}
                            public void onFinish() {
                                if (musicService != null && musicService.isPlaying()) {
                                    musicService.pause();
                                    btnPlayPause.setImageResource(R.drawable.ic_play);
                                    Toast.makeText(PlayerActivity.this, "Playback stopped by sleep timer", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }.start();
                        btnTimer.setImageResource(R.drawable.ic_timer_on);
                        Toast.makeText(this, "Timer set for " + minutes + " minutes", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private void deleteCurrentSong() {
        if (currentSong == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete Song")
                .setMessage("Are you sure you want to delete this song?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (musicService != null) musicService.stop();

                    File file = new File(currentSong.getPath());
                    Uri contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
                            .appendPath(String.valueOf(currentSong.getId())).build();

                    if (file.exists() && file.delete()) {
                        getContentResolver().delete(contentUri, null, null);
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

                        Toast.makeText(this, "Song deleted", Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("songDeleted", true);
                        resultIntent.putExtra("deletedSongPath", currentSong.getPath());
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to delete. Try enabling All Files Access.", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    protected void onDestroy() {
        if (sleepTimer != null) sleepTimer.cancel();
        handler.removeCallbacksAndMessages(null);
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        unregisterReceiver(notificationReceiver);
        super.onDestroy();
    }
}
