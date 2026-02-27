package com.example.myapplication.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

public class PomodoroServiceUtils {

    // 判断服务是否在运行
    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    // 返回 true 表示服务正在运行并已停止，false 表示服务没运行
    public static boolean stopServiceIfRunning(Context context, Class<?> serviceClass) {
        if (isServiceRunning(context, serviceClass)) {
            Intent intent = new Intent(context, serviceClass);
            context.stopService(intent);
            return true;
        }
        return false;
    }
}

