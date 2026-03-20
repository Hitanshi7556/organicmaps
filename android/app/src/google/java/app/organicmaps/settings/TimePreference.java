package app.organicmaps.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import androidx.preference.DialogPreference;
import app.organicmaps.R;

/**
 * Custom preference for selecting a time value.
 * Shows a time picker dialog when clicked.
 * Stores value as "HH:mm" format (e.g., "02:00").
 */
public class TimePreference extends DialogPreference {
  private static final String TAG = "TimePreference";
  private static final int DEFAULT_VALUE = 2 * 60; // 2 AM in minutes

  private int mTimeInMinutes = DEFAULT_VALUE;

  public TimePreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setDialogLayoutResource(R.layout.dialog_time_picker);
    setPersistent(false);
  }

  /**
   * Get the currently selected time in minutes (0-1439).
   */
  public int getTimeInMinutes() {
    return mTimeInMinutes;
  }

  /**
   * Set the time in minutes (0-1439).
   */
  public void setTimeInMinutes(int timeInMinutes) {
    if (timeInMinutes < 0 || timeInMinutes >= 24 * 60) {
      throw new IllegalArgumentException("Time must be between 0 and 1439 minutes");
    }
    mTimeInMinutes = timeInMinutes;
    persistString(formatTime(timeInMinutes));
    notifyChanged();
  }

  /**
   * Get the currently selected hour (0-23).
   */
  public int getHour() {
    return mTimeInMinutes / 60;
  }

  /**
   * Get the currently selected minute (0-59).
   */
  public int getMinute() {
    return mTimeInMinutes % 60;
  }

  /**
   * Convert minutes to HH:mm format.
   */
  public static String formatTime(int timeInMinutes) {
    int hour = timeInMinutes / 60;
    int minute = timeInMinutes % 60;
    return String.format("%02d:%02d", hour, minute);
  }

  /**
   * Convert HH:mm format to minutes.
   */
  public static int parseTime(String timeString) {
    try {
      String[] parts = timeString.split(":");
      if (parts.length != 2) return DEFAULT_VALUE;
      int hour = Integer.parseInt(parts[0]);
      int minute = Integer.parseInt(parts[1]);
      if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
        return DEFAULT_VALUE;
      }
      return hour * 60 + minute;
    } catch (Exception e) {
      Log.e(TAG, "Error parsing time: " + timeString, e);
      return DEFAULT_VALUE;
    }
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return a.getString(index);
  }

  @Override
  protected void onSetInitialValue(Object defaultValue) {
    String value;
    if (defaultValue != null) {
      value = (String) defaultValue;
    } else {
      value = getPersistedString("02:00");
    }
    mTimeInMinutes = parseTime(value);
    setSummary(formatTime(mTimeInMinutes));
  }

  @Override
  public CharSequence getSummary() {
    return formatTime(mTimeInMinutes);
  }
}

