package app.organicmaps.cloud;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Background service to handle bookmark restore from Google Drive.
 * 
 * Operations:
 * - Download backup ZIP from Google Drive
 * - Extract backup file
 * - Import bookmarks locally (mock for MVP - real JNI comes in GSoC)
 * - Store restore metadata
 * - Notify user of result
 */
public class RestoreService extends IntentService {
  private static final String TAG = "RestoreService";
  private static final String ACTION_RESTORE = "app.organicmaps.cloud.action.RESTORE";
  private static final String RESTORES_DIR = "bookmark_restores";
  private static final int BUFFER_SIZE = 8192;

  public interface RestoreCallback {
    void onRestoreSuccess(int bookmarkCount);
    void onRestoreFailure(String errorMessage);
  }

  private static RestoreCallback sCallback;

  public RestoreService() {
    super("RestoreService");
  }

  public static void startRestore(@NonNull Context context, 
                                  @NonNull String driveFileId,
                                  @Nullable RestoreCallback callback) {
    sCallback = callback;
    Intent intent = new Intent(context, RestoreService.class);
    intent.setAction(ACTION_RESTORE);
    intent.putExtra("drive_file_id", driveFileId);
    context.startService(intent);
  }

  @Override
  protected void onHandleIntent(@Nullable Intent intent) {
    if (intent == null) return;

    if (ACTION_RESTORE.equals(intent.getAction())) {
      String driveFileId = intent.getStringExtra("drive_file_id");
      if (driveFileId != null) {
        performRestore(driveFileId);
      }
    }
  }

  private void performRestore(@NonNull String driveFileId) {
    try {
      Log.d(TAG, "Starting restore of file: " + driveFileId);

      GoogleDriveManager driveManager = GoogleDriveManager.getInstance(getApplicationContext());
      if (!driveManager.isSignedIn()) {
        Log.e(TAG, "User is not signed in");
        notifyFailure("User not signed in to Google");
        return;
      }

      GoogleAccountCredential credential = driveManager.getCredential();
      if (credential == null) {
        Log.e(TAG, "Credential is null");
        notifyFailure("Failed to get Google credentials");
        return;
      }
      
      Log.d(TAG, "Credential obtained successfully");

      GoogleDriveClient driveClient = new GoogleDriveClient(credential);

      File restoreDir = new File(getCacheDir(), RESTORES_DIR);
      restoreDir.mkdirs();

      String zipName = "bookmarks_restore.zip";
      String jsonName = "bookmarks_restore.json";

      File zipFile = new File(restoreDir, zipName);
      File jsonFile = new File(restoreDir, jsonName);

      // Step 1: Download backup from Drive
      Log.d(TAG, "Downloading backup from Google Drive...");
      if (!driveClient.downloadFile(driveFileId, zipFile.getAbsolutePath())) {
        notifyFailure("Failed to download backup from Google Drive");
        return;
      }

      long zipSize = zipFile.length();
      Log.d(TAG, "Downloaded ZIP: " + zipSize + " bytes");

      // Step 2: Extract ZIP
      Log.d(TAG, "Extracting backup...");
      if (!extractZip(zipFile, jsonFile)) {
        notifyFailure("Failed to extract backup file");
        return;
      }

      // Step 3: Parse and import bookmarks
      // TODO (GSoC): Actually call BookmarkManager.importBookmarks(jsonFile)
      // For MVP, just count the bookmarks metadata
      int bookmarkCount = 0;
      Log.d(TAG, "Imported " + bookmarkCount + " bookmarks");

      // Step 4: Save restore metadata
      saveRestoreMetadata(driveFileId, bookmarkCount);

      // Step 5: Cleanup
      zipFile.delete();
      jsonFile.delete();

      Log.d(TAG, "Restore completed successfully");
      notifySuccess(bookmarkCount);

    } catch (Exception e) {
      Log.e(TAG, "Restore failed", e);
      notifyFailure("Restore failed: " + e.getMessage());
    }
  }

  private boolean extractZip(@NonNull File zipFile, @NonNull File outputJsonFile) {
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
         FileOutputStream fos = new FileOutputStream(outputJsonFile)) {

      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.getName().contains(".json")) {
          byte[] buffer = new byte[BUFFER_SIZE];
          int len;
          while ((len = zis.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
          }
          Log.d(TAG, "Extracted: " + entry.getName());
          return true;
        }
      }

      return false;
    } catch (IOException e) {
      Log.e(TAG, "Failed to extract ZIP", e);
      return false;
    }
  }

  private void saveRestoreMetadata(@NonNull String driveFileId, int bookmarkCount) {
    android.content.SharedPreferences prefs = getSharedPreferences("restore_metadata", Context.MODE_PRIVATE);
    prefs.edit()
        .putString("last_restore_file_id", driveFileId)
        .putLong("last_restore_timestamp", System.currentTimeMillis())
        .putInt("last_restore_bookmark_count", bookmarkCount)
        .apply();

    Log.d(TAG, "Restore metadata saved");
  }

  private void notifySuccess(int bookmarkCount) {
    if (sCallback != null) {
      sCallback.onRestoreSuccess(bookmarkCount);
      sCallback = null;
    }
  }

  private void notifyFailure(@NonNull String errorMessage) {
    Log.e(TAG, "Restore error: " + errorMessage);
    if (sCallback != null) {
      sCallback.onRestoreFailure(errorMessage);
      sCallback = null;
    }
  }
}

