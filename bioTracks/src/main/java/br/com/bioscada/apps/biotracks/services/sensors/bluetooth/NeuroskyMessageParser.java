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

import com.google.android.lib.mytracks.content.Sensor;

import br.com.bioscada.apps.biotracks.services.sensors.SensorUtils;


/**
 * An implementation of a Sensor MessageParser for Neurosky Bluetooth . 
 *  http://developer.neurosky.com/docs/doku.php?id=thinkgear_communications_protocol
 * @author Diego Schmaedech (modification to get attention and medidation values)
 */
public class NeuroskyMessageParser implements MessageParser {

  private int lastAttention = 0; 
  private int lastMeditation = 0; 
  public static final byte PARSER_BATTERY_CODE   = (byte) 0x01;
  public static final byte PARSER_CODE_ATTENTION   = (byte) 0x04;
  public static final byte PARSER_CODE_MEDITATION  = (byte) 0x05;  
  private static final byte PARSER_SYNC_BYTE   = (byte) 0xAA;
  private static final byte PARSER_EXCODE_BYTE = (byte) 0x55;
  private static final int PARSER_STATE_PAYLOAD_LENGTH = 0x08;
  
 
  @Override
  public Sensor.SensorDataSet parseBuffer(byte[] buffer) {
    int attention = 0;
    int meditation = 0; 
    int batery = 0; 
   // print(buffer);
    for (int i = 0; i < buffer.length-8; i++)  
    {
//      Log.d("NEUROSKY", "i = " + i + " CODE  = " +  readUnsignedByte(buffer[i])); 
        if( (buffer[i] == PARSER_SYNC_BYTE) && (buffer[i+1] == PARSER_SYNC_BYTE) ){ 
          
           // Log.d("NEUROSKY", "i = " + i + " SYNC = " + " " + readUnsignedByte(buffer[i])+ " " + readUnsignedByte(buffer[i+1])+ " " + readUnsignedByte(buffer[i+2]) + " " + readUnsignedByte(buffer[i+3])); 
            if( ( SensorUtils.readUnsignedByte(buffer[i + 5]) == 0)  ){
               attention = SensorUtils.readUnsignedByte(buffer[i+6]);
               meditation = SensorUtils.readUnsignedByte(buffer[i+7] );
               //Log.d("NEUROSKY", "i = " + i + " SYNC = " + " " + SensorUtils.readUnsignedByte(buffer[i])+ " " + SensorUtils.readUnsignedByte(buffer[i+1])+ " " + SensorUtils.readUnsignedByte(buffer[i+2]) + " " + SensorUtils.readUnsignedByte(buffer[i+3])+ " " + SensorUtils.readUnsignedByte(buffer[i+4])+ " " + SensorUtils.readUnsignedByte(buffer[i+5])+ " " + SensorUtils.readUnsignedByte(buffer[i+6])+ " " + SensorUtils.readUnsignedByte(buffer[i+7])); 
               
            }
          
        } 
// these parsers can't be seen ate mindwave mobile
//      if( buffer[i] == PARSER_STATE_PAYLOAD_LENGTH  ){ 
//        
//        Log.d("NEUROSKY", "i = " + i + " PAYLOAD = " + Integer.toHexString(buffer[i]) + " " + readUnsignedByte(buffer[i]) ); 
//      }  
//      if( buffer[i]== PARSER_BATTERY_CODE ){ 
//        batery = readUnsignedByte(buffer[i+1]);
//        Log.d("NEUROSKY", "i = " + i + " BAT  = " + batery); 
//      }  
//      if( buffer[i]== PARSER_EXCODE_BYTE ){ 
//        
//        Log.d("NEUROSKY", "i = " + i + " EXCODE  = " + Integer.toHexString(buffer[i]) + " " + readUnsignedByte(buffer[i])); 
//      }  
//      if( buffer[i] == PARSER_CODE_ATTENTION ){ 
//        attention = readUnsignedByte(buffer[i+1] );
     //  Log.d("NEUROSKY", "i = " + i + " ATTENTION  = " + attention); 
//      }  
//      if(buffer[i] == PARSER_CODE_MEDITATION ){        
//        meditation = readUnsignedByte(buffer[i+1] );
      // Log.d("NEUROSKY", "i = " + i + " MEDITATION = " + meditation); 
//      } 
      
    } 
    
    if(attention < 100 && attention > 0)
      lastAttention = attention;  
    if(meditation < 100 && meditation > 0)
      lastMeditation = meditation;  
    
    Sensor.SensorDataSet.Builder sds = Sensor.SensorDataSet.newBuilder().setCreationTime( System.currentTimeMillis()); 
    Sensor.SensorData.Builder att = Sensor.SensorData.newBuilder().setValue(lastAttention).setState(Sensor.SensorState.SENDING);
    sds.setAttention(att);
    
    Sensor.SensorData.Builder med = Sensor.SensorData.newBuilder().setValue(lastMeditation).setState(Sensor.SensorState.SENDING);
    sds.setMeditation(med);
    
    Sensor.SensorData.Builder batteryLevel = Sensor.SensorData.newBuilder().setValue(batery).setState(Sensor.SensorState.SENDING);
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
    return 170;
  }

  private boolean packetValid(byte[] buffer, int i) {     
    return (buffer[i] == PARSER_SYNC_BYTE) && (buffer[i+1] == PARSER_SYNC_BYTE);
  }
  /**
   * Searches buffer for the beginning of a valid packet.
   * 
   * @param buffer an array of bytes to parse
   * @return index to beginning of good packet, or -1 if none found.
   */
  @Override
  public int findNextAlignment(byte[] buffer) {
  
    for (int i = 0; i < buffer.length; i++) {
      if ((buffer[i] == PARSER_SYNC_BYTE) && (buffer[i+1] == PARSER_SYNC_BYTE) ){
        return i;
      }
    }
    return -1; 
  }


}