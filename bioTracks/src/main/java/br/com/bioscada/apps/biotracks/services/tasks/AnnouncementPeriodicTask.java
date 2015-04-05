/*
 * Copyright 2009 Google Inc.
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

package br.com.bioscada.apps.biotracks.services.tasks;

import android.content.Context;
import android.location.Location;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.lib.mytracks.content.Sensor;
import com.google.android.lib.mytracks.content.Waypoint;
import com.google.android.lib.mytracks.stats.TripStatistics;
import com.google.common.annotations.VisibleForTesting;

import java.util.HashMap;
import java.util.Locale;

import br.com.bioscada.apps.biotracks.R;
import br.com.bioscada.apps.biotracks.services.TrackRecordingService;
import br.com.bioscada.apps.biotracks.util.PreferencesUtils;
import br.com.bioscada.apps.biotracks.util.StringUtils;
import br.com.bioscada.apps.biotracks.util.UnitConversions;

/**
 * This class will periodically announce the user's trip statistics.
 * 
 * @author Sandor Dornbush
 */
public class AnnouncementPeriodicTask implements PeriodicTask {

  /**
   * The rate at which announcements are spoken.
   */
  @VisibleForTesting
  static final float TTS_SPEECH_RATE = 0.9f;

  private static final String TAG = AnnouncementPeriodicTask.class.getSimpleName();
  
