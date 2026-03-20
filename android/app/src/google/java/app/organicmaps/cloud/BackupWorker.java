package app.organicmaps.cloud;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker for periodic automatic bookmark backups to Google Drive.
 * 
 * This worker is scheduled using androidx.work library and runs at the configured
 * frequency (daily, weekly, or monthly). It respects network and battery constraints.
 */
public class BackupWorker extends Worker {
  private static final String TAG = "BackupWorker";

  public BackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
    super(context, params);
  }

  @NonNull
  @Override
  public Result doWork() {
    Log.d(TAG, "Starting automatic backup work...");
    
    BackupPreferences prefs = BackupPreferences.getInstance(getApplicationContext());
    
    // Check if auto-backup is enabled
    if (!prefs.isAutoBackupEnabled()) {
      Log.d(TAG, "Auto-backup is disabled");
      return Result.success();
    }

    GoogleDriveManager driveManager = GoogleDriveManager.getInstance(getApplicationContext());
    
    // Check if user is signed in
    if (!driveManager.isSignedIn()) {
      Log.w(TAG, "User is not signed in, skipping backup");
      BackupMetadataManager.getInstance(getApplicationContext())
          .recordBackupFailure("User not signed in to Google", null);
      // Don't retry - user needs to sign in manually
      return Result.failure();
    }

    // Check if OAuth scopes are granted
    if (!driveManager.hasGrantedScopes()) {
      Log.w(TAG, "User has not granted Drive API permissions");
      BackupMetadataManager.getInstance(getApplicationContext())
          .recordBackupFailure("Drive API permissions not granted", null);
      // Don't retry - user needs to re-authenticate
      return Result.failure();
    }

    // Perform the backup
    AtomicBoolean success = new AtomicBoolean(false);
    CountDownLatch latch = new CountDownLatch(1);
    
    BackupService.BackupCallback callback = new BackupService.BackupCallback() {
      @Override
      public void onBackupSuccess(String driveFileId, long fileSize) {
        Log.d(TAG, "Backup succeeded: " + driveFileId + " (" + fileSize + " bytes)");
        success.set(true);
        BackupMetadataManager.getInstance(getApplicationContext())
            .recordBackupSuccess(driveFileId, fileSize);
        latch.countDown();
      }

      @Override
      public void onBackupFailure(String errorMessage) {
        Log.e(TAG, "Backup failed: " + errorMessage);
        BackupMetadataManager.getInstance(getApplicationContext())
            .recordBackupFailure(errorMessage, null);
        latch.countDown();
      }
    };

    // Start backup on main thread (BackupService requirement)
    Handler handler = new Handler(Looper.getMainLooper());
    handler.post(() -> {
      BackupService.startBackup(getApplicationContext(), callback);
    });

    try {
      // Wait for backup to complete (with timeout)
      if (!latch.await(30, java.util.concurrent.TimeUnit.MINUTES)) {
        Log.e(TAG, "Backup timed out");
        return Result.retry();
      }
    } catch (InterruptedException e) {
      Log.e(TAG, "Backup interrupted", e);
      Thread.currentThread().interrupt();
      return Result.retry();
    }

    if (success.get()) {
      Log.d(TAG, "Automatic backup completed successfully");
      return Result.success();
    } else {
      Log.w(TAG, "Backup failed, will retry");
      return Result.retry();
    }
  }
}

