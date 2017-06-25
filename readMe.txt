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

	
		 
   