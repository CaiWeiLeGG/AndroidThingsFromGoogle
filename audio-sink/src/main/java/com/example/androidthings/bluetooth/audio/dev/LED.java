package com.example.androidthings.bluetooth.audio.dev;

import com.google.android.things.pio.PeripheralManagerService;

/**
 * Created by Administrator on 2017/6/22.
 */

public interface LED {
    void setBlinkInvetTime(long time);

    long getCurBlinkInvetTime();

    void init(PeripheralManagerService server) throws Exception;

    void destory() throws Exception;

    void runInverTime();

    void stopInverTime();

    void lightLED();

    void closeLightLED();

    void setPeripheralManagerService(PeripheralManagerService service);


    public String getGpioName();

    public void setGpioName(String gpioName);
}
