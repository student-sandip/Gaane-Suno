package com.example.gaanesuno;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class FavoritesManager {

    private static final String PREF_NAME = "favorites_data";
    private static final String KEY_FAVORITES = "favorites_list";

    public static void addFavorite(Context context, OnlineSong song) {
        if (song == null) return;
        ArrayList<OnlineSong> favorites = getFavorites(context);

        // avoid duplicates
        for (OnlineSong s : favorites) {
            if (s.getPreviewUrl().equals(song.getPreviewUrl())) {
                return;
            }
        }

        favorites.add(song);
        saveFavorites(context, favorites);
    }

    public static void removeFavorite(Context context, String songUrl) {
        ArrayList<OnlineSong> favorites = getFavorites(context);
        for (int i = 0; i < favorites.size(); i++) {
            if (favorites.get(i).getPreviewUrl().equals(songUrl)) {
                favorites.remove(i);
                break;
            }
        }
        saveFavorites(context, favorites);
    }

    public static ArrayList<OnlineSong> getFavorites(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_FAVORITES, "[]");

        ArrayList<OnlineSong> favorites = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                favorites.add(new OnlineSong(
                        obj.getString("title"),
                        obj.getString("artist"),
                        obj.getString("imageUrl"),
                        obj.getString("previewUrl")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return favorites;
    }

    private static void saveFavorites(Context context, ArrayList<OnlineSong> favorites) {
        JSONArray array = new JSONArray();
        try {
            for (OnlineSong s : favorites) {
                JSONObject obj = new JSONObject();
                obj.put("title", s.getTitle());
                obj.put("artist", s.getArtist());
                obj.put("imageUrl", s.getImageUrl());
                obj.put("previewUrl", s.getPreviewUrl());
                array.put(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_FAVORITES, array.toString()).apply();
    }

    // ✅ এইটা হলো missing method (isFavorite)
    public static boolean isFavorite(Context context, OnlineSong song) {
        if (song == null || song.getPreviewUrl() == null) return false;
        ArrayList<OnlineSong> favorites = getFavorites(context);
        for (OnlineSong s : favorites) {
            if (s.getPreviewUrl().equals(song.getPreviewUrl())) {
                return true;
            }
        }
        return false;
    }
}
