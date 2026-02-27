package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.database.WorkDBHelper;
import com.example.myapplication.enity.Work;
import com.example.myapplication.service.PomodoroService;
import com.example.myapplication.util.CalculateUtil;
import com.example.myapplication.util.PomodoroServiceUtils;
import com.example.myapplication.util.ShowTextUtil;
import com.example.myapplication.util.TimeUtil;
import com.example.myapplication.util.ToastUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WorkRecordActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private TextView tvResult;
    private WorkDBHelper mHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_work_record);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvResult = findViewById(R.id.tv_result);
        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvOption = findViewById(R.id.tv_option);
        tvTitle.setText("To Record");
        tvOption.setText("查看记录");

        MonthlySummaryManager.checkAndGenerateSummary(this);
        mHelper = WorkDBHelper.getInstance(this);

        tvOption.setOnClickListener(this);
        findViewById(R.id.iv_back).setOnClickListener(this);
        findViewById(R.id.btn_exercise).setOnClickListener(this);
        findViewById(R.id.btn_read).setOnClickListener(this);
        findViewById(R.id.btn_rest).setOnClickListener(this);
        findViewById(R.id.btn_nap).setOnClickListener(this);
        findViewById(R.id.btn_code).setOnClickListener(this);
        findViewById(R.id.btn_game).setOnClickListener(this);
        findViewById(R.id.btn_recall).setOnClickListener(this);
        findViewById(R.id.btn_listen).setOnClickListener(this);
        findViewById(R.id.btn_move).setOnClickListener(this);
        findViewById(R.id.btn_sleep).setOnClickListener(this);
        findViewById(R.id.btn_spoken).setOnClickListener(this);
        findViewById(R.id.btn_work).setOnClickListener(this);
        findViewById(R.id.btn_chart).setOnClickListener(this);

        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHelper.openReadLink();
        mHelper.openWriteLink();
        calculateToday();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHelper.closeLink();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tv_option) {
            Intent intent = new Intent(this, RecordPagerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return;
        }

        if (id == R.id.iv_back) {
            finish();
            return;
        }

        if (id == R.id.btn_rest) {
            PomodoroServiceUtils.stopServiceIfRunning(this, PomodoroService.class);
            return;
        }

        if (id == R.id.btn_chart) {
            startActivity(new Intent(this, BarChartActivity.class));
            return;
        }

        if (id == R.id.btn_recall) {
            new AlertDialog.Builder(this)
                    .setTitle("确认删除")
                    .setMessage("确定要删除最新一条记录吗？")
                    .setPositiveButton("确认", (dialog, which) -> {
                        if (mHelper.deleteLatestRecord() > 0) {
                            calculateToday();
                        } else {
                            ToastUtil.show(this, "没有数据");
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return;
        }

        if (v instanceof Button) {
            vibrate();
            handleAddWork((Button) v);
            calculateToday();
        }
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("stopPomodoro", false)) {
            PomodoroServiceUtils.stopServiceIfRunning(this, PomodoroService.class);
        }
    }

    private void vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager =
                    (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            Vibrator vibrator = vibratorManager.getDefaultVibrator();
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            }
            return;
        }

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void calculateToday() {
        List<Work> list = mHelper.queryToDay();
        if (list == null || list.isEmpty()) {
            tvResult.setText("暂无记录");
            return;
        }

        long now = System.currentTimeMillis();
        Work latest = list.get(0);
        String latestName = latest.getTitle();
        long latestTimeMillis = parseTime(latest.getTimestamp());
        if (latestTimeMillis < 0) {
            ToastUtil.show(this, "时间格式错误");
            return;
        }

        long durationMinutes = Math.max((now - latestTimeMillis) / (1000 * 60), 0);
        String timeDisplay = formatDuration(durationMinutes, "小时", "分钟");

        SpannableStringBuilder builder = new SpannableStringBuilder();
        String activePrefix = "正在" + latestName;
        builder.append(activePrefix).append("   ").append(timeDisplay).append("\n\n");

        builder.setSpan(new ForegroundColorSpan(Color.parseColor("#FF3B30")),
                0,
                activePrefix.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new StyleSpan(Typeface.BOLD),
                0,
                activePrefix.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        Map<String, Double> map = CalculateUtil.calculateTime(list);
        builder.append(ShowTextUtil.showStyledText(map));

        if (list.size() >= 2) {
            Work earliest = list.get(list.size() - 2);
            long earliestTimeMillis = parseTime(earliest.getTimestamp());
            if (earliestTimeMillis < 0) {
                ToastUtil.show(this, "时间格式错误（最早）");
                return;
            }

            long totalDurationMinutes = Math.max((now - earliestTimeMillis) / (1000 * 60), 0);
            String totalTimeDisplay = totalDurationMinutes >= 60
                    ? (totalDurationMinutes / 60) + " : " + (totalDurationMinutes % 60)
                    : totalDurationMinutes + " 分";

            int start = builder.length();
            builder.append("\n总计      ").append(totalTimeDisplay);
            builder.setSpan(new ForegroundColorSpan(Color.parseColor("#228B22")),
                    start,
                    start + 2,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new StyleSpan(Typeface.BOLD),
                    start,
                    start + 2,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvResult.setText(builder);
    }

    private long parseTime(String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
            Date date = sdf.parse(time);
            return date == null ? -1 : date.getTime();
        } catch (ParseException e) {
            return -1;
        }
    }

    private String formatDuration(long minutes, String hourUnit, String minuteUnit) {
        if (minutes >= 60) {
            return (minutes / 60) + " " + hourUnit + " " + (minutes % 60) + " " + minuteUnit;
        }
        return minutes + " " + minuteUnit;
    }

    private void handleAddWork(Button button) {
        String title = button.getText().toString();
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
}
