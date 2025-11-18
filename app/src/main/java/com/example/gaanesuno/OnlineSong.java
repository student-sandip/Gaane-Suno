package com.example.gaanesuno;

import java.io.Serializable;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class OnlineSong implements Serializable {
    private long trackId;
    private String title;
    private String artist;
    private String imageUrl;
    private String previewUrl;
    private int bitrate;
    private long duration;

    public OnlineSong(long trackId, String title, String artist, String imageUrl, String previewUrl, long duration) {
        this.trackId = trackId;
        this.title = title;
        this.artist = artist;
        this.imageUrl = imageUrl;
        this.previewUrl = previewUrl;
        this.bitrate = 128; // Default value
        this.duration = duration;
    }

    public OnlineSong(long trackId, String title, String artist, String imageUrl, String previewUrl) {
        this(trackId, title, artist, imageUrl, previewUrl, 0);
    }

    public long getTrackId() { return trackId; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getImageUrl() { return imageUrl; }
    public String getPreviewUrl() { return previewUrl; }
    public int getBitrate() { return bitrate; }
    public long getDurationMillis() { return duration; }

    // Optional: setters if needed later
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }

    public String getDuration() {
        if (duration <= 0) {
            return "N/A";
        }
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}
