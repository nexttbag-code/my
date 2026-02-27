package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.database.WorkDBHelper;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class BarChartActivity extends AppCompatActivity {

    private Button btnSelectYear;
    private WorkDBHelper dbHelper;

    private TextView tvTotal;
    private TextView tvTotalLabel;
    private TextView tvAvg;
    private TextView tvDuration;

    private TextView tvStudyHour;
    private TextView tvStudyPercent;

    private TextView tvRestHour;
    private TextView tvRestPercent;

    private TextView tvOtherHour;
    private TextView tvOtherPercent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bar_chart);

        btnSelectYear = findViewById(R.id.btnSelectYear);

        tvTotal = findViewById(R.id.tvTotal);
        tvTotalLabel = findViewById(R.id.tvTotalLabel);
        tvAvg = findViewById(R.id.tvAvg);
        tvDuration = findViewById(R.id.tvDuration);

        tvStudyHour = findViewById(R.id.tvStudyHour);
        tvStudyPercent = findViewById(R.id.tvStudyPercent);

        tvRestHour = findViewById(R.id.tvRestHour);
        tvRestPercent = findViewById(R.id.tvRestPercent);

        tvOtherHour = findViewById(R.id.tvOtherHour);
        tvOtherPercent = findViewById(R.id.tvOtherPercent);

        dbHelper = WorkDBHelper.getInstance(this);

        btnSelectYear.setOnClickListener(v -> showYearSelector());

        tvStudyHour.setOnClickListener(v -> openDetailPage("study"));
        tvRestHour.setOnClickListener(v -> openDetailPage("rest"));
        tvOtherHour.setOnClickListener(v -> openDetailPage("other"));

        tvTotal.setOnClickListener(v -> openAllStudyDetail());

        String currentYear =
                String.valueOf(LocalDate.now().getYear());

        btnSelectYear.setText(currentYear);
        loadYearlySummary(currentYear);
    }

    // ⭐ 新增：返回自动刷新
    @Override
    protected void onResume() {
        super.onResume();
        loadYearlySummary(btnSelectYear.getText().toString());
    }

    private void showYearSelector() {
        List<String> years = dbHelper.getAvailableYears();

        if (years == null || years.isEmpty()) {
            Toast.makeText(this, "暂无数据", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] yearArray = years.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("选择年份")
                .setItems(yearArray, (dialog, which) -> {
                    String selectedYear = yearArray[which];
                    btnSelectYear.setText(selectedYear);
                    loadYearlySummary(selectedYear);
                })
                .show();
    }

    private void openDetailPage(String type) {
        Intent intent =
                new Intent(this, YearDetailActivity.class);

        intent.putExtra("year",
                btnSelectYear.getText().toString());
        intent.putExtra("type", type);

        startActivity(intent);
    }

    private void openAllStudyDetail() {
        Intent intent =
                new Intent(this, YearDetailActivity.class);

        intent.putExtra("year", "ALL");
        intent.putExtra("type", "study");
        intent.putExtra("isAllStudyTotal", true);

        startActivity(intent);
    }

    private void loadYearlySummary(String year) {

        Map<String, Float> data =
                dbHelper.queryYearlySummary(year);

        if (data == null || data.isEmpty()) {
            Toast.makeText(this,
                    year + " 年暂无数据",
                    Toast.LENGTH_SHORT).show();
            clearSummary();
            return;
        }

        float study = 0f;
        float rest = 0f;
        float other = 0f;

        for (Map.Entry<String, Float> entry : data.entrySet()) {

            String label = entry.getKey();
            float minutes = entry.getValue();

            if (label.contains("睡")
                    || label.contains("休")
                    || label.contains("憩")) {
                rest += minutes;

            } else if (label.contains("读")
                    || label.contains("听")
                    || label.contains("看")
                    || label.contains("写")) {
                study += minutes;

            } else {
                other += minutes;
            }
        }

        float studyH = study / 60f;
        float restH = rest / 60f;
        float otherH = other / 60f;
        float totalH = studyH + restH + otherH;

        float allStudyMinutes =
                dbHelper.queryAllStudyMinutes();

        float extraStudyMinutes = 776.5f * 60f;

        float finalStudyHours =
                (allStudyMinutes + extraStudyMinutes) / 60f;

        LocalDate startDate =
                LocalDate.of(2024, 8, 1);

        LocalDate today = LocalDate.now();

        long totalDays =
                ChronoUnit.DAYS.between(startDate, today) + 1;

        float avgPerDay =
                totalDays == 0 ? 0 :
                        finalStudyHours / totalDays;

        int avgHours = (int) avgPerDay;
        int avgMinutes =
                Math.round((avgPerDay - avgHours) * 60);

        float yearsFloat = totalDays / 365f;

        tvTotal.setText(
                String.format("%.1f h", finalStudyHours));

        tvTotalLabel.setText("累计学习总时长");

        tvAvg.setText(
                String.format("%dh %dm / 天",
                        avgHours,
                        avgMinutes));

        tvDuration.setText(
                String.format("%d 天 · %.1f 年",
                        totalDays,
                        yearsFloat));

        float studyPercent =
                totalH == 0 ? 0 : studyH / totalH * 100;

        float restPercent =
                totalH == 0 ? 0 : restH / totalH * 100;

        float otherPercent =
                totalH == 0 ? 0 : otherH / totalH * 100;

        tvStudyHour.setText(
                String.format("%.0f h", studyH));
        tvStudyPercent.setText(
                String.format("%.0f%%", studyPercent));

        tvRestHour.setText(
                String.format("%.0f h", restH));
        tvRestPercent.setText(
                String.format("%.0f%%", restPercent));

        tvOtherHour.setText(
                String.format("%.0f h", otherH));
        tvOtherPercent.setText(
                String.format("%.0f%%", otherPercent));
    }

    private void clearSummary() {

        tvTotal.setText("0 h");
        tvAvg.setText("0h 0m / 天");
        tvDuration.setText("0 天 · 0 年");

        tvStudyHour.setText("0 h");
        tvStudyPercent.setText("0%");

        tvRestHour.setText("0 h");
        tvRestPercent.setText("0%");

        tvOtherHour.setText("0 h");
        tvOtherPercent.setText("0%");
    }
}