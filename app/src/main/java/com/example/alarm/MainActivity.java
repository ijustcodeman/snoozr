package com.example.alarm;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.Locale;

/**
 * Main Activity for setting and canceling the alarm.
 */
public class MainActivity extends AppCompatActivity {

    private TimePicker timePicker;
    private Button btnSetAlarm;
    private Button btnCancelAlarm;
    private Button btnSettings;
    private TextView tvSetAlarmTime;
    private AlarmManager alarmManager;
    private Toast currentToast;

    private static final String PREFS_NAME = "AlarmPrefs";
    private static final String KEY_LAST_ALARM = "last_alarm_time";
    private static final int REQUEST_CODE_NOTIFICATIONS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeVariables();
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAlarmDisplay();
    }

    /**
     * Initializes views and listeners.
     */
    private void initializeVariables() {
        timePicker = findViewById(R.id.timePicker);
        btnSetAlarm = findViewById(R.id.btnSetAlarm);
        btnCancelAlarm = findViewById(R.id.btnCancelAlarm);
        btnSettings = findViewById(R.id.btnSettings);
        tvSetAlarmTime = findViewById(R.id.tvSetAlarmTime);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Set TimePicker to 24-hour format (EU standard)
        timePicker.setIs24HourView(true);

        btnSetAlarm.setOnClickListener(v -> setAlarm());
        btnCancelAlarm.setOnClickListener(v -> cancelAlarm());
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Checks and requests necessary permissions.
     */
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_NOTIFICATIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showToast("Notification permission is required for the alarm to work properly.");
            }
        }
    }

    /**
     * Updates the display of the currently set alarm time from preferences.
     */
    private void updateAlarmDisplay() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lastAlarm = prefs.getString(KEY_LAST_ALARM, null);
        if (lastAlarm != null) {
            tvSetAlarmTime.setText(getString(R.string.alarm_set_for, lastAlarm));
        } else {
            tvSetAlarmTime.setText(R.string.no_alarm_set);
        }
    }

    /**
     * Sets the alarm based on the TimePicker selection.
     */
    private void setAlarm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                return;
            }
        }

        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1);
        }

        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }

        String timeText = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(KEY_LAST_ALARM, timeText);
        editor.apply();

        updateAlarmDisplay();
        showToast(getString(R.string.alarm_set_for, timeText));
    }

    /**
     * Cancels any pending alarm.
     */
    private void cancelAlarm() {
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
        
        stopService(new Intent(this, AlarmService.class));
        
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.remove(KEY_LAST_ALARM);
        editor.apply();

        updateAlarmDisplay();
        showToast(getString(R.string.alarm_canceled));
    }

    /**
     * Shows a toast message and cancels any previous toast to prevent spam.
     */
    private void showToast(String message) {
        if (currentToast != null) {
            currentToast.cancel();
        }
        currentToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        currentToast.show();
    }
}
