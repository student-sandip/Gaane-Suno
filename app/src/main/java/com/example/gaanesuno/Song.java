package com.example.gaanesuno;

import java.io.Serializable;
import java.util.Objects;

public class Song implements Serializable {
    private final long id;
    private final String title;
    private final String artist;
    private final String path;
    private final String albumArt;
    private final long duration;

    public Song(long id, String title, String artist, String path, String albumArt, long duration) {
        this.id = id;
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
        return id;
    }

    public String getAlbumArt() {
        return albumArt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Song song = (Song) o;
        return id == song.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
