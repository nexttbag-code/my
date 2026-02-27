package com.example.myapplication;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adpater.YearDetailAdapter;
import com.example.myapplication.database.WorkDBHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YearDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvTotal;
    private RecyclerView rvDetail;
    private WorkDBHelper dbHelper;

    // 你的历史补录数据
    private static final float EXTRA_LISTEN = 385.9f;
    private static final float EXTRA_READ   = 390.6f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_year_detail);

        tvTitle = findViewById(R.id.tvTitle);
        tvTotal = findViewById(R.id.tvTotal);
        rvDetail = findViewById(R.id.rvDetail);

        dbHelper = WorkDBHelper.getInstance(this);

        String year = getIntent().getStringExtra("year");
        String type = getIntent().getStringExtra("type");
        boolean isAllStudyTotal =
                getIntent().getBooleanExtra("isAllStudyTotal", false);

        if ("ALL".equals(year)) {
            tvTitle.setText("全部年份 · " + getTypeName(type));
        } else {
            tvTitle.setText(year + " 年 · " + getTypeName(type));
        }

        loadDetail(year, type, isAllStudyTotal);
    }

    private String getTypeName(String type) {
        switch (type) {
            case "study": return "学习类";
            case "rest":  return "休息 / 睡眠";
            default:      return "其他";
        }
    }

    private void loadDetail(String year, String type, boolean isAllStudyTotal) {

        Map<String, Float> allData;

        if ("ALL".equals(year)) {
            allData = dbHelper.queryAllSummary();
        } else {
            allData = dbHelper.queryYearlySummary(year);
        }

        if (allData == null || allData.isEmpty()) {
            tvTotal.setText("暂无数据");
            return;
        }

        List<Map.Entry<String, Float>> filteredList = new ArrayList<>();
        float totalMinutes = 0f;

        for (Map.Entry<String, Float> entry : allData.entrySet()) {

            String label = entry.getKey();
            float minutes = entry.getValue();

            if (type.equals("study") && isStudy(label)) {

                // 如果是累计统计页面，合并补录数据
                if (isAllStudyTotal) {

                    if (label.contains("听")) {
                        minutes += EXTRA_LISTEN * 60f;
                    }

                    if (label.contains("读")) {
                        minutes += EXTRA_READ * 60f;
                    }
                }

                filteredList.add(
                        new java.util.AbstractMap.SimpleEntry<>(label, minutes)
                );

                totalMinutes += minutes;

            } else if (type.equals("rest") && isRest(label)) {

                filteredList.add(entry);
                totalMinutes += minutes;

            } else if (type.equals("other") && isOther(label)) {

                filteredList.add(entry);
                totalMinutes += minutes;
            }
        }



        tvTotal.setText(String.format("共 %.1f 小时", totalMinutes / 60f));

        rvDetail.setLayoutManager(new LinearLayoutManager(this));
        rvDetail.setAdapter(new YearDetailAdapter(filteredList));
    }

    private boolean isStudy(String label) {
        return label.contains("读")
                || label.contains("听")
                || label.contains("看")
                || label.contains("学")
                || label.contains("写");
    }

    private boolean isRest(String label) {
        return label.contains("睡")
                || label.contains("休")
                || label.contains("憩");
    }

    private boolean isOther(String label) {
        return !isStudy(label) && !isRest(label);
    }
}