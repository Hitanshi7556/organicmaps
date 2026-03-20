package app.organicmaps.cloud;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

/**
 * Manages auto-backup preferences and settings.
 * 
 * Stores user preferences for automatic backup scheduling:
 * - Whether auto-backup is enabled
 * - Backup frequency (daily, weekly, monthly)
 * - Preferred backup time (hour in 24h format)
 * - Last successful backup timestamp
 */
public class BackupPreferences {
  private static final String PREF_AUTO_BACKUP_ENABLED = "auto_backup_enabled";
  private static final String PREF_BACKUP_FREQUENCY = "backup_frequency";
  private static final String PREF_BACKUP_TIME_HOUR = "backup_time_hour";
  private static final String PREF_LAST_BACKUP_TIME = "last_backup_time";
  private static final String PREF_BACKUP_NEEDED = "backup_needed";

  private static BackupPreferences sInstance;
  private final SharedPreferences mPrefs;

  public enum BackupFrequency {
    DAILY(0),
    WEEKLY(1),
    MONTHLY(2);

    public final int value;
    BackupFrequency(int value) {
      this.value = value;
    }

    public static BackupFrequency fromValue(int value) {
      for (BackupFrequency freq : values()) {
        if (freq.value == value) return freq;
      }
      return DAILY;
    }
  }

  private BackupPreferences(Context context) {
    mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public static synchronized BackupPreferences getInstance(Context context) {
    if (sInstance == null) {
      sInstance = new BackupPreferences(context);
    }
    return sInstance;
  }

  public boolean isAutoBackupEnabled() {
    return mPrefs.getBoolean(PREF_AUTO_BACKUP_ENABLED, false);
  }

  public void setAutoBackupEnabled(boolean enabled) {
    mPrefs.edit().putBoolean(PREF_AUTO_BACKUP_ENABLED, enabled).apply();
  }

  public BackupFrequency getBackupFrequency() {
    int value = mPrefs.getInt(PREF_BACKUP_FREQUENCY, BackupFrequency.DAILY.value);
    return BackupFrequency.fromValue(value);
  }

  public void setBackupFrequency(BackupFrequency frequency) {
    mPrefs.edit().putInt(PREF_BACKUP_FREQUENCY, frequency.value).apply();
  }

  public int getBackupTimeHour() {
    return mPrefs.getInt(PREF_BACKUP_TIME_HOUR, 2); // Default 2 AM
  }

  public void setBackupTimeHour(int hour) {
    if (hour < 0 || hour > 23) {
      throw new IllegalArgumentException("Hour must be between 0 and 23");
    }
    mPrefs.edit().putInt(PREF_BACKUP_TIME_HOUR, hour).apply();
  }

  public long getLastBackupTime() {
    return mPrefs.getLong(PREF_LAST_BACKUP_TIME, 0);
  }

  public void setLastBackupTime(long timestamp) {
    mPrefs.edit().putLong(PREF_LAST_BACKUP_TIME, timestamp).apply();
  }

  public boolean isBackupNeeded() {
    return mPrefs.getBoolean(PREF_BACKUP_NEEDED, false);
  }

  public void setBackupNeeded(boolean needed) {
    mPrefs.edit().putBoolean(PREF_BACKUP_NEEDED, needed).apply();
  }
}

