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
import android.os.Build;
import android.util.Log;

import com.google.android.lib.mytracks.content.Sensor;

import java.util.UUID;
import java.util.Vector;

import br.com.bioscada.apps.biotracks.util.StdStats;


/**
 * LeHrmSensor extracts sensor data received from a Bluetooth Smart
 * HRM server such as a heart rate monitor.
 * 
 * @see <a href='https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml'>
 * HRM Characteristic specification at bluetooth.org</a>
 * 
 * @author dgupta
 * @author schmaedech
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class LeHrmSensor extends BluetoothLeSensor {
  private static final String TAG = LeHrmSensor.class.getSimpleName();
  
  // Bitmask to figure out whether heart rates are UINT8 or UINT16.
  private static final int HR_FMT_BITMASK = 0x01;

    private int lastHeartRate = 0;
    private Vector<Float> rrVector = new Vector<Float>();
    public static final LeHrmSensor INSTANCE = new LeHrmSensor();

  private LeHrmSensor() {
    // Nothing.
  }

  @Override
  public UUID getServiceUuid() {
    return UUID.fromString(uuid.HEART_RATE_UUID);
  }

  @Override
  public UUID getMeasurementUuid() {
    return UUID.fromString(uuid.HEART_RATE_MEASUREMENT_UUID);
  }
    private static double extractHeartRate(
            BluetoothGattCharacteristic characteristic) {

        int flag = characteristic.getProperties();
        Log.d(TAG, "Heart rate flag: " + flag);
        int format = -1;
        // Heart rate bit number format
        if ((flag & 0x01) != 0) {
            format = BluetoothGattCharacteristic.FORMAT_UINT16;
            Log.d(TAG, "Heart rate format UINT16.");
        } else {
            format = BluetoothGattCharacteristic.FORMAT_UINT8;
            Log.d(TAG, "Heart rate format UINT8.");
        }
        final int heartRate = characteristic.getIntValue(format, 1);
        Log.d(TAG, String.format("Received heart rate: %d", heartRate));
        return heartRate;
    }

    private static double extractContact(
            BluetoothGattCharacteristic characteristic) {

        int flag = characteristic.getProperties();
        int format = -1;
        // Sensor contact status
        if ((flag & 0x02) != 0) {
            Log.d(TAG, "Heart rate sensor contact info exists");
            if ((flag & 0x04) != 0) {
                Log.d(TAG, "Heart rate sensor contact is ON");
            } else {
                Log.d(TAG, "Heart rate sensor contact is OFF");
            }
        } else  {
            Log.d(TAG, "Heart rate sensor contact info doesn't exists");
        }
        //final int heartRate = characteristic.getIntValue(format, 1);
        //Log.d(TAG, String.format("Received heart rate: %d", heartRate));
        return 0.0d;
    }

    private static double extractEnergyExpended(
            BluetoothGattCharacteristic characteristic) {

        int flag = characteristic.getProperties();
        int format = -1;
        // Energy calculation status
        if ((flag & 0x08) != 0) {
            Log.d(TAG, "Heart rate energy calculation exists.");
        } else {
            Log.d(TAG, "Heart rate energy calculation doesn't exists.");
        }
        //final int heartRate = characteristic.getIntValue(format, 1);
        //Log.d(TAG, String.format("Received heart rate: %d", heartRate));
        return 0.0d;
    }

    private static Integer[] extractBeatToBeatInterval(
            BluetoothGattCharacteristic characteristic) {

        int flag = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        int format = -1;
        int energy = -1;
        int offset = 1; // This depends on hear rate value format and if there is energy data
        int rr_count = 0;

        if ((flag & 0x01) != 0) {
            format = BluetoothGattCharacteristic.FORMAT_UINT16;
          //  Log.d(TAG, "Heart rate format UINT16.");
            offset = 3;
        } else {
            format = BluetoothGattCharacteristic.FORMAT_UINT8;
          //  Log.d(TAG, "Heart rate format UINT8.");
            offset = 2;
        }
        if ((flag & 0x08) != 0) {
            // calories present
            energy = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
            offset += 2;
            Log.d(TAG, "Received energy: {}"+ energy);
        }
        if ((flag & 0x16) != 0){
            // RR stuff.
         //   Log.d(TAG, "RR stuff found at offset: "+ offset);
         //   Log.d(TAG, "RR length: "+ (characteristic.getValue()).length);
            rr_count = ((characteristic.getValue()).length - offset) / 2;
          //  Log.d(TAG, "RR length: "+ (characteristic.getValue()).length);
           // Log.d(TAG, "rr_count: "+ rr_count);
            if (rr_count > 0) {
                Integer[] mRr_values = new Integer[rr_count];
                for (int i = 0; i < rr_count; i++) {
                    mRr_values[i] = characteristic.getIntValue(
                            BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                    offset += 2;
                   // Log.d(TAG, "Received RR: " + mRr_values[i]);
                }
                return mRr_values;
            }
        }
        Log.d(TAG, "No RR data on this update: ");
        return null;
    }

  
  /**
   * Read populates a {@link com.google.android.lib.mytracks.content.Sensor.SensorDataSet} with content read from the Bluetooth
   * HRM server. Bluetooth Smart HRM servers are required to support notifications per
   * <a href='https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=239866'>
   * HRM Specification</a>. 
   */
  @Override
  public Sensor.SensorDataSet parseBuffer(BluetoothGatt gatt, BluetoothGattCharacteristic ch) {
      double heartRate = 0;
      double heartRateRC1 = 0;
      double heartRateRC2 = 0;
      double heartRateBPM = 0;
      double heartRateRMSSD = 0;
      heartRateBPM = extractHeartRate(ch);
      double contact = extractContact(ch);
      double energy = extractEnergyExpended(ch);
      Integer[] interval = extractBeatToBeatInterval(ch);

      if (interval != null) {
          for (int i = 0; i < interval.length; i++) {
              int crr = (int)interval[i].floatValue();

              if (crr < 300){
                  Log.d("POLAR", "INVALID < 300 | " + crr );
                  return null;
              }
              if (crr > 2000){
                  Log.d("POLAR", "INVALID > 2000 | " + crr );
                  return null;
              }
              if(i==1) {
                  heartRate = crr;
                  heartRateBPM = Math.round(60000/crr);
              }
              if(i==2) {
                  heartRateRC1 = crr;
              }
              if(i==3) {
                  heartRateRC2 = crr;
              }

              rrVector.add((float)crr);
              lastHeartRate = crr; // Remember good value for next time.
              Log.d("POLAR", System.currentTimeMillis() + " l " +rrVector.size()+  " i = " + i + " RR = " + crr+ " contact = " + contact+ " energy = " + energy);

          }
      }

      heartRateRMSSD = Math.round(StdStats.calculeRMSSD(rrVector));

      // Log.d("POLAR", " RR " + heartRate +" BPM " + heartRateBPM+" RMSSD " + heartRateRMSSD);
      // Heart Rate
      Sensor.SensorDataSet.Builder sds = Sensor.SensorDataSet.newBuilder().setCreationTime( System.currentTimeMillis());

      if(heartRate > 0){
          Sensor.SensorData.Builder rr = Sensor.SensorData.newBuilder().setValue((int)heartRate).setState(Sensor.SensorState.SENDING);
          sds.setHeartRate(rr);
      }

      if(heartRateRC1 > 0){
          Sensor.SensorData.Builder rrrc1 = Sensor.SensorData.newBuilder().setValue((int)heartRateRC1).setState(Sensor.SensorState.SENDING);
          sds.setHeartRateRc1(rrrc1);
      }
      if(heartRateRC2 > 0){
          Sensor.SensorData.Builder rrrc2 = Sensor.SensorData.newBuilder().setValue((int)heartRateRC2).setState(Sensor.SensorState.SENDING);
          sds.setHeartRateRc2(rrrc2);
      }
      if(heartRateBPM > 0){
          Sensor.SensorData.Builder rrbpm = Sensor.SensorData.newBuilder().setValue((int)heartRateBPM).setState(Sensor.SensorState.SENDING);
          sds.setBpm(rrbpm);
      }
      if(heartRateRMSSD > 0){
          Sensor.SensorData.Builder rrrmssd = Sensor.SensorData.newBuilder().setValue((int)heartRateRMSSD).setState(Sensor.SensorState.SENDING);
          sds.setRmssd(rrrmssd);
      }


    return sds.build();
  }
}
