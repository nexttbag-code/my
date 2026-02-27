package com.example.myapplication;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.myapplication.adpater.RecordPagerAdapter;
import com.example.myapplication.database.WorkDBHelper;
import com.example.myapplication.enity.Work;
import com.example.myapplication.util.CalculateUtil;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class RecordPagerActivity extends AppCompatActivity {

    private TextView tvMonth;
    private Button btnCopy;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private RecordPagerAdapter adapter;

    private String currentYearMonth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_pager);

        tvMonth = findViewById(R.id.tv_month);
        btnCopy = findViewById(R.id.btn_copy);
        viewPager = findViewById(R.id.vp_bill);
        tabLayout = findViewById(R.id.tab_layout);

        // 1. 获取当前年月
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;

        currentYearMonth = String.format("%d-%02d", year, month);
        tvMonth.setText(currentYearMonth);

        // 2. 创建「时间轴型」Adapter（支持跨年）
        adapter = new RecordPagerAdapter(
                this,
                year - 1, 1,     // 起始：去年 1 月
                year + 1, 12     // 结束：明年 12 月
        );

        viewPager.setAdapter(adapter);

        // 3. TabLayout 绑定（月标签）
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            String ym = adapter.getYearMonth(position);
            tab.setText(ym.substring(5) + "月");
        }).attach();

        // 4. 定位到当前月份
        int index = adapter.indexOf(currentYearMonth);
        if (index >= 0) {
            viewPager.setCurrentItem(index, false);
        }

        // 5. 页面切换时，更新顶部年月显示
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentYearMonth = adapter.getYearMonth(position);
                tvMonth.setText(currentYearMonth);
            }
        });

        // 6. 点击月份选择器
        tvMonth.setOnClickListener(v -> showDatePicker());

        // 7. 点击复制按钮
        btnCopy.setOnClickListener(v -> copyCurrentMonthData());
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("选择年月")
                .build();

        picker.show(getSupportFragmentManager(), "date_picker");

        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selection);

            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1; // 转成人类月份

            String targetYearMonth = String.format("%d-%02d", year, month);

            int index = adapter.indexOf(targetYearMonth);
            if (index >= 0) {
                viewPager.setCurrentItem(index, false);
            }
        });
    }



    private void copyCurrentMonthData() {
        WorkDBHelper helper = WorkDBHelper.getInstance(this);

        // 当前 ViewPager 正在显示的年月（权威来源）
        String yearMonth = currentYearMonth;

        List<Work> list = helper.queryByMonth(yearMonth);
        Map<String, Double> recordMap = CalculateUtil.calculateTime(list); // 返回分钟

        if (recordMap == null || recordMap.isEmpty()) {
            Toast.makeText(this, "当前月份无数据", Toast.LENGTH_SHORT).show();
            return;
        }

        // 将「小憩」累加到「睡觉」
        if (recordMap.containsKey("小憩")) {
            double napTime = recordMap.remove("小憩");
            recordMap.put("睡觉",
                    recordMap.getOrDefault("睡觉", 0.0) + napTime);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(yearMonth).append("\n");

        for (Map.Entry<String, Double> entry : recordMap.entrySet()) {
            double hours = entry.getValue() / 60.0; // 分钟 → 小时
            sb.append(entry.getKey())
                    .append("：")
                    .append(String.format("%.1f", hours))
                    .append("小时\n");
        }

        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("month_data", sb.toString());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "复制成功", Toast.LENGTH_SHORT).show();
    }


}
