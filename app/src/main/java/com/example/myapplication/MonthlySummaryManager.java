package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.myapplication.database.WorkDBHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class MonthlySummaryManager {
    private static final String PREF_NAME = "monthly_summary_pref";
    private static final String LAST_SUMMARY_MONTH = "last_summary_month";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

    public static void checkAndGenerateSummary(Context context) {
        String currentMonth = getCurrentMonth(); // 比如 2025-05
        String lastMonth = getLastMonth();       // 比如 2025-04

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String lastSummarized = prefs.getString(LAST_SUMMARY_MONTH, "");

        // 如果今天是1号 且没汇总过这个月
        if (isFirstDay() && !lastSummarized.equals(currentMonth)) {
            // 执行汇总逻辑
            generateSummary(context, lastMonth);

            // 更新标记
            prefs.edit().putString(LAST_SUMMARY_MONTH, currentMonth).apply();
        }
    }

    private static boolean isFirstDay() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.DAY_OF_MONTH) == 1;
    }

    private static String getCurrentMonth() {
        return sdf.format(new Date());
    }

    private static String getLastMonth() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        return sdf.format(cal.getTime());
    }

    private static void generateSummary(Context context, String monthToSummarize) {
        try (WorkRecordDao workDao = new WorkRecordDao(context);
             SummaryDao summaryDao = new SummaryDao(context)) {
            Map<String, Double> result = workDao.getTotalDurationGroupedByActivity(monthToSummarize);

            for (Map.Entry<String, Double> entry : result.entrySet()) {
                String activity = entry.getKey();
                Double totalDuration = entry.getValue();
                summaryDao.insertOrUpdateSummary(monthToSummarize, activity, totalDuration);
            }
        } catch (Exception e) {
            Log.e("MonthlySummaryManager", "Error during summary generation", e);
        }

        // 🔔 调用通知
        showSummaryNotification(context, monthToSummarize);
    }

    private static void showSummaryNotification(Context context, String month) {
        String channelId = "summary_channel_id";
        String channelName = "月度汇总通知";

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle("月度汇总完成")
                .setContentText("已生成 " + month + " 的活动汇总")
                .setAutoCancel(true)
                .build();

        int notificationId = month.hashCode(); // 使用月份字符串的哈希值作为通知 ID
        notificationManager.notify(notificationId, notification);
    }
}

