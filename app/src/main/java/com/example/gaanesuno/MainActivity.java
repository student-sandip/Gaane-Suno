package com.example.gaanesuno;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 1;
    private static final int PLAYER_ACTIVITY_REQUEST_CODE = 101;

    private ArrayList<Song> songsList;
    private ListView songListView;

    public static int currentlyPlayingPosition = -1;

    private SongAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songListView = findViewById(R.id.songListView);
        songsList = new ArrayList<>();
        loadSongsInBackground();


        if (hasPermission()) {
            loadSongsInBackground();
        } else {
            requestPermission();
        }

        // Launch PlayerActivity on item click
        songListView.setOnItemClickListener((parent, view, pos, id) -> {
            Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
            intent.putExtra("songsList", (Serializable) songsList);
            intent.putExtra("position", pos);
            intent.putExtra("resumePlayback", pos == MusicState.currentlyPlayingPosition);

            MusicState.songList = songsList;
            MusicState.currentlyPlayingPosition = pos;

            adapter.setCurrentlyPlayingPosition(pos);
            adapter.notifyDataSetChanged();

            startActivityForResult(intent, PLAYER_ACTIVITY_REQUEST_CODE);

        });

        // Settings button click
        ImageView settingsButton = findViewById(R.id.btnSettings);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        }
    }

//    @Override
//    public void onBackPressed() {
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        startActivity(intent);
//        finish();
//    }


    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQUEST_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        }
    }

    private void loadSongsInBackground() {
        new Thread(() -> {
            ArrayList<Song> loadedSongs = new ArrayList<>();

            ContentResolver contentResolver = getContentResolver();
            Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

            Cursor cursor = contentResolver.query(songUri, null,
                    MediaStore.Audio.Media.IS_MUSIC + "!= 0", null,
                    MediaStore.Audio.Media.TITLE + " ASC");

            if (cursor != null && cursor.moveToFirst()) {
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

                do {
                    String title = cursor.getString(titleColumn);
                    String artist = cursor.getString(artistColumn);
                    String path = cursor.getString(dataColumn);
                    long albumId = cursor.getLong(albumIdColumn);
                    long duration = cursor.getLong(durationColumn);

                    Uri albumArtUri = Uri.parse("content://media/external/audio/albumart/" + albumId);
                    loadedSongs.add(new Song(title, artist, path, albumArtUri.toString(), duration));
                } while (cursor.moveToNext());

                cursor.close();
            }

            runOnUiThread(() -> {
                songsList = loadedSongs;

                if (songsList.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No songs found on device", Toast.LENGTH_SHORT).show();
                } else {
                    adapter = new SongAdapter(MainActivity.this, songsList);
                    songListView.setAdapter(adapter);
                }
            });
        }).start();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.setCurrentlyPlayingPosition(MusicState.currentlyPlayingPosition);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                loadSongsInBackground();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PLAYER_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            if (data.getBooleanExtra("songDeleted", false)) {
                String deletedPath = data.getStringExtra("deletedSongPath");

                Iterator<Song> iterator = songsList.iterator();
                while (iterator.hasNext()) {
                    Song song = iterator.next();
                    if (song.getPath().equals(deletedPath)) {
                        iterator.remove();
                        break;
                    }
                }

                adapter.notifyDataSetChanged();
                Toast.makeText(this, "Song list updated", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
