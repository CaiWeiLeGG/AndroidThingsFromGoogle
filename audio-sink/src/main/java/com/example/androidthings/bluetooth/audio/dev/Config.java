package com.example.androidthings.bluetooth.audio.dev;

/**
 * Created by Administrator on 2017-6-25.
 */

public interface Config {
    //断开连接
    long BLE_DISCONENT_BLINK_INTERVAL = 500l;
    //配对
    long BLE_PAIRING_BLINK_INTERVAL = 200l;

    int COMMONT_TIMEOUT = 300;
    //配对超时
//    long BLE_PAIRING_TIMEOUT = 5*60*1000l;
    int BLE_PAIRING_TIMEOUT = COMMONT_TIMEOUT;
   //diccoverable pairing 时间，过了蓝牙就发现不了；默认120秒
    int DISCOVERABLE_TIMEOUT_MS = COMMONT_TIMEOUT;
}
