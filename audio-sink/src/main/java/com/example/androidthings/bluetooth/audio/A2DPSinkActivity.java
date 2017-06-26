/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidthings.bluetooth.audio;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.KeyEvent;

import com.example.androidthings.bluetooth.audio.dev.Config;
import com.example.androidthings.bluetooth.audio.dev.impl.LEDImpl;
import com.example.androidthings.bluetooth.audio.util.Network;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.pio.PeripheralManagerService;
import com.socks.library.KLog;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

/**
 * Sample usage of the A2DP sink bluetooth profile. At startup, this activity sets the Bluetooth
 * adapter in pairing mode for {@link #} ms.
 * activity 开启后进入比对模式  300ms延迟
 * <p>
 * To re-enable pairing mode, press "p" on an attached keyboard, use "adb shell input keyevent 44"
 * 重新进入比对模式，开发版输入”p“；或者 "adb shell input keyevent 44"
 * or press a button attached to the GPIO pin returned by {@link BoardDefaults#getGPIOForPairing()}
 * <p>
 * To forcefully disconnect any connected A2DP device, press "d" on an attached keyboard, use
 * "adb shell input keyevent 32" or press a button attached to the GPIO pin
 * 强行断开任何连接的蓝牙设备，press "d" on an attached keyboard, use "adb shell input keyevent 32"
 * returned by {@link BoardDefaults#getGPIOForDisconnectAllBTDevices()}
 * <p>
 * <p>
 * NOTE: While in pairing mode, pairing requests are auto-accepted - at this moment there's no
 * way to block specific pairing attempts while in pairing mode. This is known limitation that is
 * being worked on.
 * 在配对模式下，配对是自动完成的，并没有任何方式阻止它配对
 */
public class A2DPSinkActivity extends Activity {
    private static final String TAG = "A2DPSinkActivity";

    private static final String ADAPTER_FRIENDLY_NAME = "Cai Android Things device";

    private static final int REQUEST_CODE_ENABLE_DISCOVERABLE = 100;

    private static final String UTTERANCE_ID =
            "com.example.androidthings.bluetooth.audio.UTTERANCE_ID";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothProfile mA2DPSinkProxy;

    private ButtonInputDriver mPairingButtonDriver;
    private ButtonInputDriver mDisconnectAllButtonDriver;

    private TextToSpeech mTtsEngine;
    private LEDImpl led;

    private Handler handler = new Handler();



