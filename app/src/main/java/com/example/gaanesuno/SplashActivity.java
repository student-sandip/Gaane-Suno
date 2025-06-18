package com.example.gaanesuno;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splashLogo);
        TextView welcomeText = findViewById(R.id.splashText);

//        ImageView splashLogo = findViewById(R.id.splashLogo);
//        Animation animation = AnimationUtils.loadAnimation(this, R.anim.logo_pulse);
//        splashLogo.startAnimation(animation);

        ImageView splashLogo = findViewById(R.id.splashLogo);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.logo_fade_slide);
        splashLogo.startAnimation(animation);


        TypeWriter splashText = findViewById(R.id.splashText);
        splashText.setCharacterDelay(60); // optional, delay in ms
        splashText.animateText("Welcome to Gaane Suno App");

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }
}
