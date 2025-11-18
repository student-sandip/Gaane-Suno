package com.example.gaanesuno;

import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class OnlineSongInfoActivity extends AppCompatActivity {

    TextView infoTitle, infoArtist, infoAlbum, infoDuration, infoPath;
    ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_info); // same XML reuse

        backButton = findViewById(R.id.backButton);
        infoTitle = findViewById(R.id.infoTitle);
        infoArtist = findViewById(R.id.infoArtist);
        infoAlbum = findViewById(R.id.infoAlbum);
        infoDuration = findViewById(R.id.infoDuration);
        infoPath = findViewById(R.id.infoPath);

        // Get data from Intent
        String title = getIntent().getStringExtra("title");
        String artist = getIntent().getStringExtra("artist");
        String album = getIntent().getStringExtra("album");
        String duration = getIntent().getStringExtra("duration");
        String url = getIntent().getStringExtra("url");

        if (title == null || url == null) {
            Toast.makeText(this, "No song data found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set info to views
        infoTitle.setText(title);
        infoArtist.setText(artist != null ? artist : "Unknown Artist");
        infoAlbum.setText(album != null ? album : "Unknown Album");
        infoDuration.setText(duration != null ? duration : "N/A");
        infoPath.setText(url);

        // Back button
        backButton.setOnClickListener(v -> onBackPressed());

        // Status bar color same as offline info
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.orange_accent));
        }
    }
}
