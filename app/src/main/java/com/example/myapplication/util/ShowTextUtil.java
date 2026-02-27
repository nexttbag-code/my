package com.example.myapplication.util;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import com.example.myapplication.enity.Work;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ShowTextUtil {
    public static SpannableStringBuilder showStyledText(Map<String, Double> map) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        for (Map.Entry<String, Double> entry : map.entrySet()) {
            double minutes = entry.getValue();
            String key = entry.getKey();
            String timeDisplay;
            // 跳过 "睡觉" 条目
            if ("睡觉".equals(key)) continue;

            if (minutes >= 60) {
                int hours = (int) (minutes / 60);
                int remainingMinutes = (int) (minutes % 60);
                timeDisplay = hours + " 时 " + remainingMinutes + " 分";
            } else {
                timeDisplay = ((int) minutes) + " 分";
            }


            String line = key + "       " + timeDisplay + "\n";

            int start = builder.length();
            builder.append(line);

            // 设置 key 部分加粗+颜色
            builder.setSpan(new ForegroundColorSpan(Color.parseColor("#0A84FF")),
                    start, start + key.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            builder.setSpan(new StyleSpan(Typeface.BOLD),
                    start, start + key.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);



        }


        return builder;
    }



}
