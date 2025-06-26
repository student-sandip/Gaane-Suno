package com.example.gaanesuno;

import static com.example.gaanesuno.MusicState.songList;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
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
    private Vibrator vibrator;
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
    private VolumeObserver volumeObserver;

    private class VolumeObserver extends ContentObserver {
        VolumeObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (audioManager != null && volumeSeekBar != null) {
                int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                volumeSeekBar.setProgress(currentVol);
            }
        }
    }

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

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.gaanesuno.ACTION_NEXT");
        filter.addAction("com.example.gaanesuno.ACTION_PREV");
        filter.addAction("com.example.gaanesuno.ACTION_CLOSE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }

        songs = (ArrayList<Song>) getIntent().getSerializableExtra("songsList");
        position = getIntent().getIntExtra("position", 0);
        if (songs == null || songs.isEmpty()) {
            Toast.makeText(this, "No songs found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volumeSeekBar.setMax(maxVolume);
        volumeSeekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));

        volumeObserver = new VolumeObserver(new Handler());
        getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, volumeObserver);

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_SHOW_UI);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        findViewById(R.id.btnVolumeUp).setOnClickListener(v -> {
            vibrateShort();
            int vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (vol < maxVolume) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol + 1, AudioManager.FLAG_PLAY_SOUND);
            }
        });

        findViewById(R.id.btnVolumeDown).setOnClickListener(v -> {
            vibrateShort();
            int vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (vol > 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol - 1, AudioManager.FLAG_PLAY_SOUND);
            }
        });

        Intent serviceIntent = new Intent(this, MusicService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> playNextSong());
        btnPrev.setOnClickListener(v -> playPreviousSong());

        btnShuffle.setOnClickListener(v -> {
            vibrateShort();
            isShuffle = !isShuffle;
            btnShuffle.setImageResource(isShuffle ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off);
            Toast.makeText(this, isShuffle ? "Shuffle On" : "Shuffle Off", Toast.LENGTH_SHORT).show();
        });

        btnRepeat.setOnClickListener(v -> {
            vibrateShort();
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


        findViewById(R.id.btnMore).setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(this, view);
            popup.getMenuInflater().inflate(R.menu.more_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_settings) {
                    openSettings();
                    return true;
                } else if (id == R.id.menu_delete) {
                    deleteCurrentSong();
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        }
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
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void vibrateShort() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(40);
            }
        }
    }
    private void playSong() {
        if (songs == null || songs.isEmpty() || musicService == null) return;
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

        if (musicService.isSameSong(currentSong.getPath())) {
            if (!musicService.isPlaying()) {
                musicService.resume();
            }
        } else {
            musicService.playMedia(currentSong);
        }
        btnPlayPause.setImageResource(R.drawable.ic_pause);

        Intent updateIntent = new Intent("com.example.gaanesuno.UPDATE_SONG");
        updateIntent.putExtra("position", position);
        sendBroadcast(updateIntent);

        musicService.setOnCompletionListener(() -> {
            if (isRepeat) {
                musicService.playMedia(currentSong);
            } else {
                playNextSong();
            }
        });
    }

    void playNextSong() {
        position = isShuffle ? new Random().nextInt(songs.size()) : (position + 1) % songs.size();
        playSong();
        vibrateShort();
    }

    void playPreviousSong() {
        if (musicService != null && musicService.getCurrentPosition() > 5000) {
            musicService.seekTo(0);
            vibrateShort();
        } else {
            position = isShuffle ? new Random().nextInt(songs.size()) : (position - 1 + songs.size()) % songs.size();
            playSong();
            vibrateShort();
        }
    }


    private void togglePlayPause() {
        vibrateShort();
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
        }, 50);
    }

    private String formatTime(int milliseconds) {
        return String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(milliseconds),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60);
    }

    private void showTimerDialog() {
        vibrateShort();
        final String[] options = {"5 minutes", "15 minutes", "30 minutes", "45 minutes", "60 minutes", "120 minutes", "Cancel Timer"};
        final int[] times = {5, 15, 30, 45, 60, 120, 0};

        boolean timerRunning = (sleepTimer != null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Sleep Timer");

        builder.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, options) {
            @Override
            public boolean isEnabled(int position) {
                // Only "Cancel Timer" (last one) is enabled if timer is already running
                return !timerRunning || position == options.length - 1;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (timerRunning && position != options.length - 1) {
                    view.setEnabled(false);
                    view.setAlpha(0.5f);
                } else {
                    view.setEnabled(true);
                    view.setAlpha(1f);
                }
                return view;
            }
        }, (dialog, which) -> {
            if (times[which] == 0) { // Cancel Timer
                if (sleepTimer != null) {
                    sleepTimer.cancel();
                    sleepTimer = null;
                    btnTimer.setImageResource(R.drawable.ic_timer);
                    Toast.makeText(this, "Sleep timer canceled", Toast.LENGTH_SHORT).show();
                }
            } else {
                int minutes = times[which];
                if (sleepTimer != null) sleepTimer.cancel();

                long duration = minutes * 60 * 1000L;
                long fadeStartTime = duration - 5000L; // Last 5 seconds fade

                sleepTimer = new CountDownTimer(duration, 1000) {
                    public void onTick(long millisUntilFinished) {
                        if (millisUntilFinished <= 5000 && musicService != null) {
                            float volume = millisUntilFinished / 5000f;
                            musicService.setPlayerVolume(volume);
                        }
                    }

                    public void onFinish() {
                        if (musicService != null && musicService.isPlaying()) {
                            musicService.pause();
                            musicService.setPlayerVolume(1.0f);
                            btnPlayPause.setImageResource(R.drawable.ic_play);
                            btnTimer.setImageResource(R.drawable.ic_timer);
                            Toast.makeText(PlayerActivity.this, "Playback stopped by sleep timer", Toast.LENGTH_SHORT).show();
                        }
                    }
                }.start();

                btnTimer.setImageResource(R.drawable.ic_timer_on);
                Toast.makeText(this, "Timer set for " + minutes + " minutes", Toast.LENGTH_SHORT).show();
            }
        });

        builder.show();
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
        if (volumeObserver != null) {
            getContentResolver().unregisterContentObserver(volumeObserver);
        }
        unregisterReceiver(notificationReceiver);
        super.onDestroy();
    }
}