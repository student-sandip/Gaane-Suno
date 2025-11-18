package com.example.gaanesuno;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.*;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.*;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {

    private static final int DELETE_REQUEST_CODE = 103;
    private ImageButton btnVolumeUp, btnVolumeDown;
    private TextView tvTitle, tvArtist, currentTime, totalTime;
    private SeekBar seekBar, volumeSeekBar;
    private ImageButton btnPlayPause, btnNext, btnPrev, btnShuffle, btnRepeat, btnTimer, btnMore;
    private ImageView albumArt;
    private Vibrator vibrator;
    private ArrayList<Song> songs;
    private int position = 0;

    private MusicService musicService;
    private boolean isBound = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private CountDownTimer sleepTimer;
    private AudioManager audioManager;
    private Song currentSong;
    private VolumeObserver volumeObserver;
    private SharedPreferences playerPrefs;

    private class VolumeObserver extends ContentObserver {
        VolumeObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (audioManager != null && volumeSeekBar != null) {
                volumeSeekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            }
        }
    }

    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) return;
            if ("com.example.gaanesuno.UPDATE_SONG".equals(intent.getAction())) {
                int newPos = intent.getIntExtra("position", -1);
                if (newPos != -1) {
                    position = newPos;
                    updateUIForCurrentSong();
                }
            }
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            MusicService.MusicBinder musicBinder = (MusicService.MusicBinder) binder;
            musicService = musicBinder.getService();
            isBound = true;
            loadPlayerPreferences();

            Song activitySong = songs.get(position);
            Song serviceSong = musicService.getCurrentSong();

            if (serviceSong != null && serviceSong.equals(activitySong)) {
                // Service is already managing this song, just sync UI
                updateUIForCurrentSong();
            } else {
                // This is a new song or a fresh start
                playSong();
            }
            updateSeekBar();
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
        playerPrefs = getSharedPreferences("PLAYER_SETTINGS", MODE_PRIVATE);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        IntentFilter filter = new IntentFilter("com.example.gaanesuno.UPDATE_SONG");
        ContextCompat.registerReceiver(this, notificationReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        songs = (ArrayList<Song>) getIntent().getSerializableExtra("songsList");
        position = getIntent().getIntExtra("position", 0);
        if (songs == null || songs.isEmpty()) {
            Toast.makeText(this, "No songs found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupAudio();

        Intent serviceIntent = new Intent(this, MusicService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        setClickListeners();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(0, R.anim.slide_out_down_fade);
            }
        });
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
        btnVolumeUp = findViewById(R.id.btnVolumeUp);
        btnVolumeDown = findViewById(R.id.btnVolumeDown);

        btnMore = findViewById(R.id.btnMore);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupAudio() {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        volumeSeekBar.setMax(maxVolume);
        volumeSeekBar.setProgress(currentVolume);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        volumeObserver = new VolumeObserver(new Handler(Looper.getMainLooper()));
        getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, volumeObserver);

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_SHOW_UI);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnVolumeUp.setOnClickListener(v -> {
            vibrateShort();
            int vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (vol < maxVolume) {
                vol++;
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_SHOW_UI);
                volumeSeekBar.setProgress(vol);
            }
        });

        btnVolumeDown.setOnClickListener(v -> {
            vibrateShort();
            int vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (vol > 0) {
                vol--;
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_SHOW_UI);
                volumeSeekBar.setProgress(vol);
            }
        });
    }

    private void setClickListeners() {
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> {
            if (musicService != null) {
                vibrateShort();
                musicService.playNextSongInternal();
            }
        });
        btnPrev.setOnClickListener(v -> {
            if (musicService != null) {
                vibrateShort();
                musicService.playPreviousSongInternal();
            }
        });

        btnShuffle.setOnClickListener(v -> {
            vibrateShort();
            boolean isShuffle = !musicService.isShuffle();
            musicService.setShuffle(isShuffle);
            playerPrefs.edit().putBoolean("shuffle_offline", isShuffle).apply();
            btnShuffle.setImageResource(isShuffle ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off);
            Toast.makeText(this, isShuffle ? "Shuffle On" : "Shuffle Off", Toast.LENGTH_SHORT).show();
        });

        btnRepeat.setOnClickListener(v -> {
            vibrateShort();
            boolean isRepeat = !musicService.isRepeat();
            musicService.setRepeat(isRepeat);
            playerPrefs.edit().putBoolean("repeat_offline", isRepeat).apply();
            btnRepeat.setImageResource(isRepeat ? R.drawable.ic_repeat_one : R.drawable.ic_repeat_off);
            Toast.makeText(this, isRepeat ? "Repeat On" : "Repeat Off", Toast.LENGTH_SHORT).show();
        });

        btnTimer.setOnClickListener(v -> showTimerDialog());

        btnMore.setOnClickListener(this::showPopupMenu);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser && musicService != null) musicService.seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.song_options_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_ringtone) {
                setAsRingtone();
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
        popupMenu.show();
    }

    private void setAsRingtone() {
        if (currentSong == null) {
            Toast.makeText(this, "No song selected!", Toast.LENGTH_SHORT).show();
            return;
        }

        File songFile = new File(currentSong.getPath());
        if (!songFile.exists()) {
            Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(this, "Please allow modify system settings!", Toast.LENGTH_LONG).show();
            return;
        }

        String mimeType = null;
        String fileName = songFile.getName();
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1);
        }

        if (!extension.isEmpty()) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.ROOT));
        }

        if (mimeType == null) {
            Toast.makeText(this, "Unsupported file type. Cannot set as ringtone.", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.TITLE, currentSong.getTitle());
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.Audio.Media.IS_RINGTONE, true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Uri newUri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
            try (OutputStream os = getContentResolver().openOutputStream(newUri)) {
                FileInputStream fis = new FileInputStream(songFile);
                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                fis.close();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to set as ringtone.", Toast.LENGTH_SHORT).show();
                return;
            }
            RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE, newUri);
            Toast.makeText(this, "Ringtone set successfully!", Toast.LENGTH_SHORT).show();
        } else {
            values.put(MediaStore.MediaColumns.DATA, songFile.getAbsolutePath());
            Uri uri = MediaStore.Audio.Media.getContentUriForPath(songFile.getAbsolutePath());
            getContentResolver().delete(uri, MediaStore.MediaColumns.DATA + "=?", new String[]{songFile.getAbsolutePath()});
            Uri newUri = getContentResolver().insert(uri, values);
            RingtoneManager.setActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_RINGTONE, newUri);
            Toast.makeText(this, "Ringtone set successfully!", Toast.LENGTH_SHORT).show();
        }
    }


    private void deleteCurrentSong() {
        if (currentSong == null) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Song")
                .setMessage("Are you sure you want to permanently delete this song from your device?")
                .setPositiveButton("Delete", (dialog, which) -> requestSongDeletion(currentSong))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void requestSongDeletion(Song song) {
        List<Uri> urisToDelete = new ArrayList<>();
        urisToDelete.add(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getId()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
            PendingIntent pendingIntent = MediaStore.createDeleteRequest(getContentResolver(), urisToDelete);
            try {
                startIntentSenderForResult(pendingIntent.getIntentSender(), DELETE_REQUEST_CODE, null, 0, 0, 0, null);
            } catch (IntentSender.SendIntentException e) {
                Toast.makeText(this, "Couldn't perform delete request.", Toast.LENGTH_SHORT).show();
            }
        } else { // Android 10 and below
            try {
                getContentResolver().delete(urisToDelete.get(0), null, null);
                handleSuccessfulDeletion(); // Success for pre-Android 10
            } catch (SecurityException e) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e instanceof RecoverableSecurityException) {
                    RecoverableSecurityException rse = (RecoverableSecurityException) e;
                    try {
                        startIntentSenderForResult(rse.getUserAction().getActionIntent().getIntentSender(), DELETE_REQUEST_CODE, null, 0, 0, 0, null);
                    } catch (IntentSender.SendIntentException ex) {
                        Toast.makeText(this, "Couldn't perform delete request.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Couldn't delete file. Permission denied.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void handleSuccessfulDeletion() {
        if (musicService != null) {
            musicService.stop();
        }
        Toast.makeText(this, "Song deleted", Toast.LENGTH_SHORT).show();

        Intent resultIntent = new Intent();
        resultIntent.putExtra("songDeleted", true);
        setResult(RESULT_OK, resultIntent);
        finish();
    }


    private void loadPlayerPreferences() {
        boolean isShuffle = playerPrefs.getBoolean("shuffle_offline", false);
        boolean isRepeat = playerPrefs.getBoolean("repeat_offline", false);

        if (musicService != null) {
            musicService.setShuffle(isShuffle);
            musicService.setRepeat(isRepeat);
        }

        btnShuffle.setImageResource(isShuffle ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off);
        btnRepeat.setImageResource(isRepeat ? R.drawable.ic_repeat_one : R.drawable.ic_repeat_off);

        long timerEndTime = playerPrefs.getLong("sleepTimerEnd_offline", 0);
        if (timerEndTime > 0) {
            long remaining = timerEndTime - System.currentTimeMillis();
            if (remaining > 0) startSleepTimer(remaining);
        }
    }

    private void playSong() {
        if (songs == null || songs.isEmpty() || musicService == null) return;
        currentSong = songs.get(position);

        musicService.playMedia(currentSong);
        updateUIForCurrentSong(); // Update UI after telling service to play
    }

    private void updateUIForCurrentSong() {
        currentSong = songs.get(position);
        MusicState.currentlyPlayingPosition = position;

        tvTitle.setSelected(true);
        tvArtist.setSelected(true);
        tvTitle.setText(currentSong.getTitle());
        tvArtist.setText(currentSong.getArtist());

        Uri artUri = (currentSong.getAlbumArtUri() != null) ? Uri.parse(currentSong.getAlbumArtUri()) : null;
        if (artUri != null) albumArt.setImageURI(artUri);
        else albumArt.setImageResource(R.drawable.ic_album_placeholder);
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
            if (musicService != null && isBound) {
                try {
                    seekBar.setMax(musicService.getDuration());
                    seekBar.setProgress(musicService.getCurrentPosition());
                    currentTime.setText(formatTime(musicService.getCurrentPosition()));
                    totalTime.setText(formatTime(musicService.getDuration()));
                    btnPlayPause.setImageResource(musicService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
                } catch (Exception e) {
                    // ignore
                }
            }
            updateSeekBar();
        }, 1000);
    }

    private String formatTime(int ms) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(ms),
                TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(ms)));
    }

    private void showTimerDialog() {
        vibrateShort();
        if (sleepTimer != null) {
            new MaterialAlertDialogBuilder(this)
                .setTitle("Sleep Timer Active")
                .setMessage("A timer is currently running.")
                .setPositiveButton("Cancel Timer", (d, w) -> cancelSleepTimer())
                .setNegativeButton("Close", null)
                .show();
        } else {
            final String[] options = {"5 minutes", "15 minutes", "30 minutes", "45 minutes", "60 minutes"};
            final int[] minutes = {5, 15, 30, 45, 60};
            new MaterialAlertDialogBuilder(this)
                .setTitle("Set Sleep Timer")
                .setItems(options, (d, which) -> setSleepTimer(minutes[which]))
                .show();
        }
    }

    private void setSleepTimer(int minutes) {
        long duration = minutes * 60 * 1000L;
        playerPrefs.edit().putLong("sleepTimerEnd_offline", System.currentTimeMillis() + duration).apply();
        startSleepTimer(duration);
        Toast.makeText(this, "Timer set for " + minutes + " minutes", Toast.LENGTH_SHORT).show();
    }

    private void startSleepTimer(long duration) {
        if (sleepTimer != null) sleepTimer.cancel();
        if (duration <= 0) {
            cancelSleepTimer();
            return;
        }
        btnTimer.setImageResource(R.drawable.ic_timer_on);
        sleepTimer = new CountDownTimer(duration, 1000) {
            public void onTick(long millisUntilFinished) {}
            public void onFinish() {
                if (musicService != null && musicService.isPlaying()) musicService.pause();
                cancelSleepTimer();
                Toast.makeText(PlayerActivity.this, "Playback stopped by timer", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void cancelSleepTimer() {
        if (sleepTimer != null) sleepTimer.cancel();
        sleepTimer = null;
        playerPrefs.edit().remove("sleepTimerEnd_offline").apply();
        btnTimer.setImageResource(R.drawable.ic_timer);
    }

    private void vibrateShort() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DELETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                handleSuccessfulDeletion();
            } else {
                Toast.makeText(this, "Deletion cancelled or failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (isBound) unbindService(serviceConnection);
        if (volumeObserver != null) getContentResolver().unregisterContentObserver(volumeObserver);
        unregisterReceiver(notificationReceiver);
        if (sleepTimer != null) sleepTimer.cancel();
    }
}
