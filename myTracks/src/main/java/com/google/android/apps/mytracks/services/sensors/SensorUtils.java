/*
 * Copyright 2010 Google Inc.
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
package com.google.android.apps.mytracks.services.sensors;

import android.content.Context;

import com.google.android.apps.mytracks.content.Sensor;

import br.com.bioscada.apps.biotracks.R;

/**
 * A collection of methods for message parsers.
 *
 * @author Sandor Dornbush
 * @author Nico Laum
 */
public class SensorUtils {

  private SensorUtils() {
  }

  /**
   * @param b is the byte to convert
   * @return a integer from the given byte
   */
  public static int readUnsignedByte(byte b) {
    return (b & 0xFF);
  }
 

  public static int toInt(byte[] b) {
    return (((int) b[3]) & 0xFF) + ((((int) b[2]) & 0xFF) << 8) + ((((int) b[1]) & 0xFF) << 16)
        + ((((int) b[0]) & 0xFF) << 24);
  }

  
  public static String byteToHex(byte b) {
    // Returns hex String representation of byte b
    char hexDigit[] = {
       '0', '1', '2', '3', '4', '5', '6', '7',
       '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };
    char[] array = { hexDigit[(b >> 4) & 0x0f], hexDigit[b & 0x0f] };
    return new String(array);
 }
  public static char toHexChar(int i) {
    if ((0 <= i) && (i <= 9))
      return (char) ('0' + i);
    else
      return (char) ('a' + (i - 10));
  }

  public static void print(byte[] packet) {

    if (packet == null)
      return;

    int size = packet.length;
    for (int c = 0; c < size; c++){ 
        System.out.println("PRINT [" + c + "] \t hex : " +  byteToHex(packet[c]) + "\t ubyte : " + readUnsignedByte(packet[c]) + "\t size : " + size);
    
      }
  }
  
   
  /**
   * Assuming the @c bytes array contains the 4 bytes of an IEEE 754
   * floating point number in network byte order (big-endian), this
   * function returns the corresponding Java floating point number.
   * 
   * @param bytes An array of 4 bytes corresponding to the 4 bytes
   *              of an IEEE 754 floating point number in network
   *              byte order (big-endian).
   *              
   * @return The Java floating point number corresponding to the
   *         IEEE 754 floating poing number in @c bytes.
   */
  public static float bigEndianBytesToFloat( byte[] bytes ) {

      int bits = (bytes[0] & 0xFF) << 0  |
                 (bytes[1] & 0xFF) << 8  |
                 (bytes[2] & 0xFF) << 16 |
                 (bytes[3] & 0xFF) << 24;
      
      return( Float.intBitsToFloat(bits) );
  }
  /**
   * Extract one unsigned short from a big endian byte array.
   * 
   * @param buffer the buffer to extract the short from
   * @param index the first byte to be interpreted as part of the short
   * @return The unsigned short at the given index in the buffer
   */
  public static int unsignedShortToInt(byte[] buffer, int index) {
    int r = (buffer[index] & 0xFF) << 8;
    r |= buffer[index + 1] & 0xFF;
    return r;
  }

  /**
   * Extract one unsigned short from a little endian byte array.
   * 
   * @param buffer the buffer to extract the short from
   * @param index the first byte to be interpreted as part of the short
   * @return The unsigned short at the given index in the buffer
   */
  public static int unsignedShortToIntLittleEndian(byte[] buffer, int index) {
    int r = buffer[index] & 0xFF;
    r |= (buffer[index + 1] & 0xFF) << 8;
    return r;
  }

  /**
   * Returns CRC8 (polynomial 0x8C) from byte array buffer[start] to
   * (excluding) buffer[start + length]
   * 
   * @param buffer the byte array of data (payload)
   * @param start the position in the byte array where the payload begins
   * @param length the length
   * @return CRC8 value
   */
  public static byte getCrc8(byte[] buffer, int start, int length) {
    byte crc = 0x0;

    for (int i = start; i < (start + length); i++) {
      crc = crc8PushByte(crc, buffer[i]);
    }
    return crc;
  }

  /**
   * Updates a CRC8 value by using the next byte passed to this method
   * 
   * @param crc int of crc value
   * @param add the next byte to add to the CRC8 calculation
   */
  private static byte crc8PushByte(byte crc, byte add) {
    crc = (byte) (crc ^ add);
    
    for (int i = 0; i < 8; i++) {
      if ((crc & 0x1) != 0x0) {
	// Using a 0xFF bit assures that 0-bits are introduced during the shift operation. 
	// Otherwise, implicit casts to signed int could shift in 1-bits if the signed bit is 1.
        crc = (byte) (((crc & 0xFF) >> 1) ^ 0x8C);
      } else {
        crc = (byte) ((crc & 0xFF) >> 1);
      }
    }
    return crc;
  }
	
  public static String getStateAsString(Sensor.SensorState state, Context c) {
    switch (state) {
      case NONE:
        return c.getString(R.string.value_none);
      case CONNECTING:
        return c.getString(R.string.sensor_state_connecting);
      case CONNECTED:
        return c.getString(R.string.sensor_state_connected);
      case DISCONNECTED:
        return c.getString(R.string.sensor_state_disconnected);
      case SENDING:
        return c.getString(R.string.sensor_state_sending);
      default:
        return "";
    }
  }
}
