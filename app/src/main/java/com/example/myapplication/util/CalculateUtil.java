package com.example.myapplication.util;

import android.util.Log;

import com.example.myapplication.enity.Work;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateUtil {

    public static Map<String, Double> calculateTime(List<Work> list){
        Map<String, Double> mergedMap = new HashMap<>();
        // 遍历 workList，按标题合并时长
        for (Work work : list) {
            // 获取当前工作项的标题
            String title = work.title;

            // 如果标题已存在于 Map 中，累加时长；如果没有，直接添加
            if (mergedMap.containsKey(title)) {
                mergedMap.put(title, mergedMap.get(title) + work.duration);
            } else {
                mergedMap.put(title, work.duration);
            }
        }

        return mergedMap;
    }
    public static String format(double value) {
        if (value == 0) {
            return "0小时";
        }
        if (value < 60) {
            return (int) value + "分钟";
        }
        double hours = value / 60.0;
        return String.format("%.1f小时", hours);
    }



}
