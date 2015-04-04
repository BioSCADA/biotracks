/*
 * Copyright 2011 Google Inc.
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
package br.com.bioscada.apps.biotracks.services.sensors.bluetooth;

import android.util.Log;

import com.google.android.lib.mytracks.content.Sensor;

import java.util.Vector;

import br.com.bioscada.apps.biotracks.services.sensors.SensorUtils;
import br.com.bioscada.apps.biotracks.util.StdStats;

/**
 * An implementation of a Sensor MessageParser for Polar Wearlink Bluetooth HRM.
 * Polar Bluetooth Wearlink packet example; Hdr Len Chk Seq Status HeartRate
 * RRInterval_16-bits FE 08 F7 06 F1 48 03 64 where; Hdr always = 254 (0xFE),
 * Chk = 255 - Len Seq range 0 to 15 Status = Upper nibble may be battery
 * voltage bit 0 is Beat Detection flag. Additional packet examples; FE 08 F7 06
 * F1 48 03 64 FE 0A F5 06 F1 48 03 64 03 70
 *
 * @author John R. Gerthoffer 
 * @author Diego Schmaedech (modification to get real values of RR) 
 */
public class PolarMessageParser implements MessageParser {

    private int lastHeartRate = 0;
    private Vector<Float> rrVector = new Vector<Float>();
    /**
     * Applies Polar packet validation rules to buffer. Polar packets are checked
     * for following; offset 0 = header byte, 254 (0xFE). offset 1 = packet length
     * byte, 8, 10, 12, 14. offset 2 = check byte, 255 - packet length. offset 3 =
     * sequence byte, range from 0 to 15.
     *
     * @param buffer an array of bytes to parse
     * @param i buffer offset to beginning of packet.
     * @return whether buffer has a valid packet at offset i
     */
    private boolean packetValid(byte[] buffer, int i) {
        boolean headerValid = (buffer[i] & 0xFF) == 0xFE;
        boolean checkbyteValid = (buffer[i + 2] & 0xFF) == (0xFF - (buffer[i + 1] & 0xFF));
        boolean sequenceValid = (buffer[i + 3] & 0xFF) < 16;

        return headerValid && checkbyteValid && sequenceValid;
    }

    /**
     * Parse a valid packet to really RR values.
     *
     * @param buffer an array of bytes to parse
     * @return Sensor.SensorDataSet.
     */
    @Override
    public Sensor.SensorDataSet parseBuffer(byte[] buffer) {


        int heartRate = 0;
        int heartRateRC1 = 0;
        int heartRateRC2 = 0;
        int heartRateBPM = 0;
        int heartRateRMSSD = 0;

        int iSize = SensorUtils.readUnsignedByte(buffer[1]);
        int iBat = SensorUtils.readUnsignedByte(buffer[5]);

        for ( int i = 6; i < iSize; i = i + 2) // different number of RRI intervals
        {
            int crr = 0;

            if (packetValid(buffer, 0)){
                crr = SensorUtils.unsignedShortToInt(buffer, i);
            }else{
                Log.d("POLAR", "INVALID < packetValid | " + crr );
                return null;
            }
            if (crr < 300){
                Log.d("POLAR", "INVALID < 300 | " + crr );
                return null;
            }
            if (crr > 2000){
                Log.d("POLAR", "INVALID > 2000 | " + crr );
                return null;
            }

            if(i==6) {
                heartRate = crr;
                heartRateBPM = Math.round(60000/crr);
            }
            if(i==8) {
                heartRateRC1 = crr;
            }
            if(i==10) {
                heartRateRC2 = crr;
            }
            
            rrVector.add((float)crr);
            lastHeartRate = crr; // Remember good value for next time.

            Log.d("POLAR", System.currentTimeMillis() + " l " +rrVector.size()+  " i = " + i + " RR = " + crr+ " iBat = " + iBat+ " iSize = " + iSize);

        }

        heartRateRMSSD = Math.round(StdStats.calculeRMSSD(rrVector));

        // Log.d("POLAR", " RR " + heartRate +" BPM " + heartRateBPM+" RMSSD " + heartRateRMSSD);
        // Heart Rate
        Sensor.SensorDataSet.Builder sds = Sensor.SensorDataSet.newBuilder().setCreationTime( System.currentTimeMillis());

        if(heartRate > 0){
            Sensor.SensorData.Builder rr = Sensor.SensorData.newBuilder().setValue(heartRate).setState(Sensor.SensorState.SENDING);
            sds.setHeartRate(rr);
        }

        if(heartRateRC1 > 0){
            Sensor.SensorData.Builder rrrc1 = Sensor.SensorData.newBuilder().setValue(heartRateRC1).setState(Sensor.SensorState.SENDING);
            sds.setHeartRateRc1(rrrc1);
        }
        if(heartRateRC2 > 0){
            Sensor.SensorData.Builder rrrc2 = Sensor.SensorData.newBuilder().setValue(heartRateRC2).setState(Sensor.SensorState.SENDING);
            sds.setHeartRateRc2(rrrc2);
        }
        if(heartRateBPM > 0){
            Sensor.SensorData.Builder rrbpm = Sensor.SensorData.newBuilder().setValue(heartRateBPM).setState(Sensor.SensorState.SENDING);
            sds.setBpm(rrbpm);
        }
        if(heartRateRMSSD > 0){
            Sensor.SensorData.Builder rrrmssd = Sensor.SensorData.newBuilder().setValue(heartRateRMSSD).setState(Sensor.SensorState.SENDING);
            sds.setRmssd(rrrmssd);
        }
        Sensor.SensorData.Builder batteryLevel = Sensor.SensorData.newBuilder().setValue(iBat).setState(Sensor.SensorState.SENDING);
        sds.setBatteryLevel(batteryLevel);

        return sds.build();
    }

    /**
     * Applies packet validation rules to buffer
     *
     * @param buffer an array of bytes to parse
     * @return whether buffer has a valid packet starting at index zero
     */
    @Override
    public boolean isValid(byte[] buffer) {
        return packetValid(buffer, 0);
    }

    /**
     * Polar uses variable packet sizes; 8, 10, 12, 14 and rarely 16. The most
     * frequent are 8 and 10. We will wait for 16 bytes. This way, we are assured
     * of getting one good one.
     *
     * @return the size of buffer needed to parse a good packet
     */
    @Override
    public int getFrameSize() {
        return 16;
    }

    /**
     * Searches buffer for the beginning of a valid packet.
     *
     * @param buffer an array of bytes to parse
     * @return index to beginning of good packet, or -1 if none found.
     */
    @Override
    public int findNextAlignment(byte[] buffer) {
        // Minimum length Polar packets is 8, so stop search 8 bytes before buffer
        // ends.
        for (int i = 0; i < buffer.length - 8; i++) {
            if (packetValid(buffer, i)) {
                return i;
            }
        }
        return -1;
    }

}