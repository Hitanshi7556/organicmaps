package app.organicmaps.cloud;

import android.content.Context;
import android.util.Log;

/**
 * Initializer for automatic backup system.
 * Called when the app starts to set up auto-backup infrastructure.
 */
public class AutoBackupInitializer {
  private static final String TAG = "AutoBackupInitializer";
  private static boolean sInitialized = false;

  /**
   * Initialize the auto-backup system.
   * Should be called once when the app starts.
   */
  public static synchronized void initialize(Context context) {
    if (sInitialized) {
      Log.d(TAG, "Auto-backup already initialized");
      return;
    }

    try {
      Log.d(TAG, "Initializing auto-backup system...");

      BackupPreferences prefs = BackupPreferences.getInstance(context);
      AutoBackupScheduler scheduler = AutoBackupScheduler.getInstance(context);

      // If auto-backup is enabled, schedule backups
      if (prefs.isAutoBackupEnabled()) {
        Log.d(TAG, "Auto-backup is enabled, scheduling backups");
        scheduler.scheduleBackups();
      } else {
        Log.d(TAG, "Auto-backup is disabled");
      }

      // TODO: Register BookmarkChangeListener with BookmarkManager
      // Once BookmarkManager API is available to register listeners

      Log.d(TAG, "Auto-backup system initialized successfully");
      sInitialized = true;

    } catch (Exception e) {
      Log.e(TAG, "Error initializing auto-backup system", e);
    }
  }

  /**
   * Check if auto-backup system is initialized.
   */
  public static boolean isInitialized() {
    return sInitialized;
  }

  /**
   * Reset initialization (mainly for testing).
   */
  public static void reset() {
    sInitialized = false;
  }
}

