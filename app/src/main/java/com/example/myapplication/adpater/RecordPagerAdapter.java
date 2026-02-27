package com.example.myapplication.adpater;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.myapplication.fragment.RecordFragment;

import java.util.ArrayList;
import java.util.List;

public class RecordPagerAdapter extends FragmentStateAdapter {

    private final List<String> yearMonthList = new ArrayList<>();

    public RecordPagerAdapter(@NonNull FragmentActivity fa,
                              int startYear, int startMonth,
                              int endYear, int endMonth) {
        super(fa);

        int y = startYear;
        int m = startMonth;

        while (y < endYear || (y == endYear && m <= endMonth)) {
            yearMonthList.add(String.format("%d-%02d", y, m));
            m++;
            if (m == 13) {
                m = 1;
                y++;
            }
        }
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return RecordFragment.newInstance(yearMonthList.get(position));
    }

    @Override
    public int getItemCount() {
        return yearMonthList.size();
    }

    public String getYearMonth(int position) {
        return yearMonthList.get(position);
    }

    public int indexOf(String yearMonth) {
        return yearMonthList.indexOf(yearMonth);
    }
}
