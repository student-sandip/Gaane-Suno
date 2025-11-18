package com.example.gaanesuno;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OnlineMusicService extends Service {

    private MediaPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;
    private AudioManager audioManager;

    private long currentTrackId = -1;
    private String currentTitle = "";
    private String currentArtist = "";
    private String currentUrl = "";
    private String currentImage = "";
    private int currentBitrate = 128;

    private boolean isPaused = false;
    private boolean isShuffle = false;
    private boolean isRepeat = false;

    private static OnlineSong currentSong = null;

    private static final String CHANNEL_ID = "MUSIC_CHANNEL";
    private static final int NOTIFICATION_ID = 1;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    private List<OnlineSong> currentList = new ArrayList<>();
    private int currentIndex = -1;

    private final List<OnlineSong> currentRelatedList = new ArrayList<>();
    private int currentRelatedIndex = -1;

    public static OnlineSong getCurrentSong() {
        return currentSong;
    }

    private final AudioManager.OnAudioFocusChangeListener focusChangeListener = focusChange -> {
        if (mediaPlayer == null) return;
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    isPaused = true;
                    updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
                    showNotification(false);
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                if (isPaused && mediaPlayer != null) {
                    mediaPlayer.start();
                    isPaused = false;
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                    showNotification(true);
                }
                break;
        }
    };

    private final BroadcastReceiver playerControlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("ACTION");
            if (action == null) return;

            switch (action) {
                case "TOGGLE_PLAY_PAUSE": togglePlayPause(); break;
                case "SEEK_TO":
                    long seekPos = intent.getLongExtra("POSITION", 0);
                    if (mediaPlayer != null) {
                        try {
                            mediaPlayer.seekTo((int) seekPos);
                            sendProgressUpdate();
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    break;
                case "NEXT_SONG": playNextFromCurrentList(); break;
                case "PREVIOUS_SONG": playPreviousFromCurrentList(); break;
                case "SET_SHUFFLE": isShuffle = intent.getBooleanExtra("SHUFFLE", false); break;
                case "SET_REPEAT": isRepeat = intent.getBooleanExtra("REPEAT", false); break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        mediaSession = new MediaSessionCompat(this, "OnlineMusicSession");
        mediaSession.setCallback(mediaSessionCallback);
        mediaSession.setActive(true);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        IntentFilter filter = new IntentFilter("ONLINE_PLAYER_CONTROL");
        ContextCompat.registerReceiver(this, playerControlReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isNetworkAvailable()) {
            Intent networkIntent = new Intent(this, NetworkRequestActivity.class);
            networkIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(networkIntent);
            return START_NOT_STICKY;
        }
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if ("SEEK_TO".equals(action)) {
                    long pos = intent.getLongExtra("POSITION", 0);
                    if (mediaPlayer != null) {
                        try {
                            mediaPlayer.seekTo((int) pos);
                            sendProgressUpdate();
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    return START_STICKY;
                }

                switch (action) {
                    case "ACTION_NEXT": playNextFromCurrentList(); return START_STICKY;
                    case "ACTION_PREV": playPreviousFromCurrentList(); return START_STICKY;
                }
            }

            if (intent.hasExtra("songsList")) {
                try {
                    currentList = (ArrayList<OnlineSong>) intent.getSerializableExtra("songsList");
                    currentIndex = intent.getIntExtra("position", 0);
                } catch (Exception e) { e.printStackTrace(); }
            }

            long trackId = intent.getLongExtra("SONG_TRACK_ID", -1);
            String url = intent.getStringExtra("SONG_URL");
            String title = intent.getStringExtra("SONG_TITLE");
            String artist = intent.getStringExtra("ARTIST_NAME");
            String image = intent.getStringExtra("SONG_IMAGE");

            if (trackId != -1) {
                if (trackId == currentTrackId && mediaPlayer != null && mediaPlayer.isPlaying()) {
                    return START_STICKY;
                }

                currentTrackId = trackId;
                currentUrl = url;
                currentTitle = title != null ? title : "Unknown";
                currentArtist = artist != null ? artist : "Unknown";
                currentImage = image != null ? image : "";
                currentSong = new OnlineSong(currentTrackId, currentTitle, currentArtist, currentImage, currentUrl);

                synchronized (currentRelatedList) {
                    currentRelatedList.clear();
                    currentRelatedIndex = -1;
                }

                saveCurrentSong();
                startStreaming(url);
            }
        }
        return START_STICKY;
    }

    private void startStreaming(String url) {
        releasePlayer();
        audioManager.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mediaPlayer.setOnInfoListener((mp, what, extra) -> {
            if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) sendBufferingUpdate(true);
            else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) sendBufferingUpdate(false);
            return true;
        });

        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
            return;
        }

        mediaPlayer.setOnPreparedListener(mp -> {
            mp.start();
            isPaused = false;
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
            showNotification(true);
            sendSongInfo();
            startProgressUpdates();
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            stopProgressUpdates();

            if (currentList != null && !currentList.isEmpty()) {
                if (isRepeat) {
                    playNextSong(currentList.get(currentIndex));
                } else {
                    currentIndex = (currentIndex + 1) % currentList.size();
                    playNextSong(currentList.get(currentIndex));
                }
                return;
            }

            synchronized (currentRelatedList) {
                if (!currentRelatedList.isEmpty()) {
                    if (isShuffle) {
                        int rnd = new Random().nextInt(currentRelatedList.size());
                        if (currentRelatedList.size() > 1 && rnd == currentRelatedIndex)
                            rnd = (rnd + 1) % currentRelatedList.size();
                        currentRelatedIndex = rnd;
                        playNextSong(currentRelatedList.get(rnd));
                    } else {
                        int nextIdx = (currentRelatedIndex + 1) % currentRelatedList.size();
                        currentRelatedIndex = nextIdx;
                        playNextSong(currentRelatedList.get(nextIdx));
                    }
                    return;
                }
            }

            fetchAndPlayNextRelatedSong(currentTitle);
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            stopProgressUpdates();
            stopSelf();
            return true;
        });
    }

    private void playNextFromCurrentList() {
        if (currentList != null && !currentList.isEmpty()) {
            currentIndex = (currentIndex + 1) % currentList.size();
            playNextSong(currentList.get(currentIndex));
        } else {
            fetchAndPlayNextRelatedSong(currentTitle);
        }
    }

    private void playPreviousFromCurrentList() {
        if (currentList != null && !currentList.isEmpty()) {
            currentIndex = (currentIndex - 1 + currentList.size()) % currentList.size();
            playNextSong(currentList.get(currentIndex));
        } else {
            fetchAndPlayPreviousRelatedSong(currentTitle);
        }
    }

    private void playNextSong(OnlineSong nextSong) {
        if (nextSong == null) return;
        currentTrackId = nextSong.getTrackId();
        currentTitle = nextSong.getTitle();
        currentArtist = nextSong.getArtist();
        currentUrl = nextSong.getPreviewUrl();
        currentImage = nextSong.getImageUrl();
        currentSong = nextSong;
        saveCurrentSong();
        startStreaming(nextSong.getPreviewUrl());
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause(); isPaused = true;
        } else {
            mediaPlayer.start(); isPaused = false;
        }
        updatePlaybackState(isPaused ? PlaybackStateCompat.STATE_PAUSED : PlaybackStateCompat.STATE_PLAYING);
        showNotification(!isPaused);
        sendProgressUpdate();
    }

    private void sendSongInfo() {
        if (currentSong == null) return;
        Intent info = new Intent("ONLINE_PLAYER_UPDATE").setPackage(getPackageName());
        info.putExtra("SONG_TRACK_ID", currentSong.getTrackId());
        info.putExtra("SONG_TITLE", currentSong.getTitle());
        info.putExtra("ARTIST_NAME", currentSong.getArtist());
        info.putExtra("SONG_URL", currentSong.getPreviewUrl());
        info.putExtra("SONG_IMAGE", currentSong.getImageUrl());
        sendBroadcast(info);
    }

    private void sendBufferingUpdate(boolean isBuffering) {
        Intent i = new Intent("ONLINE_PLAYER_UPDATE").setPackage(getPackageName());
        i.putExtra("BUFFERING_STATUS", isBuffering);
        sendBroadcast(i);
    }

    private void sendProgressUpdate() {
        if (mediaPlayer != null) {
            Intent i = new Intent("ONLINE_PLAYER_UPDATE").setPackage(getPackageName());
            try {
                i.putExtra("CURRENT_POSITION", mediaPlayer.getCurrentPosition());
                i.putExtra("DURATION", mediaPlayer.getDuration());
                i.putExtra("IS_PLAYING", mediaPlayer.isPlaying());
                sendBroadcast(i);
            } catch (IllegalStateException ignored) {}
        }
    }

    private void startProgressUpdates() {
        stopProgressUpdates();
        progressRunnable = () -> {
            sendProgressUpdate();
            progressHandler.postDelayed(progressRunnable, 1000);
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdates() {
        if (progressRunnable != null) progressHandler.removeCallbacks(progressRunnable);
    }

    private void updatePlaybackState(int state) {
        try {
            long position = mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
            long actions = PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                    PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
            PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder()
                    .setActions(actions)
                    .setState(state, position, 1.0f);
            mediaSession.setPlaybackState(builder.build());
        } catch (Exception ignored) {}
    }

    private void showNotification(boolean isPlaying) {
        Intent openIntent = new Intent(this, OnlinePlayerActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE);

        PendingIntent prevIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        PendingIntent toggleIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE);
        PendingIntent nextIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle(currentTitle != null ? currentTitle : "GaaneSuno")
                .setContentText(currentArtist)
                .setContentIntent(openPendingIntent)
                .addAction(R.drawable.ic_prev, "Previous", prevIntent)
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play, isPlaying ? "Pause" : "Play", toggleIntent)
                .addAction(R.drawable.ic_next, "Next", nextIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setOngoing(isPlaying)
                .setOnlyAlertOnce(true);

        startForeground(NOTIFICATION_ID, builder.build());
    }

    private void saveCurrentSong() {
        SharedPreferences prefs = getSharedPreferences("CURRENT_SONG", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("trackId", currentTrackId);
        editor.putString("title", currentTitle);
        editor.putString("artist", currentArtist);
        editor.putString("url", currentUrl);
        editor.putString("image", currentImage);
        editor.putInt("bitrate", currentBitrate);
        editor.apply();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Online Music Playback", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private final MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {
        @Override public void onPlay() { togglePlayPause(); }
        @Override public void onPause() { togglePlayPause(); }
        @Override public void onStop() { stopSelf(); }
        @Override public void onSkipToNext() { playNextFromCurrentList(); }
        @Override public void onSkipToPrevious() { playPreviousFromCurrentList(); }
    };

    private void fetchAndPlayNextRelatedSong(String currentTitle) {
        new Thread(() -> {
            try {
                if (currentTitle == null || currentTitle.isEmpty()) return;
                String query = java.net.URLEncoder.encode(currentTitle, "UTF-8");

                URL url = new URL("https://itunes.apple.com/search?term=" + query + "&entity=song&limit=20");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();
                connection.disconnect();

                JSONObject jsonObject = new JSONObject(response.toString());
                JSONArray results = jsonObject.optJSONArray("results");
                if (results == null || results.length() == 0) return;

                List<OnlineSong> filtered = new ArrayList<>();
                for (int i = 0; i < results.length(); i++) {
                    JSONObject obj = results.getJSONObject(i);
                    long trackId = obj.optLong("trackId");
                    String title = obj.optString("trackName", "Unknown");
                    String artist = obj.optString("artistName", "Unknown");
                    String image = obj.optString("artworkUrl100", "");
                    String previewUrl = obj.optString("previewUrl", "");
                    if (previewUrl != null && !previewUrl.isEmpty()) {
                        filtered.add(new OnlineSong(trackId, title, artist, image, previewUrl));
                    }
                }

                if (filtered.isEmpty()) return;

                OnlineSong toPlay;
                synchronized (currentRelatedList) {
                    currentRelatedList.clear();
                    currentRelatedList.addAll(filtered);
                    if (isShuffle) {
                        int pick = new Random().nextInt(currentRelatedList.size());
                        toPlay = currentRelatedList.get(pick);
                        currentRelatedIndex = pick;
                    } else {
                        toPlay = currentRelatedList.get(0);
                        currentRelatedIndex = 0;
                    }
                }

                if (toPlay != null) playNextSong(toPlay);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void fetchAndPlayPreviousRelatedSong(String currentTitle) {
        new Thread(() -> {
            try {
                synchronized (currentRelatedList) {
                    if (!currentRelatedList.isEmpty()) {
                        if (isShuffle) {
                            int pick = new Random().nextInt(currentRelatedList.size());
                            currentRelatedIndex = pick;
                            playNextSong(currentRelatedList.get(pick));
                            return;
                        } else {
                            if (currentRelatedIndex > 0) {
                                int prev = (currentRelatedIndex - 1 + currentRelatedList.size()) % currentRelatedList.size();
                                currentRelatedIndex = prev;
                                playNextSong(currentRelatedList.get(prev));
                                return;
                            }
                        }
                    }
                }
                fetchAndPlayNextRelatedSong(currentTitle);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void releasePlayer() {
        stopProgressUpdates();
        if (mediaPlayer != null) {
            try { if (mediaPlayer.isPlaying()) mediaPlayer.stop(); } catch (Exception ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releasePlayer();
        if (mediaSession != null) mediaSession.release();
        try { unregisterReceiver(playerControlReceiver); } catch (Exception ignored) {}
        synchronized (currentRelatedList) { currentRelatedList.clear(); }
        currentSong = null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void saveCurrentSource(String source) {
        getSharedPreferences("PLAY_SOURCE", MODE_PRIVATE)
                .edit()
                .putString("last_source", source)
                .apply();
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
