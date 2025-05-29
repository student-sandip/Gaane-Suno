package com.example.gaanesuno;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class MusicService extends Service {

    private MediaPlayer mediaPlayer;
    private final IBinder binder = new MusicBinder();
    private OnSongCompleteListener onSongCompleteListener;

    private static final int NOTIFICATION_ID = 101;
    private static final String CHANNEL_ID = "GaanaSuno_Channel";

    public void stop() {
        stopMedia();
        stopForeground(true);
        stopSelf();
    }

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

    private String currentPath = "";

    public boolean isSameSong(String path) {
        return currentPath.equals(path);
    }

    public void playMedia(String path) {
        if (isSameSong(path) && isPlaying()) {
            return;
        }

        stopMedia(); // Stop any previous media

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());

            mediaPlayer.setDataSource(this, Uri.parse(path));
            mediaPlayer.prepare();
            mediaPlayer.start();
            currentPath = path;

            mediaPlayer.setOnCompletionListener(mp -> {
                if (onSongCompleteListener != null) {
                    onSongCompleteListener.onComplete();
                }
            });

            showNotification("Playing");
            notifySongChanged(); // 🔔 Notify MainActivity about the song change

        } catch (Exception e) {
            e.printStackTrace();
            stopMedia(); // Clean up if something went wrong
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            showNotification("Paused");
        }
    }

    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            showNotification("Playing");
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    private void stopMedia() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void setOnCompletionListener(OnSongCompleteListener listener) {
        this.onSongCompleteListener = listener;
    }

    public interface OnSongCompleteListener {
        void onComplete();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMedia();
        stopForeground(true);
    }

    private void showNotification(String playbackState) {
        createNotificationChannel();

        Intent intent = new Intent(this, PlayerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GaanaSuno")
                .setContentText(playbackState)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        startForeground(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "GaanaSuno Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Playback controls for GaanaSuno");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void notifySongChanged() {
        Intent intent = new Intent("com.example.gaanesuno.SONG_CHANGED");
        intent.putExtra("position", MusicState.currentlyPlayingPosition);
        sendBroadcast(intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf(); // Stop the service when the app is closed from recent apps
        super.onTaskRemoved(rootIntent);
    }
}
