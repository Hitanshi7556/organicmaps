package app.organicmaps.settings;

import android.os.Bundle;
import android.widget.TimePicker;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceDialogFragmentCompat;
import app.organicmaps.R;

/**
 * Dialog fragment for TimePreference.
 * Displays a time picker and saves the selected time.
 */
public class TimePreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {
  private TimePicker mTimePicker;

  public static TimePreferenceDialogFragmentCompat newInstance(String key) {
    TimePreferenceDialogFragmentCompat fragment = new TimePreferenceDialogFragmentCompat();
    Bundle b = new Bundle(1);
    b.putString("key", key);
    fragment.setArguments(b);
    return fragment;
  }

  @Override
  protected void onBindDialogView(android.view.View view) {
    super.onBindDialogView(view);
    mTimePicker = view.findViewById(R.id.time_picker);

    TimePreference preference = (TimePreference) getPreference();
    mTimePicker.setHour(preference.getHour());
    mTimePicker.setMinute(preference.getMinute());
  }

  @Override
  public void onDialogClosed(boolean positiveResult) {
    if (positiveResult) {
      int hour = mTimePicker.getHour();
      int minute = mTimePicker.getMinute();
      int timeInMinutes = hour * 60 + minute;

      TimePreference preference = (TimePreference) getPreference();
      preference.setTimeInMinutes(timeInMinutes);
    }
  }
}

