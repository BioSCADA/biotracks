/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.com.bioscada.apps.biotracks.services.sensors.btle;


/**
 * This class includes a small subset of standard GATT attributes.
 * It mimic the Linux kernel header found here:
 * https://kernel.googlesource.com/pub/scm/bluetooth/bluez/+/5.7/lib/uuid.h
 */
public class UUID {
  // Heart Rate Monitor
  public static String HEART_RATE_UUID               =  "0000180d-0000-1000-8000-00805f9b34fb";
  public static String HEART_RATE_MEASUREMENT_UUID   =  "00002a37-0000-1000-8000-00805f9b34fb";
  public static String BODY_SENSOR_LOCATION_UUID     =  "00002a38-0000-1000-8000-00805f9b34fb";
  public static String HEART_RATE_CONTROL_POINT_UUID =  "00002a39-0000-1000-8000-00805f9b34fb";

  // Cycling Speed & Cadence
  public static String CYCLING_SC_UUID       =  "00001816-0000-1000-8000-00805f9b34fb";
  public static String CSC_MEASUREMENT_UUID  =  "00002a5b-0000-1000-8000-00805f9b34fb";
  public static String CSC_FEATURE_UUID      =  "00002a5c-0000-1000-8000-00805f9b34fb";
  public static String SENSOR_LOCATION_UUID  =  "00002a5d-0000-1000-8000-00805f9b34fb";
  public static String SC_CONTROL_POINT_UUID =  "00002a55-0000-1000-8000-00805f9b34fb";
}
