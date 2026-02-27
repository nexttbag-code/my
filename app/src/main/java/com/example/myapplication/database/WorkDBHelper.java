package com.example.myapplication.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.myapplication.MyApplication;
import com.example.myapplication.enity.Work;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class WorkDBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "Work.db";
    private static final String TABLE_NAME = "work_records";
    private static final String SUMMARY_TABLE_NAME = "monthly_summary";
    private static final int DB_VERSION = 15;
    private SQLiteDatabase mRDB = null; // 读取数据库连接
    private SQLiteDatabase mWDB = null; // 写入数据库连接

    private static WorkDBHelper instance;

    public WorkDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static synchronized WorkDBHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new WorkDBHelper(ctx.getApplicationContext());
        }
        return instance;
    }

    public SQLiteDatabase openReadLink() {
        if (mRDB == null || !mRDB.isOpen()) {
            mRDB = getReadableDatabase();
        }
        return mRDB;
    }

    public SQLiteDatabase openWriteLink() {
        if (mWDB == null || !mWDB.isOpen()) {
            mWDB = getWritableDatabase();
        }
        return mWDB;
    }

    public void closeLink() {
        try {
            // 只在数据库连接有效时关闭
            if (mRDB != null && mRDB.isOpen()) {
                mRDB.close();
                mRDB = null;
            }
            if (mWDB != null && mWDB.isOpen()) {
                mWDB.close();
                mWDB = null;
            }
        } catch (Exception e) {
            Log.e("WorkDBHelper", "Error closing database", e);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT," +
                "duration REAL," +
                "timestamp DATETIME)";
        db.execSQL(sql);

        String sql2 = "CREATE TABLE IF NOT EXISTS " + SUMMARY_TABLE_NAME + "(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "month TEXT NOT NULL," +
                "activity TEXT NOT NULL," +
                "duration REAL NOT NULL," +
                "UNIQUE(month, activity))";
        db.execSQL(sql2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {


    }

    // 插入记录
    public long insert(Work work) {
        SQLiteDatabase db = openWriteLink();  // 使用打开的写入数据库连接
        long result = -1;
        try {
            ContentValues values = new ContentValues();
            values.put("title", work.title);
            values.put("duration", work.duration);
            values.put("timestamp", work.timestamp);
            result = db.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
            Log.e("WorkDBHelper", "Error inserting record", e);
        }
        return result;
    }

    // 删除最新记录
    public long deleteLatestRecord() {
        SQLiteDatabase db = openReadLink();
        Cursor cursor = db.rawQuery("SELECT MAX(_id) FROM " + TABLE_NAME, null);
        long result = -1;
        try {
            if (cursor.moveToFirst()) {
                int maxId = cursor.getInt(0);
                result = db.delete(TABLE_NAME, "_id = ?", new String[]{String.valueOf(maxId)});
            }
        } catch (Exception e) {
            Log.e("WorkDBHelper", "Error deleting record", e);
        } finally {
            cursor.close();
        }

        return result;
    }





    @SuppressLint("Range")
    public List<Work> queryByMonth(String yearMonth) {
        ArrayList<Work> list = new ArrayList<>();

        // yearMonth 是 "2025-05" 这样的格式
        String[] parts = yearMonth.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]) - 1; // Calendar月份从0开始

        // 获取本月第一天
        Calendar startCal = Calendar.getInstance();
        startCal.set(year, month, 1);
        String start = String.format(Locale.getDefault(), "%tF", startCal.getTime()); // yyyy-MM-dd

        // 获取下月第一天
        Calendar endCal = Calendar.getInstance();
        endCal.set(year, month + 1, 1);
        String end = String.format(Locale.getDefault(), "%tF", endCal.getTime());

        String sql = "SELECT * FROM work_records WHERE timestamp >= ? AND timestamp < ?";
        SQLiteDatabase db = openReadLink();
        Cursor cursor = db.rawQuery(sql, new String[]{start, end});
        try {
            while (cursor.moveToNext()) {
                Work work = new Work();
                work.id = cursor.getInt(cursor.getColumnIndex("_id"));
                work.title = cursor.getString(cursor.getColumnIndex("title"));
                work.timestamp = cursor.getString(cursor.getColumnIndex("timestamp"));
                work.duration = cursor.getDouble(cursor.getColumnIndex("duration"));
                list.add(work);
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    public Map<String, Float> queryYearlySummary(String year) {
        Map<String, Float> result = new LinkedHashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // 提取年份（例如 "2025"）
        String sql = "SELECT title, SUM(duration) AS total " +
                "FROM work_records " +
                "WHERE strftime('%Y', timestamp) = ? " +
                "GROUP BY title " +
                "ORDER BY total DESC";

        Cursor cursor = db.rawQuery(sql, new String[]{year});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                float total = (float) cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
                result.put(title, total);
            }
            cursor.close();
        }
        return result;
    }
    public List<String> getAvailableYears() {
        List<String> years = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT DISTINCT strftime('%Y', timestamp) AS year FROM work_records ORDER BY year DESC";
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                years.add(cursor.getString(cursor.getColumnIndexOrThrow("year")));
            }
            cursor.close();
        }
        return years;
    }





    // 查询当天的记录
    public List<Work> queryToDay() {
        List<Work> list = new ArrayList<>();
        SQLiteDatabase db = openReadLink();  // 使用打开的读取数据库连接
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY _id DESC", null);
        try {
            if (cursor.moveToFirst()) {
                int titleIndex = cursor.getColumnIndexOrThrow("title");
                int durationIndex = cursor.getColumnIndexOrThrow("duration");
                int timestampIndex = cursor.getColumnIndexOrThrow("timestamp");

                do {
                    String title = cursor.getString(titleIndex);
                    double duration = cursor.getDouble(durationIndex);
                    String timestamp = cursor.getString(timestampIndex);

                    Work work = new Work(title, duration, timestamp);
                    list.add(work);

                    if ("睡觉".equals(title)) {
                        break;  // 找到"睡觉"记录，停止查询
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();  // 确保在查询结束后关闭游标
        }
        return list;
    }

    public float queryAllStudyMinutes() {
        float total = 0f;
        SQLiteDatabase db = getReadableDatabase();

        String sql =
                "SELECT title, SUM(duration) AS total " +
                        "FROM work_records " +
                        "GROUP BY title";

        Cursor cursor = db.rawQuery(sql, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String title =
                        cursor.getString(cursor.getColumnIndexOrThrow("title"));
                float minutes =
                        (float) cursor.getDouble(cursor.getColumnIndexOrThrow("total"));

                if (title.contains("读")
                        || title.contains("听")
                        || title.contains("看")
                        || title.contains("写")) {
                    total += minutes;
                }
            }
            cursor.close();
        }
        return total;
    }
    public Map<String, Float> queryAllSummary() {

        Map<String, Float> result = new LinkedHashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String sql =
                "SELECT title, SUM(duration) AS total " +
                        "FROM work_records " +
                        "GROUP BY title " +
                        "ORDER BY total DESC";

        Cursor cursor = db.rawQuery(sql, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String title = cursor.getString(
                        cursor.getColumnIndexOrThrow("title"));
                float total = (float) cursor.getDouble(
                        cursor.getColumnIndexOrThrow("total"));
                result.put(title, total);
            }
            cursor.close();
        }

        return result;
    }

}
