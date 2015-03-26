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
package com.google.android.apps.mytracks.io.file.exporter;

import android.content.Context;
import android.location.Location;

import com.google.android.apps.mytracks.content.MyTracksLocation;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Sensor.SensorData;
import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.io.file.TrackFileFormat;
import com.google.android.apps.mytracks.stats.DoubleBuffer;
import com.google.android.apps.mytracks.util.StringUtils;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Locale;

import br.com.bioscada.apps.mytracks.R;

/**
 * Write track as CSV to a file. See RFC 4180 for info on CSV. Output three
 * tables.<br>
 * The first table contains the track info. Its columns are:<br>
 * "Track name","Activity type","Track description" <br>
 * <br>
 * The second table contains the markers. Its columns are:<br>
 * "Marker name","Marker type","Marker description","Latitude (deg)","Longitude
 * (deg)","Altitude (m)","Bearing (deg)","Accuracy (m)","Speed (m/s)","Time"<br>
 * <br>
 * The thrid table contains the points. Its columns are:<br>
 * "Segment","Point","Latitude (deg)","Longitude (deg)","Altitude (m)","Bearing
 * (deg)","Accuracy (m)","Speed (m/s)","Time","Power (W)","Cadence (rpm)","Heart
 * rate (bpm)","Battery level (%)"
 *
 * @author Rodrigo Damazio
 */
public class CsvTrackWriter implements TrackWriter {

    private static final NumberFormat SHORT_FORMAT = NumberFormat.getInstance(Locale.US);

    static {
        SHORT_FORMAT.setMaximumFractionDigits(4);
    }

    private final Context context;
    private PrintWriter printWriter;
    private int segmentIndex;
    private int pointIndex;
    private String l_heartRate = "";
    private String l_heartRateRC1 = "";
    private String l_heartRateRC2 = "";

    public CsvTrackWriter(Context context) {
        this.context = context;
    }

    @Override
    public String getExtension() {
        return TrackFileFormat.CSV.getExtension();
    }

    @Override
    public void prepare(OutputStream outputStream) {
        printWriter = new PrintWriter(outputStream);
        segmentIndex = 0;
        pointIndex = 0;
    }

    @Override
    public void close() {
        if (printWriter != null) {
            printWriter.flush();
            printWriter = null;
        }
    }

    @Override
    public void writeHeader(Track[] tracks) {
        writeCommaSeparatedLine(context.getString(R.string.generic_name),
                context.getString(R.string.track_edit_activity_type_hint),
                context.getString(R.string.generic_description));
        Track track = tracks[0];
        writeCommaSeparatedLine(track.getName(), track.getCategory(), track.getDescription());
        writeCommaSeparatedLine();
    }

    @Override
    public void writeFooter() {
        // Do nothing
    }

    @Override
    public void writeBeginWaypoints(Track track) {
        writeCommaSeparatedLine(context.getString(R.string.generic_name),
                context.getString(R.string.marker_edit_marker_type_hint),
                context.getString(R.string.generic_description),
                context.getString(R.string.description_location_latitude),
                context.getString(R.string.description_location_longitude),
                context.getString(R.string.description_location_altitude),
                context.getString(R.string.description_location_bearing),
                context.getString(R.string.description_location_accuracy),
                context.getString(R.string.description_location_speed),
                context.getString(R.string.description_time));
    }

    @Override
    public void writeEndWaypoints() {
        writeCommaSeparatedLine();
    }

    @Override
    public void writeWaypoint(Waypoint waypoint) {
        Location location = waypoint.getLocation();
        writeCommaSeparatedLine(waypoint.getName(), waypoint.getCategory(), waypoint.getDescription(),
                Integer.toString(location.getLatitude()), Integer.toString(location.getLongitude()),
                getAltitude(location), getBearing(location), getAccuracy(location), getSpeed(location),
                StringUtils.formatDateTimeIso8601(location.getTime()));
    }

