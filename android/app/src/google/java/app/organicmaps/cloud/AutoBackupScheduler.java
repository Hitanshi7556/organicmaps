package app.organicmaps.cloud;

import android.content.Context;
import android.util.Log;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Manages automatic backup scheduling using AndroidX WorkManager.
 * 
 * Handles scheduling, updating, and cancelling periodic backup jobs.
 * Supports different frequencies: daily, weekly, monthly.
 */
public class AutoBackupScheduler {
  private static final String TAG = "AutoBackupScheduler";
  private static final String WORK_TAG_AUTO_BACKUP = "auto_backup_work";
  private static final long MIN_INTERVAL_MINUTES = 15; // WorkManager minimum is 15 minutes

  private static AutoBackupScheduler sInstance;
  private final Context mContext;
  private final BackupPreferences mPrefs;

  private AutoBackupScheduler(Context context) {
    mContext = context.getApplicationContext();
    mPrefs = BackupPreferences.getInstance(mContext);
  }

  public static synchronized AutoBackupScheduler getInstance(Context context) {
    if (sInstance == null) {
      sInstance = new AutoBackupScheduler(context);
    }
    return sInstance;
  }

  /**
   * Schedule automatic backups based on user preferences.
   * Replaces any existing backup work schedule.
   */
  public void scheduleBackups() {
    if (!mPrefs.isAutoBackupEnabled()) {
      Log.d(TAG, "Auto-backup is disabled, cancelling any scheduled work");
      cancelBackups();
      return;
    }

    BackupPreferences.BackupFrequency frequency = mPrefs.getBackupFrequency();
    int preferredHour = mPrefs.getBackupTimeHour();
    long intervalMinutes = getIntervalMinutes(frequency);

    Log.d(TAG, "Scheduling backups: frequency=" + frequency + ", interval=" + intervalMinutes + " min, hour=" + preferredHour);

    // Create periodic backup work
    PeriodicWorkRequest backupWork = new PeriodicWorkRequest.Builder(
        BackupWorker.class,
        intervalMinutes,
        TimeUnit.MINUTES
    )
        .setConstraints(createBackupConstraints())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15L, TimeUnit.MINUTES)
        .addTag(WORK_TAG_AUTO_BACKUP)
        .build();

    // Schedule with replacement policy to avoid duplicate schedules
    WorkManager.getInstance(mContext).enqueueUniquePeriodicWork(
        WORK_TAG_AUTO_BACKUP,
        ExistingPeriodicWorkPolicy.REPLACE,
        backupWork
    );

    Log.d(TAG, "Automatic backups scheduled successfully");
  }

  /**
   * Cancel all scheduled automatic backups.
   */
  public void cancelBackups() {
    Log.d(TAG, "Cancelling automatic backups");
    WorkManager.getInstance(mContext).cancelAllWorkByTag(WORK_TAG_AUTO_BACKUP);
  }

  /**
   * Get interval in minutes based on backup frequency.
   * Note: WorkManager minimum interval is 15 minutes.
   */
  private long getIntervalMinutes(BackupPreferences.BackupFrequency frequency) {
    switch (frequency) {
      case DAILY:
        return 24 * 60; // 24 hours
      case WEEKLY:
        return 7 * 24 * 60; // 7 days
      case MONTHLY:
        return 30 * 24 * 60; // 30 days
      default:
        return 24 * 60; // Default to daily
    }
  }

  /**
   * Create constraints for backup work.
   * - Requires network connectivity
   * - Respects device battery saver mode
   * - Prefers charging to preserve battery
   */
  private Constraints createBackupConstraints() {
    return new Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build();
  }

  /**
   * Calculate the delay until the next preferred backup time.
   * This is used for scheduling the first backup at a specific time.
   */
  public long getDelayUntilPreferredTime() {
    int preferredHour = mPrefs.getBackupTimeHour();
    Calendar now = Calendar.getInstance();
    Calendar next = Calendar.getInstance();

    // Set target time to preferred hour, minute 0
    next.set(Calendar.HOUR_OF_DAY, preferredHour);
    next.set(Calendar.MINUTE, 0);
    next.set(Calendar.SECOND, 0);

    // If target time has passed today, schedule for tomorrow
    if (next.before(now)) {
      next.add(Calendar.DAY_OF_MONTH, 1);
    }

    long delayMs = next.getTimeInMillis() - now.getTimeInMillis();
    long delayMinutes = TimeUnit.MILLISECONDS.toMinutes(delayMs);

    Log.d(TAG, "Delay until preferred backup time: " + delayMinutes + " minutes");
    return delayMinutes;
  }

  /**
   * Check if backups are currently scheduled.
   */
  public boolean isBackupScheduled() {
    // This would require additional implementation to query WorkManager state
    // For now, we can check the preference
    return mPrefs.isAutoBackupEnabled();
  }
}

