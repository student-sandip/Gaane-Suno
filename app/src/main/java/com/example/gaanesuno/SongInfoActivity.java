package com.example.gaanesuno;

import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.gaanesuno.R;
import com.example.gaanesuno.Song;

import java.io.File;
import java.io.IOException;

public class SongInfoActivity extends AppCompatActivity {

    TextView infoTitle, infoArtist, infoAlbum, infoDuration, infoPath;
    ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_info);

        Song song = (Song) getIntent().getSerializableExtra("song");

        if (song == null) {
            Toast.makeText(this, "No song data found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backButton = findViewById(R.id.backButton);
        infoTitle = findViewById(R.id.infoTitle);
        infoArtist = findViewById(R.id.infoArtist);
        infoAlbum = findViewById(R.id.infoAlbum);
        infoDuration = findViewById(R.id.infoDuration);
        infoPath = findViewById(R.id.infoPath);

        if (song != null) {
            infoTitle.setText(song.getTitle());
            infoArtist.setText(song.getArtist());
            infoPath.setText(song.getPath());

            File file = new File(song.getPath());
            if (file.exists()) {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(song.getPath());

                String album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

                long duration = 0;
                try {
                    duration = Long.parseLong(durationStr);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String formattedDuration = formatDuration(duration);
                infoAlbum.setText(album != null ? album : "Unknown Album");
                infoDuration.setText(formattedDuration);

                try {
                    mmr.release();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else {
            Toast.makeText(this, "No song data found", Toast.LENGTH_SHORT).show();
            finish();
        }

        backButton.setOnClickListener(v -> onBackPressed());


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.orange_accent));
        }
    }

    private String formatDuration(long durationMillis) {
        long minutes = durationMillis / 1000 / 60;
        long seconds = (durationMillis / 1000) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
