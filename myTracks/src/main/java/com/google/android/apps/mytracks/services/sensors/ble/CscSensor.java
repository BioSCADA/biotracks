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

package com.google.android.apps.mytracks.services.sensors.ble;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.util.Log;

import com.google.android.apps.mytracks.content.Sensor.SensorData;
import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.google.android.apps.mytracks.content.Sensor.SensorState;
import com.google.android.apps.mytracks.services.sensors.ant.CadenceCounter;

import java.util.UUID;

/**
 * CscSensor extracts sensor data received from a Bluetooth Smart
 * CSC server such as a Cycling Speed & Cadence sensor.
 * 
 * @see <a href='https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml'>
 * HRM Characteristic specification at bluetooth.org</a>
 * 
 * @author dgupta
 * @author Frederic Nadeau
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CscSensor extends BluetoothLeSensor {
  private static final String TAG = CscSensor.class.getSimpleName();
  
  // Bitmask to figure out whether heart rates are UINT8 or UINT16.
  private static final int WHEEL_REVOLUTION_BITMASK = 0x01;
  
  // Bitmask to figure out whether heart rates are UINT8 or UINT16.
  private static final int CRANK_REVOLUTION_BITMASK = 0x02;
  
  private CadenceCounter wheelRevolutionCounter;
  private CadenceCounter crankRevolutionCounter;
  
  public static final CscSensor INSTANCE = new CscSensor();

  private CscSensor() {
    wheelRevolutionCounter = new CadenceCounter();
    crankRevolutionCounter = new CadenceCounter();
  }

  @Override
  public UUID getServiceUuid() {
    return UUID.fromString(ValuesUUID.CYCLING_SC_UUID);
  }

  @Override
  public UUID getMeasurementUuid() {
    return UUID.fromString(ValuesUUID.CSC_MEASUREMENT_UUID);
  }

  
  /**
   * Read populates a {@link SensorDataSet} with content read from the Bluetooth
   * HRM server. Bluetooth Smart HRM servers are required to support notifications per
   * <a href='https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=239866'>
   * HRM Specification</a>. 
   */
  @Override
  public SensorDataSet read(BluetoothGatt gatt, BluetoothGattCharacteristic ch) {
    byte[] val = ch.getValue();
    
    int flag = (int)val[0];
    
    Integer c1, c1EventTime, c2, c2EventTime;
    
    //TODO use flag to check C1 & C2 presence
    
    c1 = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 1);
    c1EventTime = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 5);
    c2 = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 7);
    c2EventTime = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 9);
    
    int crank = crankRevolutionCounter.getEventsPerMinute(c2, c2EventTime);
    
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      
      Log.d(TAG, "Read characteristic: CSC Flag = " + flag);
      Log.d(TAG, "Read characteristic: CSC Cumulative Wheel Revolutions = " + c1);
      Log.d(TAG, "Read characteristic: CSC Last Wheel Event Time = " + c1EventTime);
      Log.d(TAG, "Read characteristic: CSC Cumulative Crank Revolutions = " + c2);
      Log.d(TAG, "Read characteristic: CSC Last Crank Event Time = " + c2EventTime);
      
      Log.d(TAG, "crankRevolutionCounter = " + crank);
    }
    
    SensorData.Builder datum = SensorData.newBuilder()
        .setState(SensorState.SENDING)
        .setValue(crank);
    SensorDataSet dataset = SensorDataSet.newBuilder()
          .setCreationTime(System.currentTimeMillis())
          .setCadence(datum)
          .build();
    
    return dataset;
  }
}
