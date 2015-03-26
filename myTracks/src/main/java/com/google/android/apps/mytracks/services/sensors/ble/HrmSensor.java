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

import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Sensor.SensorData;
import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.google.android.apps.mytracks.content.Sensor.SensorState;
import com.google.android.apps.mytracks.util.StdStats;

import java.util.UUID;
import java.util.Vector;

/**
 * HrmSensor extracts sensor data received from a Bluetooth Smart
 * HRM server such as a heart rate monitor.
 * 
 * @see <a href='https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml'>
 * HRM Characteristic specification at bluetooth.org</a>
 * 
 * @author dgupta
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class HrmSensor extends BluetoothLeSensor {
  private static final String TAG = HrmSensor.class.getSimpleName();

    private int lastHeartRate = 0;
    private Vector<Float> rrVector = new Vector<Float>();
  // Bitmask to figure out whether heart rates are UINT8 or UINT16.
  private static final int HR_FMT_BITMASK = 0x01;
  
  public static final HrmSensor INSTANCE = new HrmSensor();

  private HrmSensor() {
    // Nothing.
  }

  @Override
  public UUID getServiceUuid() {
    return UUID.fromString(ValuesUUID.HEART_RATE_UUID);
  }

  @Override
  public UUID getMeasurementUuid() {
    return UUID.fromString(ValuesUUID.HEART_RATE_MEASUREMENT_UUID);
  }

  
  /**
   * Read populates a {@link SensorDataSet} with content read from the Bluetooth
   * HRM server. Bluetooth Smart HRM servers are required to support notifications per
   * <a href='https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=239866'>
   * HRM Specification</a>. 
   */
  @Override
  public SensorDataSet read(BluetoothGatt gatt, BluetoothGattCharacteristic ch) {
      int heartRate = 0;
      int heartRateBPM = 0;
      int heartRateRMSSD = 0;
    byte[] val = ch.getValue();
    int format = (((int)val[0] & HR_FMT_BITMASK) == 0 
        ? BluetoothGattCharacteristic.FORMAT_UINT8 
            : BluetoothGattCharacteristic.FORMAT_UINT16);
    
    // Heart rate (unit BPM) is at offset 1.
    Integer hr = ch.getIntValue(format, 1);
      heartRate = hr.intValue();
    // TODO(dgupta): read RR intervals once we can log them.
   
    // TODO(dgupta): debug: remove this logging.
    /*if (Log.isLoggable(TAG, Log.DEBUG)) {
      String valStr = "";
      for (byte b : val) {
        valStr += " . " + b;
      }
      Log.d(TAG, "Read characteristic: heart rate = " + hr + ", byteval = " + valStr);
    }*/
    Log.d(TAG, "Read characteristic: HRM format = " + format);
    Log.d(TAG, "Read characteristic: HRM hr = " + hr);


      lastHeartRate = heartRate; // Remember good value for next time.
      heartRateBPM = Math.round(60000/heartRate);
      rrVector.add((float)lastHeartRate);
      try{
          heartRateRMSSD = Math.round(StdStats.calculeRMSSD(rrVector));
      }catch (Exception e){
          heartRateRMSSD = 0;
      }

      // Heart Rate
      Sensor.SensorDataSet.Builder sds = Sensor.SensorDataSet.newBuilder().setCreationTime(System.currentTimeMillis());
      Sensor.SensorData.Builder rr = Sensor.SensorData.newBuilder().setValue(heartRate).setState(Sensor.SensorState.SENDING);
      sds.setHeartRate(rr);

      Sensor.SensorData.Builder rrbpm = Sensor.SensorData.newBuilder().setValue(heartRateBPM).setState(Sensor.SensorState.SENDING);
      sds.setBpm(rrbpm);

      Sensor.SensorData.Builder rrrmssd = Sensor.SensorData.newBuilder().setValue(heartRateRMSSD).setState(Sensor.SensorState.SENDING);
      sds.setRmssd(rrrmssd);
      return sds.build();
  }
}
