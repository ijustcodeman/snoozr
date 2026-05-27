package com.example.alarm;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.Locale;

/**
 * Activity displayed when the alarm goes off.
 * Allows the user to snooze or dismiss the alarm.
 */
public class AlarmActivity extends AppCompatActivity {

    private Button btnSnooze;
    private Button btnDismiss;
    private TextView tvAlarmStatus;
    private static final String CHANNEL_ID = "SnoozeChannel";
    private static final String PREFS_NAME = "AlarmPrefs";
    private static final String KEY_LAST_ALARM = "last_alarm_time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Ensure activity shows over lockscreen and wakes up the screen
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null) {
            keyguardManager.requestDismissKeyguard(this, null);
        }

        setContentView(R.layout.activity_alarm);
        initializeVariables();
    }

    /**
     * Initializes views and listeners.
     */
    private void initializeVariables() {
        btnSnooze = findViewById(R.id.btnSnooze);
        btnDismiss = findViewById(R.id.btnDismiss);
        tvAlarmStatus = findViewById(R.id.tvAlarmStatus);

        btnSnooze.setOnClickListener(v -> snoozeAlarm());
        btnDismiss.setOnClickListener(v -> dismissAlarm());
    }

    /**
     * Snoozes the alarm for a configurable time and updates the shared preference.
     */
    private void snoozeAlarm() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int snoozeMinutes = prefs.getInt("snooze_time", 5);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, snoozeMinutes);

        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }

        String timeText = String.format(Locale.getDefault(), "%02d:%02d", 
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LAST_ALARM, timeText);
        editor.apply();

        showSnoozeNotification(calendar);
        stopAlarmService();
        finish();
    }

    /**
     * Dismisses the alarm, stops the service and clears the alarm time preference.
     */
    private void dismissAlarm() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_LAST_ALARM);
        editor.apply();

        stopAlarmService();
        finish();
    }

    /**
     * Stops the background alarm service (sound).
     */
    private void stopAlarmService() {
        stopService(new Intent(this, AlarmService.class));
    }

    /**
     * Shows a notification indicating the next alarm time.
     */
    private void showSnoozeNotification(Calendar calendar) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Snooze Notifications", NotificationManager.IMPORTANCE_HIGH);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 200, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String timeText = String.format(Locale.getDefault(), "%02d:%02d", 
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Alarm Snoozed")
                .setContentText("Next alarm at " + timeText)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (notificationManager != null) {
            notificationManager.notify(2, builder.build());
        }
    }
}