    @Override
    public void writeBeginTracks() {
        // Do nothing
    }

    @Override
    public void writeEndTracks() {
        // Do nothing
    }

    @Override
    public void writeBeginTrack(Track track, Location startLocation) {
        writeCommaSeparatedLine(context.getString(R.string.description_track_segment),
                context.getString(R.string.description_track_point),
                context.getString(R.string.description_location_latitude),
                context.getString(R.string.description_location_longitude),
                context.getString(R.string.description_location_altitude),
                context.getString(R.string.description_location_bearing),
                context.getString(R.string.description_location_accuracy),
                context.getString(R.string.description_location_speed),
                context.getString(R.string.description_time),
                context.getString(R.string.description_sensor_power),
                context.getString(R.string.description_sensor_cadence),
                context.getString(R.string.description_sensor_heart_rate),
                context.getString(R.string.description_sensor_heart_rate_bpm),
                context.getString(R.string.description_sensor_heart_rate_rmssd),
                context.getString(R.string.description_sensor_attention),
                context.getString(R.string.description_sensor_meditation));
    }

    @Override
    public void writeEndTrack(Track track, Location endLocation) {
        // Do nothing
    }

    @Override
    public void writeOpenSegment() {
        segmentIndex++;
        pointIndex = 0;
    }

    @Override
    public void writeCloseSegment() {
        // Do nothing
    }

