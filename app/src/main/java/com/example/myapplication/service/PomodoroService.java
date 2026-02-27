package com.example.myapplication.service;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.*;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.myapplication.R;
import com.example.myapplication.WorkRecordActivity;
import com.example.myapplication.database.WorkDBHelper;
import com.example.myapplication.enity.Work;
import com.example.myapplication.util.TimeUtil;

import java.util.Locale;

public class PomodoroService extends Service {

    private static final String CHANNEL_ID = "pomodoro_channel";
    private static final int NOTIFICATION_ID = 100;

    private WorkDBHelper mHelper;
    private MediaPlayer mediaPlayer;

    private AlarmManager alarmManager;
    private PendingIntent alarmPendingIntent;

    private NotificationManager notificationManager;

    private boolean isWorkSession = true;
    private boolean isRunning = false;

    private long workDuration = 25 * 60 * 1000;
    private long restDuration = 5 * 60 * 1000;

    private long sessionEndTime;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;

    @Override
    public void onCreate() {
        super.onCreate();

        mHelper = WorkDBHelper.getInstance(this);
        mHelper.openWriteLink();

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.getBooleanExtra("stopPomodoro", false)) {
            stopPomodoroLoop();
            return START_NOT_STICKY;
        }

        if (intent != null && intent.getBooleanExtra("alarmTriggered", false)) {
            handleAlarmTrigger();
            return START_STICKY;
        }

        if (!isRunning) {
            startPomodoroLoop();
        }

        return START_STICKY;
    }

    private void startPomodoroLoop() {

        isRunning = true;
        isWorkSession = true;

        startSession(workDuration);
    }

    private void startSession(long duration) {

        sessionEndTime = System.currentTimeMillis() + duration;

        scheduleExactAlarm(duration);

        startNotificationUpdater();

        showNotification(
                isWorkSession ? "番茄钟：学习中" : "番茄钟：休息中",
                formatTime(duration)
        );
    }

    private void handleAlarmTrigger() {

        playSound();

        if (!isRunning) return;

        if (isWorkSession) {
            addWorkRecord("休息");
            isWorkSession = false;
            startSession(restDuration);
        } else {
            addWorkRecord("阅读");
            isWorkSession = true;
            startSession(workDuration);
        }
    }

    private void scheduleExactAlarm(long duration) {

        long triggerTime = System.currentTimeMillis() + duration;

        Intent intent = new Intent(this, PomodoroAlarmReceiver.class);

        alarmPendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (!alarmManager.canScheduleExactAlarms()) {

                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        alarmPendingIntent
                );
                return;
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                alarmPendingIntent
        );
    }

    private void startNotificationUpdater() {

        updateRunnable = new Runnable() {
            @Override
            public void run() {

                if (!isRunning) return;

                long remaining = sessionEndTime - System.currentTimeMillis();

                if (remaining > 0) {

                    showNotification(
                            isWorkSession ? "番茄钟：学习中" : "番茄钟：休息中",
                            formatTime(remaining)
                    );

                    handler.postDelayed(this, 1000);
                }
            }
        };

        handler.post(updateRunnable);
    }

    private void stopPomodoroLoop() {

        if (alarmManager != null && alarmPendingIntent != null) {
            alarmManager.cancel(alarmPendingIntent);
        }

        handler.removeCallbacks(updateRunnable);

        isRunning = false;

        stopForeground(true);

        Intent intent = new Intent(this, WorkRecordActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        stopSelf();
    }

    private void addWorkRecord(String title) {

        String currentTime = TimeUtil.getCurrentTime();
        SQLiteDatabase db = mHelper.openWriteLink();

        Cursor cursor = db.rawQuery(
                "SELECT _id, (julianday(?) - julianday(timestamp)) * 24 * 60 AS diff " +
                        "FROM work_records ORDER BY _id DESC LIMIT 1",
                new String[]{currentTime}
        );

        if (cursor.moveToFirst()) {
            int prevId = cursor.getInt(0);
            double raw = cursor.getDouble(1);
            double durationMinutes = Math.round(raw * 10.0) / 10.0;

            ContentValues updateValues = new ContentValues();
            updateValues.put("duration", Math.max(durationMinutes, 0));
            db.update("work_records", updateValues, "_id = ?", new String[]{String.valueOf(prevId)});
        }

        cursor.close();
        mHelper.insert(new Work(title, 0.0, currentTime));
    }

    private void playSound() {

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.beep);

        if (mediaPlayer != null) {
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
            );
            mediaPlayer.start();
        }
    }

    private String formatTime(long millis) {
        long min = millis / 1000 / 60;
        long sec = (millis / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec);
    }

    private void showNotification(String title, String text) {

        Intent stopIntent = new Intent(this, PomodoroService.class);
        stopIntent.putExtra("stopPomodoro", true);

        PendingIntent pendingIntent = PendingIntent.getService(
                this,
                0,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "番茄钟通知",
                    NotificationManager.IMPORTANCE_LOW
            );

            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {

        handler.removeCallbacks(updateRunnable);

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        if (alarmManager != null && alarmPendingIntent != null) {
            alarmManager.cancel(alarmPendingIntent);
        }

        isRunning = false;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
