package com.example.gaanesuno;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int ZOOM_IN_DELAY = 2500; // Delay before the final zoom-in animation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final View content = findViewById(android.R.id.content);
        ImageView logo = findViewById(R.id.splashLogo);

        TypeWriter splashText = findViewById(R.id.splashText);
        splashText.setCharacterDelay(50);
        splashText.animateText("Welcome to Gaane Suno App");

        // 1. Initial logo zoom-in animation
        Animation logoInitialZoom = AnimationUtils.loadAnimation(this, R.anim.logo_initial_zoom);
        logo.startAnimation(logoInitialZoom);

        // 2. Handler for the final screen zoom-in and transition
        new Handler().postDelayed(() -> {
            Animation zoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
            zoomInAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            content.startAnimation(zoomInAnimation);
        }, ZOOM_IN_DELAY);
    }
}
