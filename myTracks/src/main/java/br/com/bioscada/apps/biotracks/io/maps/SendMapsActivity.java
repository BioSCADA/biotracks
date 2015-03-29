/*
 * Copyright 2012 Google Inc.
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
package br.com.bioscada.apps.biotracks.io.maps;

import android.content.Intent;

import com.google.common.annotations.VisibleForTesting;

import br.com.bioscada.apps.biotracks.R;
import br.com.bioscada.apps.biotracks.io.fusiontables.SendFusionTablesActivity;
import br.com.bioscada.apps.biotracks.io.sendtogoogle.AbstractSendActivity;
import br.com.bioscada.apps.biotracks.io.sendtogoogle.AbstractSendAsyncTask;
import br.com.bioscada.apps.biotracks.io.sendtogoogle.SendRequest;
import br.com.bioscada.apps.biotracks.io.sendtogoogle.UploadResultActivity;
import br.com.bioscada.apps.biotracks.io.spreadsheets.SendSpreadsheetsActivity;
import br.com.bioscada.apps.biotracks.util.IntentUtils;

/**
 * An activity to send a track to Google Maps.
 *
 * @author Jimmy Shih
 */
public class SendMapsActivity extends AbstractSendActivity {

  @Override
  protected AbstractSendAsyncTask createAsyncTask() {
    return new SendMapsAsyncTask(this, sendRequest.getTrackId(), sendRequest.getAccount());
  }

  @Override
  protected String getServiceName() {
    return getString(R.string.export_google_maps);
  }

  @Override
  protected void startNextActivity(boolean success, boolean isCancel) {
    sendRequest.setMapsSuccess(success);
    Class<?> next = getNextClass(sendRequest, isCancel);
    Intent intent = IntentUtils.newIntent(this, next)
        .putExtra(SendRequest.SEND_REQUEST_KEY, sendRequest);
    startActivity(intent);
    finish();
  }
  
  @VisibleForTesting
  Class<?> getNextClass(SendRequest request, boolean isCancel) {
    if (isCancel) {
      return UploadResultActivity.class;
    } else {
      if (request.isSendFusionTables()) {
        return SendFusionTablesActivity.class;
      } else if (request.isSendSpreadsheets()) {
        return SendSpreadsheetsActivity.class;
      } else {
        return UploadResultActivity.class;
      }
    }
  }
}