  @VisibleForTesting
  static final HashMap<String, String> SPEECH_PARAMS = new HashMap<String, String>();
  static {
    SPEECH_PARAMS.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "not_used");
  }

  private final OnUtteranceCompletedListener
      utteranceListener = new OnUtteranceCompletedListener() {
          @Override
        public void onUtteranceCompleted(String utteranceId) {
          int result = audioManager.abandonAudioFocus(null);
          if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            Log.w(TAG, "Failed to relinquish audio focus.");
          }
        }
      };

  private final Context context;
  private TextToSpeech tts;

  // Response from TTS after its initialization
  private int initStatus = TextToSpeech.ERROR;

  // True if TTS engine is ready
  private boolean ready = false;

  // True if speech is allowed
  private boolean speechAllowed;

  private final AudioManager audioManager;
  
  private String[] params = new String[0];

  /**
   * Listener which updates {@link #speechAllowed} when the phone state changes.
   */
  private final PhoneStateListener phoneStateListener = new PhoneStateListener() {
      @Override
    public void onCallStateChanged(int state, String incomingNumber) {
      speechAllowed = state == TelephonyManager.CALL_STATE_IDLE;
      if (!speechAllowed && tts != null && tts.isSpeaking()) {
        // If we're already speaking, stop it.
        tts.stop();
      }
    }
  };

  public AnnouncementPeriodicTask(Context context) {
    this.context = context;
    audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
  }

  @Override
  public void start() {
    if (tts == null) {
      tts = newTextToSpeech(context, new OnInitListener() {
          @Override
        public void onInit(int status) {
          initStatus = status;
        }
      });
    }
    speechAllowed = true;
    listenToPhoneState(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
  }

  @Override
  public void run(TrackRecordingService trackRecordingService) {
    if (trackRecordingService == null) {
      Log.e(TAG, "TrackRecordingService is null.");
      return;
    }
    announce(trackRecordingService);
  }

  /**
   * Runs this task.
   * 
   * @param tripStatistics the trip statistics
   */
  @VisibleForTesting
  void announce(TrackRecordingService trackRecordingService) {
    if (trackRecordingService == null) {
      Log.e(TAG, "trackRecordingService is null.");
      return;
    }

    synchronized (this) {
      if (!ready) {
        ready = initStatus == TextToSpeech.SUCCESS;
        if (ready) {
          onTtsReady();
        }
      }
      if (!ready) {
        Log.i(TAG, "TTS not ready.");
        return;
      }
    }

    if (!speechAllowed) {
      Log.i(TAG, "Speech is not allowed at this time.");
      return;
    }
    speakAnnouncement(getAnnouncement(trackRecordingService));
  }

  @Override
  public void shutdown() {
    listenToPhoneState(phoneStateListener, PhoneStateListener.LISTEN_NONE);
    if (tts != null) {
      tts.shutdown();
      tts = null;
    }
  }

  @Override
  public void setParams(String[] params) {
    this.params = params;
  }

  /**
   * Called when TTS is ready.
   */
  private void onTtsReady() {
    Locale locale = Locale.getDefault();
    int languageAvailability = tts.isLanguageAvailable(locale);
    if (languageAvailability == TextToSpeech.LANG_MISSING_DATA
        || languageAvailability == TextToSpeech.LANG_NOT_SUPPORTED) {
      Log.w(TAG, "Default locale not available, use English.");
      locale = Locale.ENGLISH;
      /*
       * TODO: instead of using english, load the language if missing and show a
       * toast if not supported. Not able to change the resource strings to
       * English.
       */
    }
    tts.setLanguage(locale);

    // Slow down the speed just a bit as it is hard to hear when exercising.
    tts.setSpeechRate(TTS_SPEECH_RATE);

    tts.setOnUtteranceCompletedListener(utteranceListener);
  }

  /**
   * Speaks the announcement.
   * 
   * @param announcement the announcement
   */
  private void speakAnnouncement(String announcement) {
    int result = audioManager.requestAudioFocus(
        null, TextToSpeech.Engine.DEFAULT_STREAM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
    if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
      Log.w(TAG, "Failed to request audio focus.");
    }

    /*
     * We don't care about the utterance id. It is supplied here to force
     * onUtteranceCompleted to be called.
     */
    tts.speak(announcement, TextToSpeech.QUEUE_FLUSH, SPEECH_PARAMS);
  }

  /**
   * Create a new {@link TextToSpeech}.
   * 
   * @param aContext a context
   * @param onInitListener an on init listener
   */
  @VisibleForTesting
  protected TextToSpeech newTextToSpeech(Context aContext, OnInitListener onInitListener) {
    return new TextToSpeech(aContext, onInitListener);
  }

  /**
   * Gets the announcement.
   * 
   * @param tripStatistics the trip statistics
   */
  @VisibleForTesting
  protected String getAnnouncement(TrackRecordingService trackRecordingService) {
    TripStatistics tripStatistics = trackRecordingService.getTripStatistics();
    Sensor.SensorDataSet sensorDataSet = trackRecordingService.getSensorDataSet();
    Location lastLocation = trackRecordingService.getLastLocation();
    Waypoint lastSplit = trackRecordingService.getLastSplitWaypoint();
    boolean metricUnits = PreferencesUtils.isMetricUnits(context);

    StringBuffer buf = new StringBuffer();

    for (String param: params) {
      
      if (param.equals(context.getString(R.string.voice_type_appname_key))) {
        buf.append(context.getString(R.string.my_tracks_app_name));
        buf.append(". ");
      }
      
      if (param.equals(context.getString(R.string.voice_type_totaldist_key))) {
        double distance = tripStatistics.getTotalDistance() * UnitConversions.M_TO_KM;
        if (!metricUnits) {
          distance *= UnitConversions.KM_TO_MI;
        }
        int totalDistanceId = metricUnits ? R.plurals.voiceTotalDistanceKilometers
            : R.plurals.voiceTotalDistanceMiles;
        buf.append(context.getResources()
            .getQuantityString(totalDistanceId, getQuantityCount(distance), distance));
        buf.append(". ");
      }
      
      if (param.equals(context.getString(R.string.voice_type_totaltime_key))) {
        double time = System.currentTimeMillis() - tripStatistics.getStartTime();
        buf.append(context.getString(R.string.voice_total_time, getAnnounceTime(Math.round(time))));
        buf.append(". ");
      }
  
      if (param.equals(context.getString(R.string.voice_type_movingtime_key))) {
        double time = tripStatistics.getMovingTime();
        buf.append(context.getString(R.string.voice_moving_time, getAnnounceTime(Math.round(time))));
        buf.append(". ");
      }
  
      if (param.equals(context.getString(R.string.voice_type_splittime_key))) {
        if (lastSplit != null) {
          double time = lastSplit.getTripStatistics().getTotalTime();
          buf.append(context.getString(R.string.voice_split_time, getAnnounceTime(Math.round(time))));
          buf.append(". ");
        }
      }
  
      if (param.equals(context.getString(R.string.voice_type_splitdist_key))) {
        if (lastSplit != null) {
          double distance = lastSplit.getTripStatistics().getTotalDistance() * UnitConversions.M_TO_KM;
          if (!metricUnits) {
            distance *= UnitConversions.KM_TO_MI;
          }
          int totalDistanceId = metricUnits ? R.plurals.voiceSplitDistanceKilometers
              : R.plurals.voiceSplitDistanceMiles;
          buf.append(context.getResources()
              .getQuantityString(totalDistanceId, getQuantityCount(distance), distance));
          buf.append(". ");
        }
      }
  
      if (param.equals(context.getString(R.string.voice_type_curpace_key))) {
        double speed = lastLocation.getSpeed();
        speed *= UnitConversions.MS_TO_KMH;
        if (!metricUnits) {
          speed *= UnitConversions.KM_TO_MI;
        }
        speed = speed == 0 ? 0.0 : 1 / speed;
        int paceId = metricUnits ? R.string.voice_pace_per_kilometer : R.string.voice_pace_per_mile;
        long time = Math.round(
            speed * UnitConversions.HR_TO_MIN * UnitConversions.MIN_TO_S * UnitConversions.S_TO_MS);
        String rate = context.getString(paceId, getAnnounceTime(time));
        buf.append(context.getString(R.string.voice_current_pace, rate));
        buf.append(". ");
      }
  
      if (param.equals(context.getString(R.string.voice_type_avgpace_key))) {
        double speed = tripStatistics.getAverageSpeed();
        speed *= UnitConversions.MS_TO_KMH;
        if (!metricUnits) {
          speed *= UnitConversions.KM_TO_MI;
        }
        speed = speed == 0 ? 0.0 : 1 / speed;
        int paceId = metricUnits ? R.string.voice_pace_per_kilometer : R.string.voice_pace_per_mile;
        long time = Math.round(
            speed * UnitConversions.HR_TO_MIN * UnitConversions.MIN_TO_S * UnitConversions.S_TO_MS);
        String rate = context.getString(paceId, getAnnounceTime(time));
        buf.append(context.getString(R.string.voice_average_pace, rate));
        buf.append(". ");
      }
  
      if (param.equals(context.getString(R.string.voice_type_avgmovingpace_key))) {
        double speed = tripStatistics.getAverageMovingSpeed();
        speed *= UnitConversions.MS_TO_KMH;
        if (!metricUnits) {
          speed *= UnitConversions.KM_TO_MI;
        }
        speed = speed == 0 ? 0.0 : 1 / speed;
        int paceId = metricUnits ? R.string.voice_pace_per_kilometer : R.string.voice_pace_per_mile;
        long time = Math.round(
            speed * UnitConversions.HR_TO_MIN * UnitConversions.MIN_TO_S * UnitConversions.S_TO_MS);
        String rate = context.getString(paceId, getAnnounceTime(time));
        buf.append(context.getString(R.string.voice_average_moving_pace, rate));
        buf.append(". ");
      }
  
      if (param.equals(context.getString(R.string.voice_type_curspeed_key))) {
        double speed = lastLocation.getSpeed();
        speed *= UnitConversions.MS_TO_KMH;
        if (!metricUnits) {
          speed *= UnitConversions.KM_TO_MI;
        }
        int speedId = metricUnits ? R.plurals.voiceSpeedKilometersPerHour
            : R.plurals.voiceSpeedMilesPerHour;
        String rate = context.getResources().getQuantityString(speedId, getQuantityCount(speed), speed);
        buf.append(context.getString(R.string.voice_current_speed, rate));
        buf.append(". ");
      }
  
      if (param.equals(context.getString(R.string.voice_type_avgspeed_key))) {
        double speed = tripStatistics.getAverageSpeed();
        speed *= UnitConversions.MS_TO_KMH;
        if (!metricUnits) {
          speed *= UnitConversions.KM_TO_MI;
        }
        int speedId = metricUnits ? R.plurals.voiceSpeedKilometersPerHour
            : R.plurals.voiceSpeedMilesPerHour;
        String rate = context.getResources().getQuantityString(speedId, getQuantityCount(speed), speed);
        buf.append(context.getString(R.string.voice_average_speed, rate));
        buf.append(". ");
      }
  
      if (param.equals(context.getString(R.string.voice_type_avgmovingspeed_key))) {
        double speed = tripStatistics.getAverageMovingSpeed();
        speed *= UnitConversions.MS_TO_KMH;
        if (!metricUnits) {
          speed *= UnitConversions.KM_TO_MI;
        }
        int speedId = metricUnits ? R.plurals.voiceSpeedKilometersPerHour
            : R.plurals.voiceSpeedMilesPerHour;
        String rate = context.getResources().getQuantityString(speedId, getQuantityCount(speed), speed);
        buf.append(context.getString(R.string.voice_average_moving_speed, rate));
        buf.append(". ");
      }
      
      if (param.equals(context.getString(R.string.voice_type_calories_key))) {
        long calories = Math.round(tripStatistics.getCalorie());
        buf.append(context.getString(R.string.voice_calories, calories));
        buf.append(". ");
      }
  
      if (param.equals(context.getString(R.string.voice_type_heartrate_key))) {
        if (sensorDataSet != null) {
          Sensor.SensorData heartrate = sensorDataSet.getHeartRate();
          if (heartrate != null) {
            buf.append(context.getString(R.string.voice_heart_rate, heartrate.getValue()));
            buf.append(". ");
          }
        }
      }
  
      if (param.equals(context.getString(R.string.voice_type_cadence_key))) {
        if (sensorDataSet != null) {
          Sensor.SensorData cadence = sensorDataSet.getCadence();
          if (cadence != null) {
            buf.append(context.getString(R.string.voice_cadence, cadence.getValue()));
            buf.append(". ");
          }
        }
      }
  
      if (param.equals(context.getString(R.string.voice_type_power_key))) {
        if (sensorDataSet != null) {
          Sensor.SensorData power = sensorDataSet.getPower();
          if (power != null) {
            buf.append(context.getString(R.string.voice_power, power.getValue()));
            buf.append(". ");
          }
        }
      }
      
    }

    return buf.toString();
  }

  /**
   * Listens to phone state.
   * 
   * @param listener the listener
   * @param events the interested events
   */
  @VisibleForTesting
  protected void listenToPhoneState(PhoneStateListener listener, int events) {
    TelephonyManager telephony = (TelephonyManager) context.getSystemService(
        Context.TELEPHONY_SERVICE);
    if (telephony != null) {
      telephony.listen(listener, events);
    }
  }

  /**
   * Gets the announce time.
   * 
   * @param time the time
   */
  @VisibleForTesting
  String getAnnounceTime(long time) {
    int[] parts = StringUtils.getTimeParts(time);
    String seconds = context.getResources()
        .getQuantityString(R.plurals.voiceSeconds, parts[0], parts[0]);
    String minutes = context.getResources()
        .getQuantityString(R.plurals.voiceMinutes, parts[1], parts[1]);
    String hours = context.getResources()
        .getQuantityString(R.plurals.voiceHours, parts[2], parts[2]);
    StringBuilder sb = new StringBuilder();
    if (parts[2] != 0) {
      sb.append(hours);
      sb.append(" ");
      sb.append(minutes);
      sb.append(" ");
      sb.append(seconds);
    } else {
      sb.append(minutes);
      sb.append(" ");
      sb.append(seconds);
    }
    return sb.toString();
  }

  /**
   * Gets the plural count to be used by getQuantityString. getQuantityString
   * only supports integer quantities, not a double quantity like "2.2".
   * <p>
   * As a temporary workaround, we convert a double quantity to an integer
   * quantity. If the double quantity is exactly 0, 1, or 2, then we can return
   * these integer quantities. Otherwise, we cast the double quantity to an
   * integer quantity. However, we need to make sure that if the casted value is
   * 0, 1, or 2, we don't return those, instead, return the next biggest integer
   * 3.
   * 
   * @param d the double value
   */
  private int getQuantityCount(double d) {
    if (d == 0) {
      return 0;
    } else if (d == 1) {
      return 1;
    } else if (d == 2) {
      return 2;
    } else {
      int count = (int) d;
      return count < 3 ? 3 : count;
    }
  }
}
