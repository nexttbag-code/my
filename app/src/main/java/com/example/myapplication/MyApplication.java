package com.example.myapplication;

import android.app.Application;
import android.content.Context;

public class MyApplication extends Application {
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        // 保存全局 Application Context
        mContext = getApplicationContext();
    }

    public static Context getContext() {
        return mContext;
    }
}
