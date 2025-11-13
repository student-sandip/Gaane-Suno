package com.example.gaanesuno;

import java.io.Serializable;

public class OnlineSong implements Serializable {
    private String title;
    private String artist;
    private String imageUrl;
    private String previewUrl;
    private int bitrate;

    public OnlineSong(String title, String artist, String imageUrl, String previewUrl) {
        this.title = title;
        this.artist = artist;
        this.imageUrl = imageUrl;
        this.previewUrl = previewUrl;
        this.bitrate = bitrate;
    }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getImageUrl() { return imageUrl; }
    public String getPreviewUrl() { return previewUrl; }
    public int getBitrate() { return bitrate; }

    // Optional: setters if needed later
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
}
