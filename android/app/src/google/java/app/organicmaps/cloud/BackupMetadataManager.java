package app.organicmaps.cloud;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Manages backup metadata and history.
 * 
 * Tracks backup success/failure history and metadata:
 * - Last successful backup file ID and size
 * - Last backup error message
 * - Backup attempt count and timestamps
 */
public class BackupMetadataManager {
  private static final String PREF_LAST_BACKUP_FILE_ID = "last_backup_file_id";
  private static final String PREF_LAST_BACKUP_FILE_SIZE = "last_backup_file_size";
  private static final String PREF_LAST_BACKUP_ERROR = "last_backup_error";
  private static final String PREF_LAST_BACKUP_ERROR_TIME = "last_backup_error_time";
  private static final String PREF_BACKUP_ATTEMPT_COUNT = "backup_attempt_count";

  private static BackupMetadataManager sInstance;
  private final SharedPreferences mPrefs;

  private BackupMetadataManager(Context context) {
    mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public static synchronized BackupMetadataManager getInstance(Context context) {
    if (sInstance == null) {
      sInstance = new BackupMetadataManager(context);
    }
    return sInstance;
  }

  /**
   * Record a successful backup.
   */
  public void recordBackupSuccess(String driveFileId, long fileSize) {
    mPrefs.edit()
        .putString(PREF_LAST_BACKUP_FILE_ID, driveFileId)
        .putLong(PREF_LAST_BACKUP_FILE_SIZE, fileSize)
        .putString(PREF_LAST_BACKUP_ERROR, null)
        .putLong(PREF_LAST_BACKUP_ERROR_TIME, 0)
        .apply();

    BackupPreferences.getInstance(null).setLastBackupTime(System.currentTimeMillis());
    BackupPreferences.getInstance(null).setBackupNeeded(false);
  }

  /**
   * Record a backup failure.
   */
  public void recordBackupFailure(String errorMessage, Exception exception) {
    String fullError = errorMessage;
    if (exception != null) {
      fullError = errorMessage + " - " + exception.getMessage();
    }

    mPrefs.edit()
        .putString(PREF_LAST_BACKUP_ERROR, fullError)
        .putLong(PREF_LAST_BACKUP_ERROR_TIME, System.currentTimeMillis())
        .apply();

    int attemptCount = mPrefs.getInt(PREF_BACKUP_ATTEMPT_COUNT, 0);
    mPrefs.edit().putInt(PREF_BACKUP_ATTEMPT_COUNT, attemptCount + 1).apply();
  }

  public String getLastBackupFileId() {
    return mPrefs.getString(PREF_LAST_BACKUP_FILE_ID, null);
  }

  public long getLastBackupFileSize() {
    return mPrefs.getLong(PREF_LAST_BACKUP_FILE_SIZE, 0);
  }

  public String getLastBackupError() {
    return mPrefs.getString(PREF_LAST_BACKUP_ERROR, null);
  }

  public long getLastBackupErrorTime() {
    return mPrefs.getLong(PREF_LAST_BACKUP_ERROR_TIME, 0);
  }

  public int getBackupAttemptCount() {
    return mPrefs.getInt(PREF_BACKUP_ATTEMPT_COUNT, 0);
  }

  /**
   * Format last backup time as human-readable string.
   * Returns "Never" if no backup has been performed.
   */
  public String getLastBackupTimeFormatted() {
    long lastBackupTime = BackupPreferences.getInstance(null).getLastBackupTime();
    if (lastBackupTime == 0) {
      return "Never";
    }
    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    return sdf.format(new Date(lastBackupTime));
  }

  /**
   * Get formatted file size in human-readable format (e.g., "1.2 MB").
   */
  public String getFormattedFileSize(long bytes) {
    if (bytes <= 0) return "0 B";
    final String[] units = new String[]{"B", "KB", "MB", "GB"};
    int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
    return String.format(Locale.US, "%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
  }

  /**
   * Clear all backup metadata.
   */
  public void clearMetadata() {
    mPrefs.edit()
        .remove(PREF_LAST_BACKUP_FILE_ID)
        .remove(PREF_LAST_BACKUP_FILE_SIZE)
        .remove(PREF_LAST_BACKUP_ERROR)
        .remove(PREF_LAST_BACKUP_ERROR_TIME)
        .remove(PREF_BACKUP_ATTEMPT_COUNT)
        .apply();
  }
}

