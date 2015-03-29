/*
 * Copyright 2008 Google Inc.
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

package br.com.bioscada.apps.biotracks;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.lib.mytracks.content.MyTracksProviderUtils;
import com.google.android.lib.mytracks.content.Track;

import br.com.bioscada.apps.biotracks.fragments.ChooseAccountDialogFragment;
import br.com.bioscada.apps.biotracks.fragments.ChooseAccountDialogFragment.ChooseAccountCaller;
import br.com.bioscada.apps.biotracks.fragments.ChooseActivityTypeDialogFragment;
import br.com.bioscada.apps.biotracks.fragments.ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller;
import br.com.bioscada.apps.biotracks.fragments.EnableSyncDialogFragment;
import br.com.bioscada.apps.biotracks.fragments.EnableSyncDialogFragment.EnableSyncCaller;
import br.com.bioscada.apps.biotracks.io.sendtogoogle.SendToGoogleUtils;
import br.com.bioscada.apps.biotracks.io.sync.SyncUtils;
import br.com.bioscada.apps.biotracks.services.TrackRecordingServiceConnection;
import br.com.bioscada.apps.biotracks.services.tasks.CheckPermissionAsyncTask;
import br.com.bioscada.apps.biotracks.services.tasks.CheckPermissionAsyncTask.CheckPermissionCaller;
import br.com.bioscada.apps.biotracks.util.EulaUtils;
import br.com.bioscada.apps.biotracks.util.PreferencesUtils;
import br.com.bioscada.apps.biotracks.util.TrackIconUtils;
import br.com.bioscada.apps.biotracks.util.TrackRecordingServiceConnectionUtils;
import br.com.bioscada.apps.biotracks.util.TrackUtils;

/**
 * An activity that let's the user see and edit the user editable track meta
 * data such as track name, activity type, and track description.
 * 
 * @author Leif Hendrik Wilden
 */
