package com.example.androidthings.bluetooth.audio.app;

import android.app.Application;

import com.example.androidthings.bluetooth.audio.BuildConfig;
import com.socks.library.KLog;

public class KLogApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
//        KLog.init(BuildConfig.LOG_DEBUG);
        KLog.init(BuildConfig.LOG_DEBUG, "Cai");

    }
}
