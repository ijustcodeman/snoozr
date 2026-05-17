package com.example.alarm;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Settings Activity to configure the snooze duration.
 */
public class SettingsActivity extends AppCompatActivity {

    private NumberPicker npSnooze;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initializeVariables();
    }

    /**
     * Initializes views and sets up the NumberPicker.
     */
    private void initializeVariables() {
        npSnooze = findViewById(R.id.npSnooze);
        btnSave = findViewById(R.id.btnSaveSettings);

        // Configure NumberPicker
        npSnooze.setMinValue(1);
        npSnooze.setMaxValue(60);

        // Load current value
        SharedPreferences prefs = getSharedPreferences("AlarmPrefs", MODE_PRIVATE);
        int currentSnooze = prefs.getInt("snooze_time", 5);
        npSnooze.setValue(currentSnooze);

        btnSave.setOnClickListener(v -> saveSettings());
    }

    /**
     * Saves the snooze duration to SharedPreferences.
     */
    private void saveSettings() {
        int snoozeTime = npSnooze.getValue();
        SharedPreferences prefs = getSharedPreferences("AlarmPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("snooze_time", snoozeTime);
        editor.apply();

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