public class TrackEditActivity extends AbstractMyTracksActivity implements ChooseActivityTypeCaller,
    EnableSyncCaller, ChooseAccountCaller, CheckPermissionCaller {

  public static final String EXTRA_TRACK_ID = "track_id";
  public static final String EXTRA_NEW_TRACK = "new_track";

  private static final String TAG = TrackEditActivity.class.getSimpleName();
  private static final String ICON_VALUE_KEY = "icon_value_key";

  private static final int DRIVE_REQUEST_CODE = 0;

  private Long trackId;
  private TrackRecordingServiceConnection trackRecordingServiceConnection;
  private MyTracksProviderUtils myTracksProviderUtils;
  private Track track;
  private String iconValue;

  private EditText name;
  private AutoCompleteTextView activityType;
  private Spinner activityTypeIcon;
  private EditText description;

  private boolean newWeight = false;

  private CheckPermissionAsyncTask syncDriveAsyncTask;

  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    Object retained = getLastCustomNonConfigurationInstance();
    if (retained instanceof CheckPermissionAsyncTask) {
      syncDriveAsyncTask = (CheckPermissionAsyncTask) retained;
      syncDriveAsyncTask.setActivity(this);
    }

    trackRecordingServiceConnection = new TrackRecordingServiceConnection(this, null);
    trackId = getIntent().getLongExtra(EXTRA_TRACK_ID, -1L);
    if (trackId == -1L) {
      Log.e(TAG, "invalid trackId");
      finish();
      return;
    }

    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(this);
    track = myTracksProviderUtils.getTrack(trackId);
    if (track == null) {
      Log.e(TAG, "No track for " + trackId);
      finish();
      return;
    }

    name = (EditText) findViewById(R.id.track_edit_name);
    name.setText(track.getName());

    activityType = (AutoCompleteTextView) findViewById(R.id.track_edit_activity_type);
    activityType.setText(track.getCategory());

    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
        this, R.array.activity_types, android.R.layout.simple_dropdown_item_1line);
    activityType.setAdapter(adapter);
    activityType.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        setActivityTypeIcon(TrackIconUtils.getIconValue(
            TrackEditActivity.this, (String) activityType.getAdapter().getItem(position)));
      }
    });
    activityType.setOnFocusChangeListener(new View.OnFocusChangeListener() {
        @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
          setActivityTypeIcon(TrackIconUtils.getIconValue(
              TrackEditActivity.this, activityType.getText().toString()));
        }
      }
    });

    iconValue = null;
    if (bundle != null) {
      iconValue = bundle.getString(ICON_VALUE_KEY);
    }
    if (iconValue == null) {
      iconValue = track.getIcon();
    }

    activityTypeIcon = (Spinner) findViewById(R.id.track_edit_activity_type_icon);
    activityTypeIcon.setAdapter(TrackIconUtils.getIconSpinnerAdapter(this, iconValue));
    activityTypeIcon.setOnTouchListener(new View.OnTouchListener() {
        @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
          ChooseActivityTypeDialogFragment.newInstance(activityType.getText().toString()).show(
              getSupportFragmentManager(),
              ChooseActivityTypeDialogFragment.CHOOSE_ACTIVITY_TYPE_DIALOG_TAG);
        }
        return true;
      }
    });
    activityTypeIcon.setOnKeyListener(new View.OnKeyListener() {
        @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
          ChooseActivityTypeDialogFragment.newInstance(activityType.getText().toString()).show(
              getSupportFragmentManager(),
              ChooseActivityTypeDialogFragment.CHOOSE_ACTIVITY_TYPE_DIALOG_TAG);
        }
        return true;
      }
    });

    description = (EditText) findViewById(R.id.track_edit_description);
    description.setText(track.getDescription());

    Button save = (Button) findViewById(R.id.track_edit_save);
    save.setOnClickListener(new View.OnClickListener() {
        @Override
      public void onClick(View v) {
        TrackUtils.updateTrack(TrackEditActivity.this, track, name.getText().toString(),
                activityType.getText().toString(), description.getText().toString(),
                myTracksProviderUtils, trackRecordingServiceConnection, newWeight);

        if (EulaUtils.showEnableSync(TrackEditActivity.this)) {
          EulaUtils.setShowEnableSync(TrackEditActivity.this);
          if (PreferencesUtils.getBoolean(TrackEditActivity.this, R.string.drive_sync_key,
              PreferencesUtils.DRIVE_SYNC_DEFAULT)) {
            finish();
          } else {
            new EnableSyncDialogFragment().show(
                getSupportFragmentManager(), EnableSyncDialogFragment.ENABLE_SYNC_DIALOG_TAG);
          }
        } else {
          finish();
        }
      }
    });

    Button cancel = (Button) findViewById(R.id.track_edit_cancel);
    if (getIntent().getBooleanExtra(EXTRA_NEW_TRACK, false)) {
      setTitle(R.string.track_edit_new_track_title);
      cancel.setVisibility(View.GONE);
    } else {
      setTitle(R.string.menu_edit);
      cancel.setOnClickListener(new View.OnClickListener() {
          @Override
        public void onClick(View v) {
          finish();
        }
      });
      cancel.setVisibility(View.VISIBLE);
    }
  }

  @Override
  public Object onRetainCustomNonConfigurationInstance() {
    if (syncDriveAsyncTask != null) {
      syncDriveAsyncTask.setActivity(null);
    }
    return syncDriveAsyncTask;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == DRIVE_REQUEST_CODE) {
      SendToGoogleUtils.cancelNotification(this, SendToGoogleUtils.DRIVE_NOTIFICATION_ID);
      if (resultCode == Activity.RESULT_OK) {
        onDrivePermissionSuccess();
      } else {
        onPermissionFailure();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    TrackRecordingServiceConnectionUtils.startConnection(this, trackRecordingServiceConnection);
  }

  @Override
  protected void onStop() {
    super.onStop();
    trackRecordingServiceConnection.unbind();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(ICON_VALUE_KEY, iconValue);
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.track_edit;
  }

  private void setActivityTypeIcon(String value) {
    iconValue = value;
    TrackIconUtils.setIconSpinner(activityTypeIcon, value);
  }

  @Override
  public void onChooseActivityTypeDone(String value, boolean hasNewWeight) {
    if (!newWeight) {
      newWeight = hasNewWeight;
    }
    setActivityTypeIcon(value);
    activityType.setText(getString(TrackIconUtils.getIconActivityType(value)));
  }

  @Override
  public void onEnableSyncDone(boolean enable) {
    if (enable) {
      new ChooseAccountDialogFragment().show(
          getSupportFragmentManager(), ChooseAccountDialogFragment.CHOOSE_ACCOUNT_DIALOG_TAG);
    } else {
      finish();
    }
  }

  @Override
  public void onChooseAccountDone(String account) {
    PreferencesUtils.setString(this, R.string.google_account_key, account);
    if (PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT.equals(account)) {
      finish();
    } else {
      syncDriveAsyncTask = new CheckPermissionAsyncTask(this, account, SendToGoogleUtils.DRIVE_SCOPE);
      syncDriveAsyncTask.execute();
    }
  }

  @Override
  public void onCheckPermissionDone(String scope, boolean success, Intent userRecoverableIntent) {
    syncDriveAsyncTask = null;
    if (success) {
      onDrivePermissionSuccess();
    } else {
      if (userRecoverableIntent != null) {
        startActivityForResult(userRecoverableIntent, DRIVE_REQUEST_CODE);
      } else {
        onPermissionFailure();
      }
    }
  }

  private void onDrivePermissionSuccess() {
    SyncUtils.enableSync(this);
    finish();
  }

  private void onPermissionFailure() {
    Toast.makeText(this, R.string.send_google_no_account_permission, Toast.LENGTH_LONG).show();
    finish();
  }
}
