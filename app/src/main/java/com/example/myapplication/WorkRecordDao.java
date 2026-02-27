package com.example.myapplication;

import static com.example.myapplication.MyApplication.getContext;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.database.WorkDBHelper;
import com.example.myapplication.enity.Work;
import com.example.myapplication.util.CalculateUtil;

import java.util.List;
import java.util.Map;

public class WorkRecordDao implements AutoCloseable{
    private final WorkDBHelper helper;
    private final SQLiteDatabase db;

    public WorkRecordDao(Context context) {
        helper = WorkDBHelper.getInstance(getContext());

        db = helper.openReadLink();
    }

    public Map<String, Double> getTotalDurationGroupedByActivity(String month) {
        List<Work> list = helper.queryByMonth(month);
        Map<String, Double> result = CalculateUtil.calculateTime(list);

        return result;
    }

    @Override
    public void close() {
        // 关闭资源，释放数据库连接等
        if (db != null) {
            db.close();
        }
    }
    }