    @Override
    public void writeLocation(Location location) {
        String power = "";
        String cadence = "";
        String heartRate = "";
        String heartRateRC1 = "";
        String heartRateRC2 = "";
        String bpm = "";
        String rmssd = "";
        String attention = "";
        String meditation = "";
        if (location instanceof MyTracksLocation) {
            SensorDataSet sensorDataSet = ((MyTracksLocation) location).getSensorDataSet();

            if (sensorDataSet != null) {
                if (sensorDataSet.hasPower()) {
                    SensorData sensorData = sensorDataSet.getPower();
                    if (sensorData.hasValue() && sensorData.getState() == Sensor.SensorState.SENDING) {
                        power = Integer.toString(sensorData.getValue());
                    }
                }
                if (sensorDataSet.hasCadence()) {
                    SensorData sensorData = sensorDataSet.getCadence();
                    if (sensorData.hasValue() && sensorData.getState() == Sensor.SensorState.SENDING) {
                        cadence = Integer.toString(sensorData.getValue());
                    }
                }
                if (sensorDataSet.hasHeartRate()) {
                    SensorData sensorData = sensorDataSet.getHeartRate();
                    if (sensorData.hasValue() && sensorData.getState() == Sensor.SensorState.SENDING) {
                        if( !heartRate.equals(l_heartRate)  ){
                            heartRate = Integer.toString(sensorData.getValue());
                            l_heartRate = heartRate;
                        }else{
                            heartRate = "";
                        }


                    }
                }
                if (sensorDataSet.hasHeartRateRc1()) {
                    SensorData sensorData = sensorDataSet.getHeartRateRc1();
                    if (sensorData.hasValue() && sensorData.getState() == Sensor.SensorState.SENDING) {
                        heartRateRC1 = Integer.toString(sensorData.getValue());
                        if(!heartRateRC1.equals(l_heartRateRC1)){
                            l_heartRateRC1 = heartRateRC1;
                        }else{
                            heartRateRC1 = "";
                        }


                    }
                }
                if (sensorDataSet.hasHeartRateRc2()) {
                    SensorData sensorData = sensorDataSet.getHeartRateRc2();
                    if (sensorData.hasValue() && sensorData.getState() == Sensor.SensorState.SENDING) {
                        if(!heartRateRC2.equals(l_heartRateRC2)){
                            heartRateRC2 = Integer.toString(sensorData.getValue());
                            l_heartRateRC2 = heartRateRC2;
                        }else{
                            heartRateRC2 = "";
                        }


                    }
                }
                if (sensorDataSet.hasBpm()) {
                    SensorData sensorData = sensorDataSet.getBpm();
                    if (sensorData.hasValue() && sensorData.getState() == Sensor.SensorState.SENDING) {
                        bpm = Integer.toString(sensorData.getValue());
                    }
                }
                if (sensorDataSet.hasRmssd()) {
                    SensorData sensorData = sensorDataSet.getRmssd();
                    if (sensorData.hasValue() && sensorData.getState() == Sensor.SensorState.SENDING) {
                        rmssd = Integer.toString(sensorData.getValue());
                    }
                }
                if (sensorDataSet.hasAttention()) {
                    SensorData sensorData = sensorDataSet.getAttention();
                    if (sensorData.hasValue() && sensorData.getState() == Sensor.SensorState.SENDING) {
                        attention = Integer.toString(sensorData.getValue());
                    }
                }
                if (sensorDataSet.hasMeditation()) {
                    SensorData sensorData = sensorDataSet.getMeditation();
                    if (sensorData.hasValue() && sensorData.getState() == Sensor.SensorState.SENDING) {
                        meditation = Integer.toString(sensorData.getValue());
                    }
                }
            }
        }
        pointIndex++;
        writeCommaSeparatedLine(
                Integer.toString(segmentIndex),
                Integer.toString(pointIndex),
                Double.toString(location.getLatitude()),
                Double.toString(location.getLongitude()),
                getAltitude(location),
                getBearing(location),
                getAccuracy(location),
                getSpeed(location),
                StringUtils.formatDateTimeIso8601(location.getTime()),
                power,
                cadence,
                heartRate,
                bpm,
                rmssd,
                attention,
                meditation);

        if(heartRateRC1 !=  l_heartRateRC1){

            writeCommaSeparatedLine(
                    Integer.toString(segmentIndex),
                    Integer.toString(pointIndex),
                    Integer.toString(location.getLatitude()),
                    Integer.toString(location.getLongitude()),
                    getAltitude(location),
                    getBearing(location),
                    getAccuracy(location),
                    getSpeed(location),
                    StringUtils.formatDateTimeIso8601(location.getTime()),
                    power,
                    cadence,
                    heartRateRC1,
                    bpm,
                    rmssd,
                    attention,
                    meditation);
        }
        if(heartRateRC2 !=  l_heartRateRC2){

            writeCommaSeparatedLine(
                    Integer.toString(segmentIndex),
                    Integer.toString(pointIndex),
                    Integer.toString(location.getLatitude()),
                    Integer.toString(location.getLongitude()),
                    getAltitude(location),
                    getBearing(location),
                    getAccuracy(location),
                    getSpeed(location),
                    StringUtils.formatDateTimeIso8601(location.getTime()),
                    power,
                    cadence,
                    heartRateRC2,
                    bpm,
                    rmssd,
                    attention,
                    meditation);
        }

    }

    private String getAltitude(Location location) {
        return location.hasAltitude() ? Integer.toString(location.getAltitude()) : null;
    }

    private String getBearing(Location location) {
        return location.hasBearing() ? Integer.toString(location.getBearing()) : null;
    }

    private String getAccuracy(Location location) {
        return location.hasAccuracy() ? SHORT_FORMAT.format(location.getAccuracy()) : null;
    }

    private String getSpeed(Location location) {
        return location.hasSpeed() ? SHORT_FORMAT.format(location.getSpeed()) : null;
    }

    /**
     * Writes a single line of a CSV file.
     *
     * @param values the values to be written as CSV
     */
    private void writeCommaSeparatedLine(String... values) {
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for (String value : values) {
            if (!isFirst) {
                builder.append(',');
            }
            isFirst = false;

            builder.append('"');
            if (value != null) {
                builder.append(value.replaceAll("\"", "\"\""));
            }
            builder.append('"');
        }
        printWriter.println(builder.toString());
    }
}
