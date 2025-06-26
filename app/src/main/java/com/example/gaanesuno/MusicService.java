package com.example.gaanesuno;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import java.io.IOException;

public class MusicService extends Service {

    public static final String ACTION_PLAY_PAUSE = "com.example.gaanesuno.ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "com.example.gaanesuno.ACTION_NEXT";
    public static final String ACTION_PREV = "com.example.gaanesuno.ACTION_PREV";
    public static final String ACTION_CLOSE = "com.example.gaanesuno.ACTION_CLOSE";

    private static final int NOTIFICATION_ID = 101;
    private static final String CHANNEL_ID = "GaaneSuno_Channel";

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;
    private String currentPath = "";
    private boolean isPlaying = false;
    private Song currentSong;

    private OnSongCompleteListener onSongCompleteListener;

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PREV:
                    playPrevious();
                    break;
                case ACTION_PLAY_PAUSE:
                    if (isPlaying()) pause(); else resume();
                    break;
                case ACTION_NEXT:
                    playNext();
                    break;
                case ACTION_CLOSE:
                    stop();
                    break;
            }
        }
        return START_STICKY;
    }

    public void playMedia(Song song) {
        if (song == null) return;
        if (isSameSong(song.getPath()) && isPlaying()) return;

        stopMedia();
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());

            mediaPlayer.setDataSource(this, Uri.parse(song.getPath()));
            mediaPlayer.prepare();
            mediaPlayer.start();

            currentPath = song.getPath();
            currentSong = song;
            isPlaying = true;

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                if (onSongCompleteListener != null) {
                    onSongCompleteListener.onComplete();
                }
                // Auto play next if available
//                playNext(); // or notify completion only if you want manual next
            });

            showNotification(true);
            notifySongChanged();  // Broadcast song change to UI

        } catch (IOException e) {
            e.printStackTrace();
            stopMedia();
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            showNotification(false);
            notifyPlaybackStateChanged();
        }
    }

    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            showNotification(true);
            notifyPlaybackStateChanged();
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public boolean isSameSong(String path) {
        return currentPath.equals(path);
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) mediaPlayer.seekTo(position);
    }

    private void stopMedia() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false;
        }
    }

    public void stop() {
        stopMedia();
        stopForeground(true);
        stopSelf();
    }

    public void setOnCompletionListener(OnSongCompleteListener listener) {
        this.onSongCompleteListener = listener;
    }

    public interface OnSongCompleteListener {
        void onComplete();
    }

    private void playPrevious() {
        if (MusicState.currentlyPlayingPosition > 0) {
            MusicState.currentlyPlayingPosition--;
            playMedia(MusicState.songList.get(MusicState.currentlyPlayingPosition));
            notifySongChanged();
        }
    }

    private void playNext() {
        if (MusicState.currentlyPlayingPosition < MusicState.songList.size() - 1) {
            MusicState.currentlyPlayingPosition++;
            playMedia(MusicState.songList.get(MusicState.currentlyPlayingPosition));
            notifySongChanged();
        }
    }

    private void showNotification(boolean isPlaying) {
        createNotificationChannel();

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, PlayerActivity.class)
                        .putExtra("songsList", MusicState.songList)
                        .putExtra("position", MusicState.currentlyPlayingPosition),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent prevPending = getServicePendingIntent(ACTION_PREV, 1);
        PendingIntent playPausePending = getServicePendingIntent(ACTION_PLAY_PAUSE, 2);
        PendingIntent nextPending = getServicePendingIntent(ACTION_NEXT, 3);
        PendingIntent closePending = getServicePendingIntent(ACTION_CLOSE, 4);

        Bitmap albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.ic_album_placeholder);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentSong != null ? currentSong.getTitle() : "GaaneSuno")
                .setContentText(currentSong != null ? currentSong.getArtist() : "Unknown Artist")
                .setSmallIcon(R.drawable.ic_music_note)
                .setLargeIcon(albumArt)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_prev, "Previous", prevPending)
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play,
                        isPlaying ? "Pause" : "Play", playPausePending)
                .addAction(R.drawable.ic_next, "Next", nextPending)
                .addAction(R.drawable.ic_close, "Close", closePending)
                .setStyle(new MediaStyle().setShowActionsInCompactView(0, 1, 2, 3))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .setOngoing(isPlaying);

        startForeground(NOTIFICATION_ID, builder.build());
    }

    private PendingIntent getServicePendingIntent(String action, int requestCode) {
        Intent intent = new Intent(this, MusicService.class).setAction(action);
        return PendingIntent.getService(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "GaaneSuno Playback", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Playback controls for GaaneSuno");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void notifySongChanged() {
        Intent intent = new Intent("com.example.gaanesuno.SONG_CHANGED");
        intent.putExtra("position", MusicState.currentlyPlayingPosition);
        sendBroadcast(intent);
    }

    private void notifyPlaybackStateChanged() {
        Intent intent = new Intent("com.example.gaanesuno.PLAYBACK_STATE_CHANGED");
        intent.putExtra("isPlaying", isPlaying);
        sendBroadcast(intent);
    }

    public void setPlayerVolume(float volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume, volume);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stop();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMedia();
        stopForeground(true);
    }
}