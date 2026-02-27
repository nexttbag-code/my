package com.example.myapplication.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.adpater.WorkListAdpater;
import com.example.myapplication.database.WorkDBHelper;
import com.example.myapplication.enity.Work;
import com.example.myapplication.util.CalculateUtil;

import java.util.List;
import java.util.Map;

public class RecordFragment extends Fragment {

    public static RecordFragment newInstance(String yearMonth) {
        RecordFragment fragment = new RecordFragment();
        Bundle args = new Bundle();
        args.putString("yearMonth", yearMonth);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_work, container, false);
        ListView lvWork = view.findViewById(R.id.lv_work);
        TextView emptyText = view.findViewById(R.id.empty_text);

        Bundle args = getArguments();
        if (args == null) return view;

        String yearMonth = args.getString("yearMonth");
        if (yearMonth == null) return view;

        // 提前拿 Context，避免子线程中为 null
        Context context = getContext();
        if (context == null) return view;

        new Thread(() -> {
            WorkDBHelper helper = WorkDBHelper.getInstance(context);
            List<Work> list = helper.queryByMonth(yearMonth);
            Map<String, Double> recordMap = CalculateUtil.calculateTime(list);

            if (recordMap != null && recordMap.containsKey("小憩")) {
                double napTime = recordMap.remove("小憩");
                recordMap.put("睡觉",
                        recordMap.getOrDefault("睡觉", 0.0) + napTime);
            }

            // Fragment 可能已经被销毁
            if (getView() == null) return;


            requireActivity().runOnUiThread(() -> {
                if (recordMap == null || recordMap.isEmpty()) {
                    lvWork.setVisibility(View.GONE);
                    emptyText.setVisibility(View.VISIBLE);
                } else {
                    lvWork.setVisibility(View.VISIBLE);
                    emptyText.setVisibility(View.GONE);
                    lvWork.setAdapter(new WorkListAdpater(context, recordMap));
                }
            });
        }).start();

        return view;
    }
}