    /**
     * Handle an intent that is broadcast by the Bluetooth adapter whenever it changes its
     * state (after calling enable(), for example).
     * Action is {@link BluetoothAdapter#ACTION_STATE_CHANGED} and extras describe the old
     * and the new states. You can use this intent to indicate that the device is ready to go.
     * 蓝牙适配器状态改变
     */
    private final BroadcastReceiver mAdapterStateChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int oldState = A2dpSinkHelper.getPreviousAdapterState(intent);
            int newState = A2dpSinkHelper.getCurrentAdapterState(intent);
            KLog.d(TAG, "Bluetooth Adapter changing state from " + oldState + " to " + newState);
            if (newState == BluetoothAdapter.STATE_ON) {
                KLog.i(TAG, "Bluetooth Adapter is ready");
                initA2DPSink();
            }
        }
    };

    /**
     * Handle an intent that is broadcast by the Bluetooth A2DP sink profile whenever a device
     * connects or disconnects to it.
     * Action is {@link A2dpSinkHelper#ACTION_CONNECTION_STATE_CHANGED} and
     * extras describe the old and the new connection states. You can use it to indicate that
     * there's a device connected.
     * 设备连上或者断连
     */
    private final BroadcastReceiver mSinkProfileStateChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(A2dpSinkHelper.ACTION_CONNECTION_STATE_CHANGED)) {
                int oldState = A2dpSinkHelper.getPreviousProfileState(intent);
                int newState = A2dpSinkHelper.getCurrentProfileState(intent);
                BluetoothDevice device = A2dpSinkHelper.getDevice(intent);
                KLog.d(TAG, "Bluetooth A2DP sink changing connection state from " + oldState +
                        " to " + newState + " device " + device);
                if (device != null) {
                    String deviceName = Objects.toString(device.getName(), "a device");
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        speak("Connected to " + deviceName);
                        deviceConnectBleCallBack();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        speak("Disconnected from " + deviceName);
                        deviceDisconnectBleCallBack();
                    }
                }
            }
        }
    };

    /**
     * Handle an intent that is broadcast by the Bluetooth A2DP sink profile whenever a device
     * starts or stops playing through the A2DP sink.
     * Action is {@link A2dpSinkHelper#ACTION_PLAYING_STATE_CHANGED} and
     * extras describe the old and the new playback states. You can use it to indicate that
     * there's something playing. You don't need to handle the stream playback by yourself.
     * 开始播放或停止音频播放  ，不允许你自己修改音频播放流
     */
    private final BroadcastReceiver mSinkProfilePlaybackChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(A2dpSinkHelper.ACTION_PLAYING_STATE_CHANGED)) {
                int oldState = A2dpSinkHelper.getPreviousProfileState(intent);
                int newState = A2dpSinkHelper.getCurrentProfileState(intent);
                BluetoothDevice device = A2dpSinkHelper.getDevice(intent);
                KLog.d(TAG, "Bluetooth A2DP sink changing playback state from " + oldState +
                        " to " + newState + " device " + device);
                if (device != null) {
                    if (newState == A2dpSinkHelper.STATE_PLAYING) {
                        KLog.i(TAG, "Playing audio from device " + device.getAddress());
                    } else if (newState == A2dpSinkHelper.STATE_NOT_PLAYING) {
                        KLog.i(TAG, "Stopped playing audio from " + device.getAddress());
                    }
                }
            }
        }
    };


    private void deviceDisconnectBleCallBack() {
        if (led != null) {
//                            led.closeLightLED();
            led.setBlinkInvetTime(Config.BLE_DISCONENT_BLINK_INTERVAL);
            led.runInverTime();
        }
    }

    private void deviceConnectBleCallBack() {
        if (led != null) {
            led.stopInverTime();
            led.lightLED();
        }

        stopUnParingTimeOut();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Network.testNetwork();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            KLog.w(TAG, "No default Bluetooth adapter. Device likely does not support bluetooth.");
            return;
        }

        // We use Text-to-Speech to indicate status change to the user
        initTts();

        registerReceiver(mAdapterStateChangeReceiver, new IntentFilter(
                BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(mSinkProfileStateChangeReceiver, new IntentFilter(
                A2dpSinkHelper.ACTION_CONNECTION_STATE_CHANGED));
        registerReceiver(mSinkProfilePlaybackChangeReceiver, new IntentFilter(
                A2dpSinkHelper.ACTION_PLAYING_STATE_CHANGED));


        if (mBluetoothAdapter.isEnabled()) {
            KLog.d(TAG, "Bluetooth Adapter is already enabled.");
            initA2DPSink();
        } else {
            KLog.d(TAG, "Bluetooth adapter not enabled. Enabling.");
            mBluetoothAdapter.enable();
        }
        initBoardDevice();
    }

    private void initBoardDevice() {
        PeripheralManagerService service = new PeripheralManagerService();

        led = new LEDImpl();
        try {
            led.init(service);
            led.setBlinkInvetTime(300l);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_P:
                // Enable Pairing mode (discoverable)
                //进入配对模式 断开连接？
                disconnectConnectedDevices();
                enableDiscoverable();
                KLog.i("wei", "KEYCODE_P");
                return true;
            case KeyEvent.KEYCODE_D:
                // Disconnect any currently connected devices
                disconnectConnectedDevices();
                KLog.i("wei", "KEYCODE_D");
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }
//    input keyevent KEYCODE_MENU

    @Override
    protected void onDestroy() {
        super.onDestroy();
        KLog.d(TAG, "onDestroy");

        try {
            if (mPairingButtonDriver != null) mPairingButtonDriver.close();
        } catch (IOException e) { /* close quietly */}
        try {
            if (mDisconnectAllButtonDriver != null) mDisconnectAllButtonDriver.close();
        } catch (IOException e) { /* close quietly */}

        unregisterReceiver(mAdapterStateChangeReceiver);
        unregisterReceiver(mSinkProfileStateChangeReceiver);
        unregisterReceiver(mSinkProfilePlaybackChangeReceiver);

        if (mA2DPSinkProxy != null) {
            mBluetoothAdapter.closeProfileProxy(A2dpSinkHelper.A2DP_SINK_PROFILE,
                    mA2DPSinkProxy);
        }
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }

        if (mTtsEngine != null) {
            mTtsEngine.stop();
            mTtsEngine.shutdown();
        }

        destroyBoardDevice();
        // we intentionally leave the Bluetooth adapter enabled, so that other samples can use it
        // without having to initialize it.
    }

    private void destroyBoardDevice() {
        if (led != null) {
            try {
                led.destory();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Initiate the A2DP sink.
     */
    private void initA2DPSink() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            KLog.e(TAG, "Bluetooth adapter not available or not enabled.");
            return;
        }
        KLog.d(TAG, "Set up Bluetooth Adapter name and profile");
        mBluetoothAdapter.setName(ADAPTER_FRIENDLY_NAME);
        mBluetoothAdapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                mA2DPSinkProxy = proxy;
                enableDiscoverable();
            }

            @Override
            public void onServiceDisconnected(int profile) {
            }
        }, A2dpSinkHelper.A2DP_SINK_PROFILE);

        configureButton();
    }

    /**
     * Enable the current {@link BluetoothAdapter} to be discovered (available for pairing) for
     * the next {@link #} ms.
     * 使用android things 进入蓝牙被发现模式
     */
    private void enableDiscoverable() {
        KLog.d(TAG, "Registering for discovery.");
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                Config.DISCOVERABLE_TIMEOUT_MS);
        startActivityForResult(discoverableIntent, REQUEST_CODE_ENABLE_DISCOVERABLE);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ENABLE_DISCOVERABLE) {
            KLog.d(TAG, "Enable discoverable returned with result " + resultCode);

            // ResultCode, as described in BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE, is either
            // RESULT_CANCELED or the number of milliseconds that the device will stay in
            // discoverable mode. In a regular Android device, the user will see a popup requesting
            // authorization, and if they cancel, RESULT_CANCELED is returned. In Android Things,
            // on the other hand, the authorization for pairing is always given without user
            // interference, so RESULT_CANCELED should never be returned.
            if (resultCode == RESULT_CANCELED) {
                KLog.e(TAG, "Enable discoverable has been cancelled by the user. " +
                        "This should never happen in an Android Things device.");
                return;
            }
            KLog.i(TAG, "Bluetooth adapter successfully set to discoverable mode. " +
                    "Any A2DP source can find it with the name " + ADAPTER_FRIENDLY_NAME +
                    " and pair for the next " + Config.DISCOVERABLE_TIMEOUT_MS + " ms. " +
                    "Try looking for it on your phone, for example.");

            // There is nothing else required here, since Android framework automatically handles
            // A2DP Sink. Most relevant Bluetooth events, like connection/disconnection, will
            // generate corresponding broadcast intents or profile proxy events that you can
            // listen to and react appropriately.

            //到此 进入被发现状态  pairing 模式
            speak("Bluetooth audio sink is discoverable for " + Config.DISCOVERABLE_TIMEOUT_MS +
                    " milliseconds. Look for a device named " + ADAPTER_FRIENDLY_NAME);

            devcicePairingModeCallBack();
        }
    }

    private void devcicePairingModeCallBack() {
        //led快闪
        if (led != null) {
            led.setBlinkInvetTime(Config.BLE_PAIRING_BLINK_INTERVAL);
            led.runInverTime();
        }
        startUnParingTimeOut();
    }

    private void disconnectConnectedDevices() {
        if (mA2DPSinkProxy == null || mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            return;
        }
        speak("Disconnecting devices");
        for (BluetoothDevice device : mA2DPSinkProxy.getConnectedDevices()) {
            KLog.i(TAG, "Disconnecting device " + device);
            A2dpSinkHelper.disconnect(mA2DPSinkProxy, device);
        }

        stopUnParingTimeOut();
    }

    private void configureButton() {
        try {
            mPairingButtonDriver = new ButtonInputDriver(BoardDefaults.getGPIOForPairing(),
                    Button.LogicState.PRESSED_WHEN_LOW, KeyEvent.KEYCODE_P);
            mPairingButtonDriver.register();
            mDisconnectAllButtonDriver = new ButtonInputDriver(
                    BoardDefaults.getGPIOForDisconnectAllBTDevices(),
                    Button.LogicState.PRESSED_WHEN_LOW, KeyEvent.KEYCODE_D);
            mDisconnectAllButtonDriver.register();
        } catch (IOException e) {
            KLog.w(TAG, "Could not register GPIO button drivers. Use keyboard events to trigger " +
                    "the functions instead", e);
        }
    }

    private void initTts() {
        mTtsEngine = new TextToSpeech(A2DPSinkActivity.this,
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            mTtsEngine.setLanguage(Locale.US);
                        } else {
                            KLog.w(TAG, "Could not open TTS Engine (onInit status=" + status
                                    + "). Ignoring text to speech");
                            mTtsEngine = null;
                        }
                    }
                });
    }


    private void speak(String utterance) {
        KLog.i(TAG, utterance);
        if (mTtsEngine != null) {
            //暂时不需要输出语音提示
            //mTtsEngine.speak(utterance, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
        }
    }


    ///配对paring/discovery 到计时
    private int currentCountDownTime = 0;
    private int interval_time = 1;
    CountDownRunnable mCountDownRunnable;

    private void startUnParingTimeOut() {
        if (mCountDownRunnable == null) {
            mCountDownRunnable = new CountDownRunnable();
        }
        handler.post(mCountDownRunnable);
    }

    private void stopUnParingTimeOut() {
        if (mCountDownRunnable != null) {
            handler.removeCallbacks(mCountDownRunnable);
            mCountDownRunnable = null;
        }
    }

    private class CountDownRunnable implements Runnable {
        @Override
        public void run() {
            if (currentCountDownTime < Config.BLE_PAIRING_TIMEOUT) {

                handler.postDelayed(this, 1000);
                currentCountDownTime += interval_time;
            } else {
                //disconnet
                disconnectConnectedDevices();
            }
        }
    }
}