package com.example.slime;

import android.content.Context;
import android.content.Intent;
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

public class MainMenuActivity extends AppCompatActivity implements SensorEventListener {

    private TextView tvGreeting;
    private Button btnStart, btnLeaderboard, btnSignIn;
    
    private ImageView bgImageView;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private BackgroundTheme currentTheme = BackgroundTheme.DAY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main_menu);
        
        bgImageView = findViewById(R.id.mainMenuBg);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        tvGreeting = findViewById(R.id.tvGreeting);
        btnStart = findViewById(R.id.btnStart);
        btnLeaderboard = findViewById(R.id.btnLeaderboard);
        btnSignIn = findViewById(R.id.btnSignIn);

        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, GameActivity.class);
            intent.putExtra("BG_THEME", currentTheme.name());
            startActivity(intent);
        });

        btnLeaderboard.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, LeaderboardActivity.class);
            startActivity(intent);
        });

        btnSignIn.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                // Logout
                FirebaseAuth.getInstance().signOut();
                updateUI();
            } else {
                Intent intent = new Intent(MainMenuActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
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

    private void updateUI() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvGreeting.setVisibility(View.VISIBLE);
            tvGreeting.setText("Hello, " + user.getDisplayName() + "!");
            btnSignIn.setText(getString(R.string.logout));
        } else {
            tvGreeting.setVisibility(View.GONE);
            btnSignIn.setText(getString(R.string.sign_in));
        }
    }
}
