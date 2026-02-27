package com.example.myapplication.adpater;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.util.List;
import java.util.Map;

public class YearDetailAdapter extends RecyclerView.Adapter<YearDetailAdapter.VH> {

    private final List<Map.Entry<String, Float>> list;

    public YearDetailAdapter(List<Map.Entry<String, Float>> data) {
        list = data;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_year_detail, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        Map.Entry<String, Float> e = list.get(position);
        holder.tvName.setText(e.getKey());
        holder.tvValue.setText(String.format("%.1f h", e.getValue() / 60f));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvValue;
        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvValue = v.findViewById(R.id.tvValue);
        }
    }
}
