/*
 * Copyright 2014 Google Inc.
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

package br.com.bioscada.apps.biotracks.services.sensors.btle;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;
import android.util.Log;

import com.google.android.lib.mytracks.content.Sensor;

import java.util.UUID;

/**
 * BluetoothLeSensor represents a Bluetooth Smart sensor that 
 * supports reading data from a Bluetooth GATT server using notifications.
 * 
 * @author dgupta
 * @author schmaedech
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class BluetoothLeSensor {
  private static final String TAG = BluetoothLeSensor.class.getSimpleName();
  
  private int mState = STATE_UNCONFIGURED;
  
  /**
   *  UUID for descriptor to enable notifications when a characteristic's
   *  value changes.
   */
  private static UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString(
      "00002902-0000-1000-8000-00805f9b34fb");

  protected BluetoothLeSensor() {
  }
  
  public static final int STATE_UNCONFIGURED = -1;
  public static final int STATE_DISCONNECTED = 0;
  public static final int STATE_CONNECTING = 1;
  public static final int STATE_CONNECTED = 2;
  
  /**
   * @return a UUID representing the characteristic.
   */
  public abstract UUID getServiceUuid();
  
  /**
   * @return a UUID representing the characteristic.
   */
  public abstract UUID getMeasurementUuid();

  /**
   * Reads the current value of a characteristic from a Bluetooth Smart server.
   * Note that this operation does not imply reading data from the peer; the
   * characteristic will already have the latest value once it starts receiving
   * notifications from the peer.
   * 
   * @param gatt A GATT endpoint representing the remote server
   * @param ch The characteristic associated with {@link gatt}.
   * @return A {@link com.google.android.lib.mytracks.content.Sensor.SensorDataSet} representing the latest value.
   */
  public abstract Sensor.SensorDataSet parseBuffer(BluetoothGatt gatt, BluetoothGattCharacteristic ch);
  
  public int getState(){
    return mState;
  }
  public void setState(int state){
    mState = state;
  }
  
  /**
   * Turns on notifications for characteristic value changes from a GATT server.
   */
  public boolean subscribe(BluetoothGatt gatt, BluetoothGattCharacteristic ch) {
    if (!getMeasurementUuid().equals(ch.getUuid())) {
      return false;
    }

    // Turn on notification for the HRM_CHARACTERISTIC.
    if (!gatt.setCharacteristicNotification(ch, true)) {
      Log.e(TAG, "Failed to start notifier for characteristic " + ch.getUuid());
      return false;
    }

    // In addition, update the descriptor to indicate that notifications
    // are enabled.
    BluetoothGattDescriptor descriptor = ch.getDescriptor(
        BluetoothLeSensor.CLIENT_CHARACTERISTIC_CONFIG);
    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    if (!gatt.writeDescriptor(descriptor)) {
      Log.e(TAG, "Failed to write notification descriptor for characteristic: " + ch.getUuid());
      return false;
    }

    return true;
  }
}