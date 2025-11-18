package com.example.gaanesuno;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 1;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 2;
    private static final int PLAYER_ACTIVITY_REQUEST_CODE = 101;
    private static final int DELETE_REQUEST_CODE = 102;

    private ArrayList<Song> songsList;
    private ArrayList<Song> originalSongsList;
    private ListView songListView;
    private SongAdapter adapter;

    private Toolbar selectionToolbar;
    private TextView tvSelectedCount;
    private ImageButton btnCloseSelection, btnShareSelection, btnDeleteSelection, btnInfoSelection;

    private Set<Song> selectedItems = new HashSet<>();
    private List<Song> songsPendingDeletion;
    private boolean isSelectionMode = false;

    private LinearLayout localSearchBar;
    private EditText etLocalSearch;

    private Animation slideDownAnim;
    private Animation slideUpAnim;


    private final BroadcastReceiver songUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            if ("com.example.gaanesuno.UPDATE_SONG".equals(intent.getAction())) {
                int newPos = intent.getIntExtra("position", -1);
                if (newPos != -1) {
                    MusicState.currentlyPlayingPosition = newPos;
                    if (adapter != null) {
                        adapter.setCurrentlyPlayingPosition(newPos);
                        adapter.notifyDataSetChanged();
                        songListView.invalidateViews();
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isDesktopOrLaptop()) {
            showDesktopWarning();
        }

        View rootView = findViewById(R.id.mainLayout);
        Animation loadAnim = AnimationUtils.loadAnimation(this, R.anim.slide_in_up_fade);
        rootView.startAnimation(loadAnim);

        // Load animations
        slideDownAnim = AnimationUtils.loadAnimation(this, R.anim.no_delay);
        slideUpAnim = AnimationUtils.loadAnimation(this, R.anim.no_delay);

        // Toolbar setup
        selectionToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(selectionToolbar);
        selectionToolbar.setVisibility(View.GONE);

        songListView = findViewById(R.id.songListView);
        songsList = new ArrayList<>();
        originalSongsList = new ArrayList<>();

        // Search bar
        localSearchBar = findViewById(R.id.localSearchBar);
        etLocalSearch = findViewById(R.id.etLocalSearch);

        // Toolbar buttons
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        btnCloseSelection = findViewById(R.id.btnCloseSelection);
        btnShareSelection = findViewById(R.id.btnShareSelection);
        btnDeleteSelection = findViewById(R.id.btnDeleteSelection);
        btnInfoSelection = findViewById(R.id.btnInfoSelection);


        btnCloseSelection.setOnClickListener(v -> exitSelectionMode());

        // SHARE button action
        btnShareSelection.setOnClickListener(v -> {
            if (selectedItems.isEmpty()) {
                Toast.makeText(MainActivity.this, "No song selected!", Toast.LENGTH_SHORT).show();
                return;
            }
            ArrayList<Uri> uris = new ArrayList<>();
            for (Song song : selectedItems) {
                File file = new File(song.getPath());
                if (file.exists()) {
                    Uri uri = FileProvider.getUriForFile(MainActivity.this,
                            getPackageName() + ".provider", file);
                    uris.add(uri);
                }
            }

            if (uris.isEmpty()) {
                Toast.makeText(MainActivity.this, "No valid song file found!", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType("audio/*");
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
            exitSelectionMode();
        });

        // DELETE button action
        btnDeleteSelection.setOnClickListener(v -> {
            if (selectedItems.isEmpty()) {
                Toast.makeText(MainActivity.this, "No song selected!", Toast.LENGTH_SHORT).show();
                return;
            }

            new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle("Delete selected song(s)")
                    .setMessage("Are you sure you want to permanently delete the selected song(s) from your device?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteSelectedSongs())
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnInfoSelection.setOnClickListener(v -> {
            if (selectedItems.size() == 1) {
                Song selectedSong = selectedItems.iterator().next();
                showSongInfoDialog(selectedSong);
            }
        });

        songListView.setOnItemClickListener((parent, view, pos, id) -> {
            if (isSelectionMode) {
                toggleSelection(pos);
            } else {
                openPlayerActivity(pos);
            }
        });

        songListView.setOnItemLongClickListener((parent, view, pos, id) -> {
            if (!isSelectionMode) enterSelectionMode();
            toggleSelection(pos);
            return true;
        });

        ImageView settingsButton = findViewById(R.id.btnSettings);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        ImageButton btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v -> toggleSearchBar());

        if (hasPermission()) {
            requestNotificationPermission();
        } else {
            requestPermission();
        }

        // Status bar black
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        }

        FloatingActionButton fabNowPlaying = findViewById(R.id.fabNowPlaying);
        fabNowPlaying.setOnClickListener(v -> {
            if (MusicState.songList != null && !MusicState.songList.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                intent.putExtra("songsList", (Serializable) MusicState.songList);
                intent.putExtra("position", MusicState.currentlyPlayingPosition);
                intent.putExtra("resumePlayback", true);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No song is currently playing", Toast.LENGTH_SHORT).show();
            }
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_offline);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_offline) {
                return true;
            } else if (id == R.id.nav_online) {
                Intent intent = new Intent(MainActivity.this, OnlineActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_up_fade, R.anim.slide_out_down_fade);
                finish();
                return true;
            } else if (id == R.id.nav_favorite) {
                Intent favIntent = new Intent(this, FavoritesActivity.class);
                startActivity(favIntent);
                overridePendingTransition(R.anim.slide_in_up_fade, R.anim.slide_out_down_fade);
                finish();
                return true;
            }

            return false;
        });

        setupLocalSearch();
    }

    private void deleteSelectedSongs() {
        songsPendingDeletion = new ArrayList<>(selectedItems);
        List<Uri> urisToDelete = new ArrayList<>();
        for (Song song : songsPendingDeletion) {
            urisToDelete.add(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getId()));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
            PendingIntent pendingIntent = MediaStore.createDeleteRequest(getContentResolver(), urisToDelete);
            try {
                startIntentSenderForResult(pendingIntent.getIntentSender(), DELETE_REQUEST_CODE, null, 0, 0, 0, null);
            } catch (IntentSender.SendIntentException e) {
                Toast.makeText(this, "Couldn't perform delete request", Toast.LENGTH_SHORT).show();
            }
        } else { // Android 10 and below
            ContentResolver contentResolver = getContentResolver();
            for (Uri uri : urisToDelete) {
                try {
                    contentResolver.delete(uri, null, null);
                } catch (SecurityException e) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e instanceof RecoverableSecurityException) {
                        RecoverableSecurityException rse = (RecoverableSecurityException) e;
                        PendingIntent pendingIntent = rse.getUserAction().getActionIntent();
                        try {
                            startIntentSenderForResult(pendingIntent.getIntentSender(), DELETE_REQUEST_CODE, null, 0, 0, 0, null);
                            return;
                        } catch (IntentSender.SendIntentException sendEx) {
                            Toast.makeText(this, "Couldn't perform delete request", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Couldn't delete file. Permission denied.", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
            }
            handleSuccessfulDeletion();
        }
    }

    private void handleSuccessfulDeletion() {
        if (songsPendingDeletion == null) return;

        boolean currentlyPlayingDeleted = false;
        int currentlyPlayingPosition = MusicState.currentlyPlayingPosition;
        Song currentlyPlayingSong = null;
        if (currentlyPlayingPosition != -1 && currentlyPlayingPosition < originalSongsList.size()) {
            currentlyPlayingSong = originalSongsList.get(currentlyPlayingPosition);
        }

        for (Song song : songsPendingDeletion) {
            if (song.equals(currentlyPlayingSong)) {
                currentlyPlayingDeleted = true;
                break;
            }
        }

        if (currentlyPlayingDeleted) {
            Intent stopIntent = new Intent(MainActivity.this, MusicService.class);
            stopService(stopIntent);
            MusicState.clearState();
        }

        songsList.removeAll(songsPendingDeletion);
        originalSongsList.removeAll(songsPendingDeletion);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        Toast.makeText(MainActivity.this, songsPendingDeletion.size() + " song(s) deleted.", Toast.LENGTH_SHORT).show();
        exitSelectionMode();
        songsPendingDeletion.clear();
    }

    private boolean isDesktopOrLaptop() {
        PackageManager pm = getPackageManager();
        boolean isChromeOS = pm.hasSystemFeature("org.chromium.arc");
        int screenLayout = getResources().getConfiguration().screenLayout & android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK;
        boolean isLargeScreen = screenLayout >= android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE;
        return isChromeOS || isLargeScreen;
    }

    private void showDesktopWarning() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Desktop Version")
                .setMessage("Local music playback is only available on mobile & tablet devices. You can play online songs on this device.")
                .setPositiveButton("OK", null)
                .show();
    }


    @Override
    public void onBackPressed() {
        if (localSearchBar.getVisibility() == View.VISIBLE) {
            toggleSearchBar();
        } else if (isSelectionMode) {
            exitSelectionMode();
        } else {
            super.onBackPressed();
            finishAffinity();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("com.example.gaanesuno.UPDATE_SONG");
        ContextCompat.registerReceiver(this, songUpdateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        if (adapter != null) {
            adapter.setCurrentlyPlayingPosition(MusicState.currentlyPlayingPosition);
            adapter.notifyDataSetChanged();
        }

        FloatingActionButton fabNowPlaying = findViewById(R.id.fabNowPlaying);
        if (MusicState.currentlyPlayingPosition != -1 && MusicState.songList != null && !MusicState.songList.isEmpty()) {
            fabNowPlaying.show();
        } else {
            fabNowPlaying.hide();
        }

        if (MusicState.currentlyPlayingPosition != -1 && (MusicState.songList == null || MusicState.songList.isEmpty())) {
            MusicState.songList = new ArrayList<>(originalSongsList);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(songUpdateReceiver);
    }

    private void openPlayerActivity(int pos) {
        MusicState.songList = new ArrayList<>(originalSongsList);
        MusicState.currentlyPlayingPosition = pos;

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("songsList", (Serializable) originalSongsList);
        intent.putExtra("position", pos);
        startActivityForResult(intent, PLAYER_ACTIVITY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLAYER_ACTIVITY_REQUEST_CODE) {
            if (adapter != null) {
                adapter.setCurrentlyPlayingPosition(MusicState.currentlyPlayingPosition);
                adapter.notifyDataSetChanged();
            }
        } else if (requestCode == DELETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                handleSuccessfulDeletion();
            } else {
                Toast.makeText(this, "Deletion cancelled or failed.", Toast.LENGTH_SHORT).show();
                exitSelectionMode();
            }
        }
    }


    private void enterSelectionMode() {
        isSelectionMode = true;
        selectedItems.clear();
        selectionToolbar.startAnimation(slideDownAnim);
        selectionToolbar.setVisibility(View.VISIBLE);
        if (adapter != null) {
            adapter.setSelectionMode(true);
            adapter.notifyDataSetChanged();
        }
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();

        if (adapter != null) {
            adapter.setSelectionMode(false);
            adapter.setSelectedItems(new HashSet<>());
            adapter.notifyDataSetChanged();
        }

        selectionToolbar.setVisibility(View.GONE);
    }

    private void toggleSelection(int pos) {
        if (pos < 0 || pos >= songsList.size()) return;

        Song song = songsList.get(pos);
        if (selectedItems.contains(song)) {
            selectedItems.remove(song);
        } else {
            selectedItems.add(song);
            vibrateShort();
        }

        if (selectedItems.isEmpty() && isSelectionMode) {
            exitSelectionMode();
        } else {
            tvSelectedCount.setText(selectedItems.size() + " selected");
            if (selectedItems.size() == 1) {
                btnInfoSelection.setVisibility(View.VISIBLE);
            } else {
                btnInfoSelection.setVisibility(View.GONE);
            }
        }

        if (adapter != null) {
            adapter.setSelectedItems(selectedItems);
            adapter.notifyDataSetChanged();
        }
    }

    private String formatDuration(long duration) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) -
                TimeUnit.MINUTES.toSeconds(minutes);
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void showSongInfoDialog(Song song) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Song Details")
                .setMessage(
                        "Title: " + song.getTitle() + "\n\n" +
                                "Artist: " + song.getArtist() + "\n\n" +
                                "Duration: " + formatDuration(song.getDuration()) + "\n\n" +
                                "Path: " + song.getPath()
                )
                .setPositiveButton("OK", null)
                .show();
    }

    private void vibrateShort() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(40);
            }
        }
    }

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
                    new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                    REQUEST_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            } else {
                loadSongs();
            }
        } else {
            loadSongs();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission();
            } else {
                // Permission Denied
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                    // User has selected "Don't ask again"
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Permission Required")
                            .setMessage("This app needs permission to access your local music files. Please grant the permission in the app settings.")
                            .setPositiveButton("Go to Settings", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                Toast.makeText(this, "Permission Denied! App is closing.", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .setCancelable(false)
                            .show();
                } else {
                    // User has denied permission, but not permanently
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Permission Required")
                            .setMessage("This app needs permission to access your local music files to play music.")
                            .setPositiveButton("Grant", (dialog, which) -> requestPermission())
                            .setNegativeButton("Deny", (dialog, which) -> {
                                Toast.makeText(this, "Permission Denied! App is closing.", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .setCancelable(false)
                            .show();
                }
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with loading songs
                loadSongs();
            } else {
                // Permission denied, you can show a message or disable notification-related features
                Toast.makeText(this, "Notification Permission Denied!", Toast.LENGTH_SHORT).show();
                loadSongs();
            }
        }
    }

    @SuppressLint("Range")
    private void loadSongs() {
        songsList.clear();
        originalSongsList.clear();

        ContentResolver contentResolver = getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC";
        Cursor cursor = contentResolver.query(songUri, null, selection, null, sortOrder);

        if (cursor != null && cursor.moveToFirst()) {
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int pathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);


            do {
                String title = cursor.getString(titleColumn);
                String artist = cursor.getString(artistColumn);
                String path = cursor.getString(pathColumn);
                long duration = cursor.getLong(durationColumn);
                long albumId = cursor.getLong(albumIdColumn);
                long id = cursor.getLong(idColumn);

                Uri albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"), albumId
                );

                songsList.add(new Song(id, title, artist, path, albumArtUri.toString(), duration));
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            if (!isDesktopOrLaptop()) {
                showNoSongsPopup();
            }
        }

        originalSongsList.addAll(songsList);

        adapter = new SongAdapter(this, songsList, selectedItems);
        songListView.setAdapter(adapter);
    }

    private void showNoSongsPopup() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("No Local Songs Found")
                .setMessage("It seems you don\'t have any local songs on your device. Try downloading some music to get started.")
                .setPositiveButton("OK", null)
                .show();
    }


    private void setupLocalSearch() {
        etLocalSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSongs(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void filterSongs(String query) {
        songsList.clear();
        if (query.isEmpty()) {
            songsList.addAll(originalSongsList);
        } else {
            for (Song song : originalSongsList) {
                if (song.getTitle().toLowerCase().contains(query.toLowerCase())) {
                    songsList.add(song);
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void toggleSearchBar() {
        if (localSearchBar.getVisibility() == View.VISIBLE) {
            localSearchBar.startAnimation(slideUpAnim);
            localSearchBar.setVisibility(View.GONE);
            hideKeyboard();
        } else {
            localSearchBar.setVisibility(View.VISIBLE);
            localSearchBar.startAnimation(slideDownAnim);
            etLocalSearch.requestFocus();
            showKeyboard();
        }
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etLocalSearch, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etLocalSearch.getWindowToken(), 0);
        }
    }
}
