package app.organicmaps.cloud;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupService extends IntentService {
  private static final String TAG = "BackupService";
  private static final String ACTION_BACKUP = "app.organicmaps.cloud.action.BACKUP";
  private static final String BACKUPS_DIR = "bookmark_backups";
  private static final int BUFFER_SIZE = 8192;

  public interface BackupCallback {
    void onBackupSuccess(String driveFileId, long fileSize);
    void onBackupFailure(String errorMessage);
  }

  private static BackupCallback sCallback;

  public BackupService() {
    super("BackupService");
  }

  public static void startBackup(@NonNull Context context, @Nullable BackupCallback callback) {
    sCallback = callback;
    Intent intent = new Intent(context, BackupService.class);
    intent.setAction(ACTION_BACKUP);
    context.startService(intent);
  }

  @Override
  protected void onHandleIntent(@Nullable Intent intent) {
    if (intent == null) return;
    if (ACTION_BACKUP.equals(intent.getAction())) {
      performBackup();
    }
  }

  private void performBackup() {
    try {
      Log.d(TAG, "Starting backup...");

      GoogleDriveManager driveManager = GoogleDriveManager.getInstance(getApplicationContext());
      if (!driveManager.isSignedIn()) {
        Log.e(TAG, "User is not signed in");
        notifyFailure("User not signed in to Google");
        return;
      }

      // Check if OAuth scopes are granted
      if (!driveManager.hasGrantedScopes()) {
        Log.e(TAG, "User has not granted Drive API permissions");
        notifyFailure("Please re-authenticate and grant Drive API permissions");
        return;
      }

      GoogleAccountCredential credential = driveManager.getCredential();
      if (credential == null) {
        Log.e(TAG, "Credential is null - attempting to recreate");
        notifyFailure("Failed to get Google credentials");
        return;
      }

      Log.d(TAG, "Credential obtained successfully");

      GoogleDriveClient driveClient = new GoogleDriveClient(credential);

      File backupDir = new File(getCacheDir(), BACKUPS_DIR);
      backupDir.mkdirs();

      String timestamp = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(new Date());
      String backupName = "bookmarks_" + timestamp + ".json";
      String zipName = "bookmarks_" + timestamp + ".zip";

      File jsonFile = new File(backupDir, backupName);
      File zipFile = new File(backupDir, zipName);

      // CRITICAL FIX: Export bookmarks on MAIN thread (BookmarkManager requires it)
      // Use CountDownLatch to wait for main thread to complete
      Log.d(TAG, "Exporting bookmarks to JSON on main thread...");
      final AtomicBoolean exportSuccess = new AtomicBoolean(false);
      final CountDownLatch latch = new CountDownLatch(1);
      
      Handler mainHandler = new Handler(Looper.getMainLooper());
      mainHandler.post(() -> {
        try {
          exportSuccess.set(BookmarkBackupHelper.exportBookmarksToJson(jsonFile));
          Log.d(TAG, "Bookmark export completed on main thread");
        } catch (Exception e) {
          Log.e(TAG, "Error exporting bookmarks on main thread", e);
          exportSuccess.set(false);
        } finally {
          latch.countDown();
        }
      });
      
      // Wait for main thread to complete export
      try {
        latch.await();
      } catch (InterruptedException e) {
        Log.e(TAG, "Interrupted while waiting for bookmark export", e);
        notifyFailure("Backup interrupted");
        return;
      }
      
      if (!exportSuccess.get()) {
        notifyFailure("Failed to export bookmarks");
        return;
      }

      long jsonSize = jsonFile.length();
      Log.d(TAG, "JSON file size: " + jsonSize + " bytes");

      Log.d(TAG, "Compressing to ZIP...");
      if (!compressToZip(jsonFile, zipFile)) {
        notifyFailure("Failed to compress backup file");
        return;
      }

      long zipSize = zipFile.length();
      Log.d(TAG, "ZIP file size: " + zipSize + " bytes");

      Log.d(TAG, "Uploading to Google Drive...");
      String driveFileId = driveClient.uploadFile(zipFile.getAbsolutePath(), zipName);
      if (driveFileId == null) {
        notifyFailure("Failed to upload backup to Google Drive");
        return;
      }

      saveBackupMetadata(driveFileId, zipName, zipSize);
      jsonFile.delete();
      zipFile.delete();

      Log.d(TAG, "Backup completed successfully");
      notifySuccess(driveFileId, zipSize);

    } catch (Exception e) {
      Log.e(TAG, "Backup failed", e);
      notifyFailure("Backup failed: " + e.getMessage());
    }
  }

  private boolean compressToZip(@NonNull File inputFile, @NonNull File zipFile) {
    try (FileInputStream fis = new FileInputStream(inputFile);
         ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {

      ZipEntry entry = new ZipEntry(inputFile.getName());
      zos.putNextEntry(entry);

      byte[] buffer = new byte[BUFFER_SIZE];
      int len;
      while ((len = fis.read(buffer)) != -1) {
        zos.write(buffer, 0, len);
      }

      zos.closeEntry();
      return true;
    } catch (IOException e) {
      Log.e(TAG, "Failed to compress file", e);
      return false;
    }
  }

  private void saveBackupMetadata(@NonNull String driveFileId, @NonNull String fileName, long fileSize) {
    android.content.SharedPreferences prefs = getSharedPreferences("backup_metadata", Context.MODE_PRIVATE);
    prefs.edit()
        .putString("last_backup_file_id", driveFileId)
        .putString("last_backup_file_name", fileName)
        .putLong("last_backup_timestamp", System.currentTimeMillis())
        .putLong("last_backup_size", fileSize)
        .apply();
    Log.d(TAG, "Backup metadata saved");
  }

  private void notifySuccess(@NonNull String driveFileId, long fileSize) {
    // Post to main thread to avoid CalledFromWrongThreadException when updating UI
    Handler mainHandler = new Handler(Looper.getMainLooper());
    mainHandler.post(() -> {
      if (sCallback != null) {
        sCallback.onBackupSuccess(driveFileId, fileSize);
        sCallback = null;
      }
    });
  }

  private void notifyFailure(@NonNull String errorMessage) {
    Log.e(TAG, "Backup error: " + errorMessage);
    // Post to main thread to avoid CalledFromWrongThreadException when updating UI
    Handler mainHandler = new Handler(Looper.getMainLooper());
    mainHandler.post(() -> {
      if (sCallback != null) {
        sCallback.onBackupFailure(errorMessage);
        sCallback = null;
      }
    });
  }
}

