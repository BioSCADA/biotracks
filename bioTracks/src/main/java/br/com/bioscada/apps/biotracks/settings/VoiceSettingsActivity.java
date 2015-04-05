/*
 * Copyright 2012 Google Inc., 2014 Edwin Woudt
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

package br.com.bioscada.apps.biotracks.settings;

import android.os.Bundle;
import android.preference.ListPreference;

import br.com.bioscada.apps.biotracks.R;
import br.com.bioscada.apps.biotracks.util.PreferencesUtils;
import br.com.bioscada.apps.biotracks.util.StringUtils;

/**
 * An activity for accessing voice announcement settings.
 * 
 * @author Edwin Woudt
 */
public class VoiceSettingsActivity extends AbstractSettingsActivity {

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.voice_settings);

    boolean metricUnits = PreferencesUtils.isMetricUnits(this);

    configFrequencyPreference(R.string.voice_split_frequency_key,
        PreferencesUtils.VOICE_FREQUENCY_DEFAULT, R.array.frequency_values, metricUnits);
    configFrequencyPreference(R.string.voice_1_frequency_key,
        PreferencesUtils.VOICE_FREQUENCY_DEFAULT, R.array.frequency_values, metricUnits);
    configFrequencyPreference(R.string.voice_2_frequency_key,
        PreferencesUtils.VOICE_FREQUENCY_DEFAULT, R.array.frequency_values, metricUnits);
  }

  @SuppressWarnings("deprecation")
  private void configFrequencyPreference(
      int key, int defaultValue, int valueArray, boolean metricUnits) {
    ListPreference preference = (ListPreference) findPreference(getString(key));
    int value = PreferencesUtils.getInt(this, key, defaultValue);
    String[] values = getResources().getStringArray(valueArray);
    String[] options = StringUtils.getFrequencyOptions(this, metricUnits);
    configureListPreference(preference, options, options, values, String.valueOf(value), null);
  }

}
