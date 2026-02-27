package com.example.myapplication.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PomodoroAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent serviceIntent = new Intent(context, PomodoroService.class);
        serviceIntent.putExtra("alarmTriggered", true);
        context.startForegroundService(serviceIntent);
    }
}
