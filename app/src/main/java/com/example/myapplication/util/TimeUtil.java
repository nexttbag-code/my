package com.example.myapplication.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtil {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public static String getCurrentTime() {
        return sdf.format(new Date());
    }

    public static double calculateDurationInSeconds(String startTimeStr, String endTimeStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date start = sdf.parse(startTimeStr);
            Date end = sdf.parse(endTimeStr);
            long diffMillis = end.getTime() - start.getTime();
            //return diffMillis / 1000.0; // 返回秒数
            double minutes = diffMillis / (1000.0 * 60.0);
            return Math.round(minutes * 100.0) / 100.0;  // 四舍五入保留两位小数
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
