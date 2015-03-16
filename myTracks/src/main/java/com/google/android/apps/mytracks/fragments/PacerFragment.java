 
package com.google.android.apps.mytracks.fragments;

import android.annotation.SuppressLint;
import android.location.Location;
import android.net.http.SslError;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ZoomControls;

import com.google.android.apps.mytracks.ChartView;
import com.google.android.apps.mytracks.content.MyTracksLocation;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackDataHub;
import com.google.android.apps.mytracks.content.TrackDataListener;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.stats.TripStatisticsUpdater;
import com.google.android.apps.mytracks.util.CalorieUtils.ActivityType;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;

import br.com.bioscada.apps.mytracks.R;

/**
 * A fragment to display track pacer to the user.
 * 
 * @author Marlon Moraes 
 */
public class PacerFragment  extends Fragment implements TrackDataListener {
    
  public static final String PACER_FRAGMENT_TAG = "pacerFragment";

  private boolean[] chartShow = new boolean[] { true, true, true, true, true, true };
  private boolean chartByDistance = true;
  
  private final ArrayList<double[]> pendingPoints = new ArrayList<double[]>();
 
  private void runOnUiThread(Runnable runnable) {
    FragmentActivity fragmentActivity = getActivity();
    if (fragmentActivity != null) {
      fragmentActivity.runOnUiThread(runnable);
    }
  }
  
  private long startTime;
  private TripStatisticsUpdater tripStatisticsUpdater;
  private boolean metricUnits = true;
  private boolean reportSpeed = true;
  private int recordingDistanceInterval = PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT;

  // UI elements
  private ChartView chartView;
  private WebView webview;
  private ZoomControls zoomControls;
  private synchronized boolean isSelectedTrackRecording() {
    return trackDataHub != null && trackDataHub.isSelectedTrackRecording();
  }
  
  /**
   * A runnable that will enable/disable zoom controls and orange pointer as
   * appropriate and redraw.
   */
  private final Runnable updateChart = new Runnable() {
      @Override
    public void run() {
      if (!isResumed() || trackDataHub == null) {
        return;
      }

      zoomControls.setIsZoomInEnabled(chartView.canZoomIn());
      zoomControls.setIsZoomOutEnabled(chartView.canZoomOut());
      chartView.setShowPointer(isSelectedTrackRecording());
      chartView.invalidate();
    }
  };
  
