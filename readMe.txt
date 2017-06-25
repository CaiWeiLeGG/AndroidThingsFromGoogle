Demo 来源: https://github.com/androidthings/sample-bluetooth-audio
##androidThings 蓝牙适配器
1 注册三个broadcastReceiver
   -----BluetoothAdapter.ACTION_STATE_CHANGED    蓝牙适配器状态改变
   ----- A2dpSinkHelper.ACTION_CONNECTION_STATE_CHANGED   音频设备连接  
   ----- A2dpSinkHelper.ACTION_PLAYING_STATE_CHANGED     音频设备播放   上面两者手机设备一连接上马上就会触发广播
   
2     mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      mBluetoothAdapter.enable(); 获取BluetoothAdapter  并让其使能

3  初始化mBluetoothAdapter的其他信息并获取代理对象BluetoothProfile （该对象用于断开连接 且可以获得所有连接的设备）
   让androidThings蓝牙进入被发现模式 enableDiscoverable(); 这是一种无可拒绝的模式  任何设备都可以连接
        ---其实就是开启一个意图的activity 并在onActivityResult处做监听（RESULT_CANCELED永远不会被调用）
   	
	
last 结束生命周期ondestory（）
    ----释放代理对象 
	    if (mA2DPSinkProxy != null) {
            mBluetoothAdapter.closeProfileProxy(A2dpSinkHelper.A2DP_SINK_PROFILE,
                    mA2DPSinkProxy);
        }	
   
##补充部分：
BrroadcastReceiver : A2dpSinkHelper.ACTION_CONNECTION_STATE_CHANGED两个状态  
          --BluetoothProfile.STATE_CONNECTED   手机外设连接
		  --BluetoothProfile.STATE_DISCONNECTED  手机外设断开连接
	 
BroadcastReceiver :A2dpSinkHelper.ACTION_PLAYING_STATE_CHANGED 三个extra状态    经测试：只有首次外设连接有action通知，更改播放器状态没有通知响应
       ---BluetoothProfile.EXTRA_PREVIOUS_STATE  之前状态
	   ---BluetoothProfile.EXTRA_STATE  当前状态  (上面两者可能是STATE_NOT_PLAYING或者STATE_PLAYING)
	   ---BluetoothDevice.EXTRA_DEVICE  设备---mac
	
可以手动断开连接和启动进入被发现模式


##无关紧要的知识点：
how to use TextToSpeech (一个可以将文本转成语音  目前知道的版本语音是English)
1 初始化
 private void initTts() {
        mTtsEngine = new TextToSpeech(A2DPSinkActivity.this,
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            mTtsEngine.setLanguage(Locale.US);
                        } else {
                            Log.w(TAG, "Could not open TTS Engine (onInit status=" + status
                                    + "). Ignoring text to speech");
                            mTtsEngine = null;
                        }
                    }
                });
    }
	
2 调用
 private static final String UTTERANCE_ID =
            "com.example.androidthings.bluetooth.audio.UTTERANCE_ID";
   private void speak(String utterance) {
        Log.i(TAG, utterance);
        if (mTtsEngine != null) {
            mTtsEngine.speak(utterance, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
        }
    }

last destroy
   if (mTtsEngine != null) {
            mTtsEngine.stop();
            mTtsEngine.shutdown();
        }

	
	
	
===================================================================================
通过外设设备驱动实现外设和板子的交互（驱动framework）
初始化驱动lib包

    1搜索驱动包索引找到匹配外设的驱动lib
    2添加上面的lib声明到app的build.gradle文件中
	 compile 'com.google.android.things.contrib:driver-button:0.3'

    3使用适当的外设IO来初始化驱动类
	    --p1：pin脚name;p2:低压;p3:映射的按键码
	mButtonInputDriver = new ButtonInputDriver(
                    BUTTON_PIN_NAME,
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_SPACE);

	
    4当app不在需要外设时，关闭创建的连接
     mButtonInputDriver.close();


    5生命周期
 @Override
    protected void onStart() {
        super.onStart();
        mButtonInputDriver.register();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mButtonInputDriver.unregister();
    }
	
	6监听事件
	 @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            // Handle button pressed event
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            // Handle button released event
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

=================================================================
处理按钮开关的事件（输入事件的处理）(GPIO)

当连接到GPIO端口按钮被按下时为了接收到事件需要做：

    1使用PeripheralManagerService打开一个接有按钮开关的GPIO端口的连接
	 PeripheralManagerService service = new PeripheralManagerService();
	  mButtonGpio = service.openGpio(BUTTON_PIN_NAME);

    2配置端口的类型为DIRECTION_IN
	  mButtonGpio.setDirection(Gpio.DIRECTION_IN);

    3使用api（setEdgeTriggerType()）来配置回调事件的状态转变类型
	   mButtonGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);

    4注册一个GPIO回调来接受事件
	  mButtonGpio.registerGpioCallback(mCallback);

    5如果继续接受未来的事件在api（onGpioEdge（））中返回true
	 private GpioCallback mCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            Log.i(TAG, "GPIO changed, button pressed");

            // Step 5. Return true to keep callback active.
            return true;
        }
    };

    6当app不在需要GPIO连接的时候，关闭GPIO连接，节省资源和提升性能
	 @Override
    protected void onDestroy() {
        super.onDestroy();

        // Step 6. Close the resource
        if (mButtonGpio != null) {
            mButtonGpio.unregisterGpioCallback(mCallback);
            try {
                mButtonGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    }
======================================================================
开关LED灯	（输出事件的处理）(GPIO)
	
  1  使用PheralManagerService打开一个连接上led灯的GPIO连接
  PeripheralManagerService service = new PeripheralManagerService();

  2  配置端口类型为：DIRECTION_OUT_INITIALLY_LOW
       mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

  3  通过两个api（setValue()和getValue()）开关控制led灯的状态
     mLedGpio.setValue(!mLedGpio.getValue());

  4  短暂的延迟后，使用handler去处理开关GPIO端口的事件
  
  5  当app不再需要GPIO连接的时候，关闭GPIO连接进而节省资源，即：
    if (mLedGpio != null) {
            try {
                mLedGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }


	 
		 
   