package com.example.androidthings.bluetooth.audio.dev.impl;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.example.androidthings.bluetooth.audio.dev.LED;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * Created by Administrator on 2017/6/22.
 */

public class LEDImpl implements LED {

    private String GpioName = "BCM17";
    private PeripheralManagerService server;
    private long blinkTime = 1000L;
    private Gpio ledGpio;
    private Handler handler = new Handler();
    private Runnable LEDBlinkRun;
    String TAG = "cai";

    @Override
    public void setBlinkInvetTime(long time) {
        this.blinkTime = time;
    }

    @Override
    public long getCurBlinkInvetTime() {
        return blinkTime;
    }

    @Override
    public void init(PeripheralManagerService server) throws Exception {
        if (server == null ) {
            throw new RuntimeException("PeripheralManagerService can not be empty");
        }
        setPeripheralManagerService(server);

        if (TextUtils.isEmpty(GpioName)) {
            throw new RuntimeException("GpioName can not be empty");
        }

        ledGpio = server.openGpio(GpioName);
        if (ledGpio == null) {
            throw new RuntimeException("ledGpio can not be empty");
        }

        ledGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);


    }

    @Override
    public void destory() throws Exception {
        stopInverTime();

        if (ledGpio != null) {
            ledGpio.close();
        }
    }

    @Override
    public void runInverTime() {
        if (LEDBlinkRun != null) {
            return;
        }

        LEDBlinkRun = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG,""+Thread.currentThread());
                if (ledGpio == null) {
                    throw new RuntimeException("ledGpio can not be null");
                }

                try {
                    ledGpio.setValue(!ledGpio.getValue());
                    handler.postDelayed(LEDBlinkRun,getCurBlinkInvetTime());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        handler.post(LEDBlinkRun);


    }

    @Override
    public void stopInverTime() {
        if (handler != null && LEDBlinkRun!= null) {
            handler.removeCallbacks(LEDBlinkRun);
            LEDBlinkRun = null;
        }
    }

    @Override
    public void lightLED() {
        try {
            if (ledGpio.getValue()) {
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        controlLED(true);
    }

    @Override
    public void closeLightLED() {
        try {
            if (!ledGpio.getValue()) {
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        controlLED(false);
    }

    private void controlLED(boolean state) {
        stopInverTime();
        if (ledGpio!=null) {
            try {
                ledGpio.setValue(state);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setPeripheralManagerService(PeripheralManagerService service) {
        this.server = service;
    }


    public String getGpioName() {
        return GpioName;
    }

    public void setGpioName(String gpioName) {
        GpioName = gpioName;
    }
}
