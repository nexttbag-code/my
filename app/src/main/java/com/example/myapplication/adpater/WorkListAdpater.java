package com.example.myapplication.adpater;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.util.CalculateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkListAdpater extends BaseAdapter {
    private final Context mContext;
    private final Map<String, Double> mWork;
    private final List<String> mKeys;

    public WorkListAdpater(Context context, Map<String, Double> recordMap) {
        this.mContext = context;
        this.mWork = recordMap;
        this.mKeys = new ArrayList<>(recordMap.keySet()); // 把 key 转成 list 便于 get(i)
    }

    @Override
    public int getCount() {
        return mWork != null ? mKeys.size() + 1 : 0;
    }


    @Override
    public Object getItem(int i) {
        if (i < mKeys.size()) {
            return mWork.get(mKeys.get(i));
        }
        return null;
    }


    @Override
    public long getItemId(int i) {
        return i; // 这里只能返回 position，因为 Map 没有 id
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = LayoutInflater.from(mContext).inflate(R.layout.item_work, null);
            holder.tv_remark = view.findViewById(R.id.tv_remark);
            holder.tv_time = view.findViewById(R.id.tv_time);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        if (i < mKeys.size()) {
            String activity = mKeys.get(i);
            Double duration = mWork.get(activity);
            holder.tv_remark.setText(activity);
            holder.tv_time.setText(CalculateUtil.format(duration));
            holder.tv_remark.setTextColor(0xFF333333);
            holder.tv_remark.setTypeface(null, android.graphics.Typeface.NORMAL);

        } else {
            double total = 0;
            if (mWork.containsKey("阅读")) {
                total += mWork.get("阅读");
            }
            if (mWork.containsKey("听力")) {
                total += mWork.get("听力");
            }
            holder.tv_remark.setText("学习总计");
            holder.tv_time.setText(CalculateUtil.format(total));
            holder.tv_remark.setTextColor(0xFFD32F2F);
            holder.tv_remark.setTypeface(null, android.graphics.Typeface.BOLD);

        }

        return view;
    }


    public static final class ViewHolder {
        public TextView tv_remark;
        public TextView tv_time;
    }
}
