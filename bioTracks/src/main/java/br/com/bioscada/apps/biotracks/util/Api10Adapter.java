/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package br.com.bioscada.apps.biotracks.util;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;

import br.com.bioscada.apps.biotracks.services.sensors.bluetooth.BluetoothConnectionManager;

/**
 * API level 10 specific implementation of the {@link ApiAdapter}.
 *
 * @author Jimmy Shih
 */
@TargetApi(10)
public class Api10Adapter extends Api9Adapter {

  private static final String TAG = Api10Adapter.class.getSimpleName();
  
  @Override
  public BluetoothSocket getBluetoothSocket(BluetoothDevice bluetoothDevice) throws IOException {
    try {
      return bluetoothDevice.createInsecureRfcommSocketToServiceRecord(
          BluetoothConnectionManager.MY_TRACKS_UUID);
    } catch (IOException e) {
      Log.d(TAG, "Unable to create insecure connection", e);
    }
    return bluetoothDevice.createRfcommSocketToServiceRecord(BluetoothConnectionManager.MY_TRACKS_UUID);
  };
}
