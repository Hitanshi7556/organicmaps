package app.organicmaps.settings;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import androidx.work.WorkManager;
import app.organicmaps.R;
import app.organicmaps.cloud.AutoBackupBookmarkListener;
import app.organicmaps.cloud.AutoBackupScheduler;
import app.organicmaps.cloud.BackupMetadataManager;
import app.organicmaps.cloud.BackupPreferences;
import app.organicmaps.sdk.bookmarks.data.BookmarkManager;

/**
 * Fragment for managing automatic backup settings.
 * 
 * Allows user to:
 * - Enable/disable automatic backups
 * - Set backup frequency (daily, weekly, monthly)
 * - Set preferred backup time
 * - View last backup status
 */
public class AutoBackupSettingsFragment extends BaseXmlSettingsFragment {
  private static final String TAG = "AutoBackupSettingsFragment";
  private static final String DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG";

  private BackupPreferences mBackupPrefs;
  private BackupMetadataManager mMetadataManager;
  private AutoBackupScheduler mBackupScheduler;
  private AutoBackupBookmarkListener mBookmarkListener;

  @Override
  protected int getXmlResources() {
    return R.xml.prefs_backup;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    Context context = requireContext();
    mBackupPrefs = BackupPreferences.getInstance(context);
    mMetadataManager = BackupMetadataManager.getInstance(context);
    mBackupScheduler = AutoBackupScheduler.getInstance(context);

    // Initialize bookmark listener for detecting changes
    if (mBookmarkListener == null) {
      mBookmarkListener = new AutoBackupBookmarkListener(context);
      // TODO: Register listener with BookmarkManager when API is available
    }

    setupAutoBackupPreferences();
    updateAutoBackupStatus();
  }

  /**
   * Setup listeners for auto-backup preferences.
   */
  private void setupAutoBackupPreferences() {
    // Auto-backup enabled/disabled toggle
    SwitchPreferenceCompat autoBackupSwitch = findPreference("pref_auto_backup_enabled");
    if (autoBackupSwitch != null) {
      autoBackupSwitch.setChecked(mBackupPrefs.isAutoBackupEnabled());
      autoBackupSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
        boolean enabled = (boolean) newValue;
        mBackupPrefs.setAutoBackupEnabled(enabled);
        if (enabled) {
          mBackupScheduler.scheduleBackups();
          Log.d(TAG, "Auto-backup enabled and scheduled");
        } else {
          mBackupScheduler.cancelBackups();
          Log.d(TAG, "Auto-backup disabled and cancelled");
        }
        updateAutoBackupStatus();
        return true;
      });
    }

    // Backup frequency selector
    ListPreference frequencyPref = findPreference("pref_backup_frequency");
    if (frequencyPref != null) {
      int frequencyValue = mBackupPrefs.getBackupFrequency().value;
      frequencyPref.setValue(String.valueOf(frequencyValue));
      frequencyPref.setOnPreferenceChangeListener((preference, newValue) -> {
        try {
          int value = Integer.parseInt((String) newValue);
          BackupPreferences.BackupFrequency frequency = BackupPreferences.BackupFrequency.fromValue(value);
          mBackupPrefs.setBackupFrequency(frequency);
          if (mBackupPrefs.isAutoBackupEnabled()) {
            mBackupScheduler.scheduleBackups();
            Log.d(TAG, "Backup frequency changed to: " + frequency);
          }
          updateFrequencyDisplay(frequencyPref, frequency);
        } catch (NumberFormatException e) {
          Log.e(TAG, "Invalid frequency value: " + newValue, e);
        }
        return true;
      });
      updateFrequencyDisplay(frequencyPref, mBackupPrefs.getBackupFrequency());
    }

    // Backup time selector
    TimePreference timePref = findPreference("pref_backup_time");
    if (timePref != null) {
      int hour = mBackupPrefs.getBackupTimeHour();
      timePref.setTimeInMinutes(hour * 60);
      timePref.setOnPreferenceChangeListener((preference, newValue) -> {
        // Time change is handled in the dialog, just reschedule if auto-backup is enabled
        if (mBackupPrefs.isAutoBackupEnabled()) {
          mBackupScheduler.scheduleBackups();
          Log.d(TAG, "Backup time changed");
        }
        return true;
      });
    }

    // Auto-backup status (read-only)
    Preference statusPref = findPreference("pref_auto_backup_status");
    if (statusPref != null) {
      statusPref.setSelectable(false);
    }
  }

  /**
   * Update the display of backup frequency preference.
   */
  private void updateFrequencyDisplay(ListPreference frequencyPref, BackupPreferences.BackupFrequency frequency) {
    String[] labels = getResources().getStringArray(R.array.backup_frequency_labels);
    frequencyPref.setSummary(labels[frequency.value]);
  }

  /**
   * Update auto-backup status display.
   */
  private void updateAutoBackupStatus() {
    Preference statusPref = findPreference("pref_auto_backup_status");
    if (statusPref == null) return;

    if (!mBackupPrefs.isAutoBackupEnabled()) {
      statusPref.setSummary("Auto-backup is disabled");
      return;
    }

    long lastBackupTime = mBackupPrefs.getLastBackupTime();
    String statusText;

    if (lastBackupTime == 0) {
      statusText = getString(R.string.auto_backup_never_run);
    } else {
      String lastBackupFormatted = mMetadataManager.getLastBackupTimeFormatted();
      statusText = getString(R.string.auto_backup_last_run, lastBackupFormatted);

      // Add file size if available
      long fileSize = mMetadataManager.getLastBackupFileSize();
      if (fileSize > 0) {
        String formattedSize = mMetadataManager.getFormattedFileSize(fileSize);
        statusText += " (" + formattedSize + ")";
      }
    }

    // Check for errors
    String lastError = mMetadataManager.getLastBackupError();
    if (lastError != null && !lastError.isEmpty()) {
      statusText += "\nLast error: " + lastError;
    }

    statusPref.setSummary(statusText);
  }

  @Override
  public void onDisplayPreferenceDialog(@NonNull Preference preference) {
    // Handle TimePreference dialog
    if (preference instanceof TimePreference) {
      PreferenceDialogFragmentCompat dialogFragment = TimePreferenceDialogFragmentCompat.newInstance(preference.getKey());
      dialogFragment.setTargetFragment(this, 0);
      dialogFragment.show(getParentFragmentManager(), DIALOG_FRAGMENT_TAG);
      return;
    }

    super.onDisplayPreferenceDialog(preference);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    // TODO: Unregister bookmark listener when destroyed
  }

  /**
   * Get string resource by name (for preference lookups).
   */
  @Nullable
  private <T extends Preference> T findPreference(String key) {
    return getPreferenceScreen().findPreference(key);
  }
}

