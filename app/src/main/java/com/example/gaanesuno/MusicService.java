package com.example.gaanesuno;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import java.io.IOException;
import java.util.Random;

import android.media.audiofx.LoudnessEnhancer;

public class MusicService extends Service implements AudioManager.OnAudioFocusChangeListener {

    private LoudnessEnhancer loudnessEnhancer;
    public static final String ACTION_PLAY_PAUSE = "com.example.gaanesuno.ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "com.example.gaanesuno.ACTION_NEXT";
    public static final String ACTION_PREV = "com.example.gaanesuno.ACTION_PREV";
    public static final String ACTION_CLOSE = "com.example.gaanesuno.ACTION_CLOSE";

    private static final int NOTIFICATION_ID = 101;
    private static final String CHANNEL_ID = "GaaneSuno_Channel";

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;
    private Song currentSong;
    private MediaSessionCompat mediaSession;
    private AudioManager audioManager;

    private boolean isShuffle = false;
    private boolean isRepeat = false;
    private int resumePosition = 0; // To store position on audio focus loss
    private boolean pausedByFocusLoss = false; // Flag to check if pause was due to focus loss

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Nullable
    public Song getCurrentSong() {
        return currentSong;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PREV:
                    playPreviousSongInternal();
                    break;
                case ACTION_PLAY_PAUSE:
                    if (isPlaying()) {
                        pause();
                    } else {
                        resume();
                    }
                    break;
                case ACTION_NEXT:
                    playNextSongInternal();
                    break;
                case ACTION_CLOSE:
                    stop();
                    break;
            }
        }
        return START_STICKY;
    }

    public void playMedia(Song song) {
        playMedia(song, 0);
    }

    public void playMedia(Song song, int startPosition) {
        if (song == null) return;

        // No need to restart if it's the same song and already playing
        if (isPlaying() && song.equals(currentSong)) {
            return;
        }

        stopMedia();
        if (requestAudioFocus()) {
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());

                mediaPlayer.setDataSource(this, Uri.parse(song.getPath()));
                mediaPlayer.prepare();
                initLoudnessEnhancer();

                if (startPosition > 0) {
                    mediaPlayer.seekTo(startPosition);
                }

                mediaPlayer.start();
                currentSong = song;
                resumePosition = startPosition;

                initMediaSession();
                showNotification(true);

                mediaPlayer.setOnCompletionListener(mp -> {
                    if (isRepeat) {
                        playMedia(currentSong);
                    } else {
                        playNextSongInternal();
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
            showNotification(false);
            // User-initiated pause should not abandon audio focus
        }
    }

    public void resume() {
        if (requestAudioFocus()) {
            if (mediaPlayer == null && currentSong != null) {
                playMedia(currentSong, resumePosition);
            } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
            showNotification(true);
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
        if (mediaPlayer != null) mediaPlayer.seekTo(position);
    }

    private void stopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        abandonAudioFocus();
    }

    public void stop() {
        stopMedia();
        stopForeground(true);
        stopSelf();
    }

    public void setShuffle(boolean shuffle) {
        isShuffle = shuffle;
    }

    public boolean isShuffle() {
        return isShuffle;
    }

    public void setRepeat(boolean repeat) {
        isRepeat = repeat;
    }

    public boolean isRepeat() {
        return isRepeat;
    }

    public int getNextSongPosition() {
        if (isShuffle) {
            return new Random().nextInt(MusicState.songList.size());
        } else {
            return (MusicState.currentlyPlayingPosition + 1) % MusicState.songList.size();
        }
    }

    public int getPreviousSongPosition() {
        if (isShuffle) {
            return new Random().nextInt(MusicState.songList.size());
        } else {
            return (MusicState.currentlyPlayingPosition - 1 + MusicState.songList.size()) % MusicState.songList.size();
        }
    }

    public void playNextSongInternal() {
        if (MusicState.songList == null || MusicState.songList.isEmpty()) return;
        int nextPos = getNextSongPosition();
        MusicState.currentlyPlayingPosition = nextPos;
        Song nextSong = MusicState.songList.get(nextPos);
        playMedia(nextSong);
        broadcastUpdate(nextPos);
    }

    public void playPreviousSongInternal() {
        if (MusicState.songList == null || MusicState.songList.isEmpty()) return;
        if (getCurrentPosition() > 3000) {
            seekTo(0);
        } else {
            int prevPos = getPreviousSongPosition();
            MusicState.currentlyPlayingPosition = prevPos;
            Song prevSong = MusicState.songList.get(prevPos);
            playMedia(prevSong);
            broadcastUpdate(prevPos);
        }
    }

    private void broadcastUpdate(int newPosition) {
        Intent intent = new Intent("com.example.gaanesuno.UPDATE_SONG");
        intent.putExtra("position", newPosition);
        sendBroadcast(intent.setPackage(getPackageName()));
    }

    private void showNotification(boolean isPlaying) {
        createNotificationChannel();

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("songsList", MusicState.songList);
        intent.putExtra("position", MusicState.currentlyPlayingPosition);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent prevPending = getServicePendingIntent(ACTION_PREV, 1);
        PendingIntent playPausePending = getServicePendingIntent(ACTION_PLAY_PAUSE, 2);
        PendingIntent nextPending = getServicePendingIntent(ACTION_NEXT, 3);
        PendingIntent closePending = getServicePendingIntent(ACTION_CLOSE, 4);

        Bitmap albumArt = null;
        if (currentSong != null && currentSong.getAlbumArtUri() != null) {
            try {
                albumArt = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(currentSong.getAlbumArtUri()));
            } catch (IOException e) {
                // ignore
            }
        }
        if (albumArt == null) {
            albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.ic_album_placeholder);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentSong != null ? currentSong.getTitle() : "GaaneSuno")
                .setContentText(currentSong != null ? currentSong.getArtist() : "Unknown Artist")
                .setSmallIcon(R.drawable.ic_music_note)
                .setLargeIcon(albumArt)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_prev, "Previous", prevPending)
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play, isPlaying ? "Pause" : "Play", playPausePending)
                .addAction(R.drawable.ic_next, "Next", nextPending)
                .addAction(R.drawable.ic_close, "Close", closePending)
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(isPlaying);

        startForeground(NOTIFICATION_ID, builder.build());
    }

    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, "GaaneSunoMediaSession");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                resume();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onSkipToNext() {
                playNextSongInternal();
            }

            @Override
            public void onSkipToPrevious() {
                playPreviousSongInternal();
            }
        });
        mediaSession.setActive(true);
    }

    private PendingIntent getServicePendingIntent(String action, int requestCode) {
        Intent intent = new Intent(this, MusicService.class).setAction(action);
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "GaaneSuno Playback", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    public void initLoudnessEnhancer() {
        if (mediaPlayer != null) {
            loudnessEnhancer = new LoudnessEnhancer(mediaPlayer.getAudioSessionId());
            loudnessEnhancer.setEnabled(true);
        }
    }

    public void setPlayerVolume(float volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume, volume);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMedia();
        if (mediaSession != null) {
            mediaSession.release();
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (pausedByFocusLoss) {
                    if (mediaPlayer == null) {
                        playMedia(currentSong, resumePosition);
                    } else {
                        mediaPlayer.start();
                    }
                    mediaPlayer.setVolume(1.0f, 1.0f);
                    pausedByFocusLoss = false;
                    showNotification(true);
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if (isPlaying()) {
                    resumePosition = getCurrentPosition();
                    stopMedia();
                    pausedByFocusLoss = true;
                    showNotification(false);
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (isPlaying()) {
                    pause();
                    pausedByFocusLoss = true;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.setVolume(0.1f, 0.1f);
                }
                break;
        }
    }

    private boolean requestAudioFocus() {
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonAudioFocus() {
        audioManager.abandonAudioFocus(this);
    }
}