  // States from TrackDetailActivity, set in onResume
  private TrackDataHub trackDataHub;
      
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
       
  }
  
  @SuppressLint("SetJavaScriptEnabled") @Override
  public View onCreateView(LayoutInflater inflater,
          ViewGroup container,
          Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.pacer, container, false);
     webview=(WebView)view.findViewById(R.id.pacerView);
     webview.setWebViewClient(new MyWebViewClient());
     WebSettings settings = webview.getSettings();
     settings.setJavaScriptEnabled(true);
     settings.setDomStorageEnabled(true);
     openURL();
     return view;
     }
     
  /** Opens the URL in a browser */
  private void openURL() {
      //webview.loadUrl("https://bioscada.me/bsweb/pages/respirar/#/menu/home");
      webview.loadUrl("file:///android_asset/index.html");
      webview.requestFocus();
  }        
  
  private class MyWebViewClient extends WebViewClient {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        view.loadUrl(url);
        return true;
    }
    
    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
      super.onReceivedSslError(view, handler, error); 
      handler.proceed(); // Ignore SSL certificate errors
    }       
}
  
  @Override
  public void onTrackUpdated(Track track) {
    if (isResumed()) {
      if (track == null || track.getTripStatistics() == null) {
        startTime = -1L;
        return;
      }
      startTime = track.getTripStatistics().getStartTime();
    }
  }
    
  @Override
  public void clearTrackPoints() {
    if (isResumed()) {
      tripStatisticsUpdater = startTime != -1L ? new TripStatisticsUpdater(startTime) : null;
      pendingPoints.clear();
      chartView.reset();
      runOnUiThread(new Runnable() {
          @Override
        public void run() {
          if (isResumed()) {
            chartView.resetScroll();
          }
        }
      });
    } 
    }
   
 

  @Override
  public void onSampledInTrackPoint(Location location) { 
    if (isResumed()) {
      double[] data = new double[ChartView.NUM_SERIES + 1];
      fillDataPoint(location, data);
      pendingPoints.add(data);
    } 
  }

  @Override
  public void onSampledOutTrackPoint(Location location) { 
    if (isResumed()) {
      fillDataPoint(location, null);
    } 
  }

  @Override
  public void onSegmentSplit(Location location) { 
    if (isResumed()) {
      fillDataPoint(location, null);
    } 
  }

  @Override
  public void onNewTrackPointsDone() { 
    if (isResumed()) {
      chartView.addDataPoints(pendingPoints);
      pendingPoints.clear();
      runOnUiThread(updateChart);
    } 
  }

  @Override
  public void clearWaypoints() { 
    if (isResumed()) {
      chartView.clearWaypoints();
    } 
  }

  @Override
  public void onNewWaypoint(Waypoint waypoint) { 
    if (isResumed() && waypoint != null && LocationUtils.isValidLocation(waypoint.getLocation())) {
      chartView.addWaypoint(waypoint);
    } 
  }

  @Override
  public void onNewWaypointsDone() { 
    if (isResumed()) {
      runOnUiThread(updateChart);
    }
  }

  @Override
  public boolean onMetricUnitsChanged(boolean metric) {
    if (isResumed()) {
      if (metricUnits == metric) {
        return false;
      }
      metricUnits = metric;
      chartView.setMetricUnits(metricUnits);
      runOnUiThread(new Runnable() {
          @Override
        public void run() {
          if (isResumed()) {
            chartView.requestLayout();
          }
        }
      });
      return true;
    }
    return false;
  }
  
  private boolean setSeriesEnabled(int index, boolean value) {
    if (chartShow[index] != value) {
      chartShow[index] = value;
      chartView.setChartValueSeriesEnabled(index, value);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean onReportSpeedChanged(boolean speed) {
    if (isResumed()) {
      if (reportSpeed == speed) {
        return false;
      }
      reportSpeed = speed;
      chartView.setReportSpeed(reportSpeed);
      boolean chartShowSpeed = PreferencesUtils.getBoolean(
          getActivity(), R.string.chart_show_speed_key, PreferencesUtils.CHART_SHOW_SPEED_DEFAULT);
      setSeriesEnabled(ChartView.SPEED_SERIES, chartShowSpeed && reportSpeed);
      setSeriesEnabled(ChartView.PACE_SERIES, chartShowSpeed && !reportSpeed);
      runOnUiThread(new Runnable() {
          @Override
        public void run() {
          if (isResumed()) {
            chartView.requestLayout();
          }
        }
      });
      return true;
    }
    return false; 
  }
 
  
  @Override
  public boolean onRecordingDistanceIntervalChanged(int value) {
    if (isResumed()) {
      if (recordingDistanceInterval == value) {
        return false;
      }
      recordingDistanceInterval = value;
      return true;
    }
    return false;
  }
  
  @Override
  public boolean onMapTypeChanged(int mapType) {
    // We don't care.
    return false;
  }

  @VisibleForTesting
  void fillDataPoint(Location location, double data[]) {
    double timeOrDistance = Double.NaN;
    double elevation = Double.NaN;
    double speed = Double.NaN;
    double pace = Double.NaN;
    double heartRate = Double.NaN;
    double cadence = Double.NaN;
    double power = Double.NaN;

    if (tripStatisticsUpdater != null) {
      tripStatisticsUpdater.addLocation(
          location, recordingDistanceInterval, false, ActivityType.INVALID, 0.0);
      TripStatistics tripStatistics = tripStatisticsUpdater.getTripStatistics();
      if (chartByDistance) {
        double distance = tripStatistics.getTotalDistance() * UnitConversions.M_TO_KM;
        if (!metricUnits) {
          distance *= UnitConversions.KM_TO_MI;
        }
        timeOrDistance = distance;
      } else {
        timeOrDistance = tripStatistics.getTotalTime();
      }

      elevation = tripStatisticsUpdater.getSmoothedElevation();
      if (!metricUnits) {
        elevation *= UnitConversions.M_TO_FT;
      }

      speed = tripStatisticsUpdater.getSmoothedSpeed() * UnitConversions.MS_TO_KMH;
      if (!metricUnits) {
        speed *= UnitConversions.KM_TO_MI;
      }
      pace = speed == 0 ? 0.0 : 60.0 / speed;
    }
    if (location instanceof MyTracksLocation
        && ((MyTracksLocation) location).getSensorDataSet() != null) {
      SensorDataSet sensorDataSet = ((MyTracksLocation) location).getSensorDataSet();
      if (sensorDataSet.hasHeartRate()
          && sensorDataSet.getHeartRate().getState() == Sensor.SensorState.SENDING
          && sensorDataSet.getHeartRate().hasValue()) {
        heartRate = sensorDataSet.getHeartRate().getValue();
      }
      if (sensorDataSet.hasCadence()
          && sensorDataSet.getCadence().getState() == Sensor.SensorState.SENDING
          && sensorDataSet.getCadence().hasValue()) {
        cadence = sensorDataSet.getCadence().getValue();
      }
      if (sensorDataSet.hasPower()
          && sensorDataSet.getPower().getState() == Sensor.SensorState.SENDING
          && sensorDataSet.getPower().hasValue()) {
        power = sensorDataSet.getPower().getValue();
      }
    }

    if (data != null) {
      data[0] = timeOrDistance;
      data[1] = elevation;
      data[2] = speed;
      data[3] = pace;
      data[4] = heartRate;
      data[5] = cadence;
      data[6] = power;
    }
  }

  @VisibleForTesting
  ChartView getChartView() {
    return chartView;
  }

  @VisibleForTesting
  void setTripStatisticsUpdater(long time) {
    tripStatisticsUpdater = new TripStatisticsUpdater(time);
  }

  @VisibleForTesting
  void setChartView(ChartView view) {
    chartView = view;
  }

  @VisibleForTesting
  void setMetricUnits(boolean value) {
    metricUnits = value;
  }

  @VisibleForTesting
  void setReportSpeed(boolean value) {
    reportSpeed = value;
  }

  @VisibleForTesting
  void setChartByDistance(boolean value) {
    chartByDistance = value;
  } 
  public boolean onRecordingGpsAccuracy(int recordingGpsAccuracy) {
    // TODO Auto-generated method stub
    return false;
  }
 
 
}
