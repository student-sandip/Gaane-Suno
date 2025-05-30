package com.example.gaanesuno;

import android.content.Context;
import android.media.MediaMetadataRetriever;

import java.io.IOException;

public class SongUtils {

    public static String getTitleFromPath(Context context, String path) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            return title != null ? title : "Unknown Title";
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown Title";
        } finally {
            retriever.release();
        }
    }

    public static String getArtistFromPath(Context context, String path) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            return artist != null ? artist : "Unknown Artist";
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown Artist";
        } finally {
            retriever.release();
        }
    }
}
