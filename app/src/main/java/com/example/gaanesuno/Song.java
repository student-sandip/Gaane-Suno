package com.example.gaanesuno;

import java.io.Serializable;

public class Song implements Serializable {
    private String title;
    private String artist;
    private String path;
    private String albumArt;
    private long duration;

    public Song(String title, String artist, String path, String albumArt, long duration) {
        this.title = title;
        this.artist = artist;
        this.path = path;
        this.albumArt = albumArt;
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getPath() {
        return path;
    }

    public String getAlbumArtUri() {
        return albumArt;
    }

    public long getDuration() {
        return duration;
    }

    public long getId() {
        return 0; // Optional, change if you store ID
    }
}
