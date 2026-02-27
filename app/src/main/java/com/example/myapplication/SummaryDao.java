package com.example.myapplication;

import static com.example.myapplication.MyApplication.getContext;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.database.WorkDBHelper;

public class SummaryDao implements AutoCloseable{
    private final WorkDBHelper helper;
    private final SQLiteDatabase db;
    private static final String SUMMARY_TABLE_NAME = "monthly_summary";

    public SummaryDao(Context context) {
        helper = WorkDBHelper.getInstance(getContext());
        db = helper.openWriteLink();
    }

    public void insertOrUpdateSummary(String month, String activity, double duration) {
        // 将分钟转换为小时并保留两位小数
        double hours = duration / 60.0;  // 转换为小时
        double roundedHours = Math.round(hours * 100.0) / 100.0; // 保留两位小数

        // 查询当前是否存在该月和该活动类型的记录
        String query = "SELECT _id FROM " + SUMMARY_TABLE_NAME + " WHERE month = ? AND activity = ?";
        Cursor cursor = db.rawQuery(query, new String[]{month, activity});

        if (cursor != null && cursor.moveToFirst()) {
            // 已存在：更新
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            ContentValues values = new ContentValues();
            values.put("duration", roundedHours);  // 更新为小时数

            db.update(SUMMARY_TABLE_NAME, values, "_id = ?", new String[]{String.valueOf(id)});
            cursor.close();
        } else {
            // 不存在：插入
            ContentValues values = new ContentValues();
            values.put("month", month);
            values.put("activity", activity);
            values.put("duration", roundedHours);  // 插入小时数

            db.insert(SUMMARY_TABLE_NAME, null, values);
        }
        cursor.close();
    }


    @Override
    public void close() {
        // 关闭资源，释放数据库连接等
        if (db != null) {
            db.close();
        }
    }
}
