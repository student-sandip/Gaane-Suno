package com.example.gaanesuno;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageView backButton = findViewById(R.id.settings_back_btn);
        backButton.setOnClickListener(v -> finish());

        findViewById(R.id.settings_remove_ads).setOnClickListener(v -> {
            Toast.makeText(this, "this feature will come soon", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.settings_customer_support).setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:support@gaanesuno.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Support Needed");
            startActivity(Intent.createChooser(emailIntent, "Contact Support"));
        });

        findViewById(R.id.settings_rate_us).setOnClickListener(v -> {
            final String appPackageName = getPackageName();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }
        });

        findViewById(R.id.settings_share).setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out GaaneSuno music player: https://play.google.com/store/apps/details?id=" + getPackageName());
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        findViewById(R.id.settings_privacy_policy).setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com/privacy")));
        });

        findViewById(R.id.settings_terms_conditions).setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com/terms")));
        });

        findViewById(R.id.settings_bug_fix).setOnClickListener(v -> {
            Intent bugIntent = new Intent(Intent.ACTION_SENDTO);
            bugIntent.setData(Uri.parse("mailto:support@gaanesuno.com"));
            bugIntent.putExtra(Intent.EXTRA_SUBJECT, "Bug Report");
            startActivity(Intent.createChooser(bugIntent, "Report Bug"));
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.orange)); // or your desired color
        }

    }
}
