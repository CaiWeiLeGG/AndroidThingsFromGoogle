package com.example.androidthings.bluetooth.audio.util;


import com.socks.library.KLog;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Administrator on 2017-6-26.
 */

public class Network {

    public static void testNetwork() {
        String url = "https://www.baidu.com/";
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = okHttpClient.newCall(request);
        try {

            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    KLog.w(response.body().string());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
