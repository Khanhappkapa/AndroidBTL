package com.example.slime;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slime.entities.BackgroundTheme;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class GameOverActivity extends AppCompatActivity implements SensorEventListener {

    private TextView tvCurrentScore, tvBestScore, tvSignInPrompt;
    private Button btnPlayAgain, btnMenu;
    private ImageView bgImageView;
    
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private BackgroundTheme currentTheme = BackgroundTheme.DAY;

    private static final String PREFS = "slime_prefs";

    private String getHighScoreKey() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return "hi_score_" + user.getUid();
        }
        return "hi_score_guest";
    }

    private int currentScore;
    private int hiScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_game_over);
        
        bgImageView = findViewById(R.id.gameOverBg);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        tvCurrentScore = findViewById(R.id.tvCurrentScore);
        tvBestScore = findViewById(R.id.tvBestScore);
        tvSignInPrompt = findViewById(R.id.tvSignInPrompt);
        btnPlayAgain = findViewById(R.id.btnPlayAgain);
        btnMenu = findViewById(R.id.btnMenu);

        currentScore = getIntent().getIntExtra("SCORE", 0);

        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String hiKey = getHighScoreKey();
        hiScore = prefs.getInt(hiKey, 0);

        if (currentScore > hiScore) {
            hiScore = currentScore;
            prefs.edit().putInt(hiKey, hiScore).apply();
            tvBestScore.setText("✨ New High Score: " + hiScore + " ! ✨");
        } else {
            tvBestScore.setText(String.format(getString(R.string.best_score), hiScore));
        }

        tvCurrentScore.setText(String.format(getString(R.string.current_score), currentScore));

        btnPlayAgain.setOnClickListener(v -> {
            Intent intent = new Intent(GameOverActivity.this, GameActivity.class);
            intent.putExtra("BG_THEME", currentTheme.name());
            startActivity(intent);
            finish();
        });

        btnMenu.setOnClickListener(v -> {
            startActivity(new Intent(GameOverActivity.this, MainMenuActivity.class));
            finish();
        });

        tvSignInPrompt.setOnClickListener(v -> {
            startActivity(new Intent(GameOverActivity.this, LoginActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkFirebaseSync();
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            BackgroundTheme newTheme = (lux > 40) ? BackgroundTheme.DAY : BackgroundTheme.NIGHT;
            if (newTheme != currentTheme) {
                currentTheme = newTheme;
                bgImageView.setImageResource(currentTheme == BackgroundTheme.DAY ? R.drawable.cloudsday : R.drawable.cloudsnight);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void checkFirebaseSync() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvSignInPrompt.setVisibility(View.GONE);
            syncScoreToFirestore(user, hiScore);
        } else {
            tvSignInPrompt.setVisibility(View.VISIBLE);
        }
    }

    private void syncScoreToFirestore(FirebaseUser user, int newScore) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("leaderboard").document(user.getUid());

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Long cloudScoreObj = documentSnapshot.getLong("score");
                long cloudScore = cloudScoreObj != null ? cloudScoreObj : 0;
                
                if (newScore > cloudScore) {
                    docRef.update("score", newScore, "lastUpdated", System.currentTimeMillis());
                }
            } else {
                // Should exist but just in case
                docRef.update("score", newScore, "lastUpdated", System.currentTimeMillis());
            }
        });
    }
}
