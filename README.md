Android Things Bluetooth Audio sample
=====================================

This sample demonstrates the use of Android Bluetooth APIs for audio from an
Android Things app. It demonstrates how to enable an A2DP sink on your
Android Things device and control lifecycle events, such as pairing, connection
and playback so that other devices, like a phone, can connect and play audio in
your Android Things device.

Pre-requisites
--------------

- Android Things compatible board
- Android Studio 2.2+
- (optional) a speaker or headsets, so that you can listen to the audio and
  notifications.
- (optional) Two buttons connected to the GPIO pins, so that you can control the
  sample at runtime. Without the buttons, you can use a keyboard or adb. For
  more on this, look at the main activity, where the supported commands are
  described.


Build and install
=================

On Android Studio, click on the "Run" button.

If you prefer to run on the command line, type

```bash
./gradlew installDebug
adb shell am start com.example.androidthings.bluetooth.audio/.A2DPSinkActivity
```

_Note_: If you connect an audio source to an Android Things audio sink (eg this
sample) but you can't hear your media playing through the audio jack, check if
you have an HDMI display connected. If so, the audio will be routed to the HDMI
output.

License
-------

Copyright 2017 The Android Open Source Project, Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.
