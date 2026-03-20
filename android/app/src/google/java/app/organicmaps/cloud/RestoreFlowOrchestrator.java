package app.organicmaps.cloud;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;

public class RestoreFlowOrchestrator {
  private static final String TAG = "RestoreFlowOrchestrator";

  private final Context mContext;
  private final BackupService mBackupService;
  private final RestoreService mRestoreService;
  private final MultiDeviceMetadata mMultiDeviceMetadata;

  public RestoreFlowOrchestrator(
      @NonNull Context context,
      @NonNull BackupService backupService,
      @NonNull RestoreService restoreService,
      @NonNull MultiDeviceMetadata multiDeviceMetadata) {
    mContext = context.getApplicationContext();
    mBackupService = backupService;
    mRestoreService = restoreService;
    mMultiDeviceMetadata = multiDeviceMetadata;
  }

  public void restoreWithSnapshot(@NonNull BackupVersionManager.BackupVersion selectedBackup) {
    Log.d(TAG, "Starting restore flow with pre-snapshot");
    Toast.makeText(mContext, "Saving current bookmarks as backup...", Toast.LENGTH_SHORT).show();

    new Thread(() -> {
      try {
        Log.d(TAG, "Uploading snapshot of current state...");
        BackupService.startBackup(mContext, null);
        Thread.sleep(2000);
        Log.d(TAG, "Snapshot uploaded successfully");

        Log.d(TAG, "Starting restore of selected backup...");
        RestoreService.startRestore(mContext, selectedBackup.revisionId, null);

        Toast.makeText(mContext, "Restore complete! Your current state is saved at top of version history.", Toast.LENGTH_LONG).show();

      } catch (Exception e) {
        Log.e(TAG, "Error during snapshot or restore", e);
        Toast.makeText(mContext, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
      }
    }).start();
  }

  public void restoreDirectly(@NonNull BackupVersionManager.BackupVersion selectedBackup) {
    Log.d(TAG, "Restoring directly without snapshot");
    Toast.makeText(mContext, "Restoring bookmarks...", Toast.LENGTH_SHORT).show();

    new Thread(() -> {
      try {
        RestoreService.startRestore(mContext, selectedBackup.revisionId, null);
        Toast.makeText(mContext, "Restore complete!", Toast.LENGTH_SHORT).show();
      } catch (Exception e) {
        Log.e(TAG, "Error during restore", e);
        Toast.makeText(mContext, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
      }
    }).start();
  }
}

