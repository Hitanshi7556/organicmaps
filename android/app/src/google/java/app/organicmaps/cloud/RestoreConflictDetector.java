package app.organicmaps.cloud;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RestoreConflictDetector {
  private static final String TAG = "RestoreConflictDetector";

  public static void checkAndPromptForRestore(
      @NonNull Context context,
      long localLastModified,
      @NonNull BackupVersionManager.BackupVersion selectedBackup,
      @NonNull RestoreFlowOrchestrator restoreOrchestrator) {

    long backupTimestamp = selectedBackup.timestamp;

    Log.d(TAG, "Checking conflict: local=" + localLastModified + ", backup=" + backupTimestamp);

    if (localLastModified <= backupTimestamp) {
      Log.d(TAG, "No conflict detected - restoring immediately");
      restoreOrchestrator.restoreDirectly(selectedBackup);
      return;
    }

    Log.d(TAG, "Conflict detected - showing dialog");
    showConflictDialog(context, localLastModified, selectedBackup, restoreOrchestrator);
  }

  private static void showConflictDialog(
      @NonNull Context context,
      long localLastModified,
      @NonNull BackupVersionManager.BackupVersion selectedBackup,
      @NonNull RestoreFlowOrchestrator restoreOrchestrator) {

    String localDateStr = formatTimestamp(localLastModified);
    String backupDateStr = selectedBackup.formattedDate;

    String message = String.format(
        "You have local bookmarks modified on %s.\n" +
        "The backup is from %s (%d bookmarks).\n\n" +
        "What would you like to do?",
        localDateStr, backupDateStr, selectedBackup.bookmarkCount);

    new MaterialAlertDialogBuilder(context)
        .setTitle("Local Changes Detected")
        .setMessage(message)
        .setNegativeButton("Cancel", (dialog, which) -> {
          Log.d(TAG, "User cancelled restore");
          dialog.dismiss();
        })
        .setPositiveButton("Restore", (dialog, which) -> {
          Log.d(TAG, "User chose restore - showing confirmation");
          showRestoreConfirmation(context, selectedBackup, restoreOrchestrator);
        })
        .setCancelable(false)
        .show();
  }

  private static void showRestoreConfirmation(
      @NonNull Context context,
      @NonNull BackupVersionManager.BackupVersion selectedBackup,
      @NonNull RestoreFlowOrchestrator restoreOrchestrator) {

    String confirmMessage = "Your current bookmarks will be saved as a backup first, then the selected version will be restored. You can always come back to your current state from backup history.";

    new MaterialAlertDialogBuilder(context)
        .setTitle("Confirm Restore")
        .setMessage(confirmMessage)
        .setNegativeButton("Cancel", (dialog, which) -> {
          Log.d(TAG, "User cancelled restore confirmation");
          dialog.dismiss();
        })
        .setPositiveButton("Restore", (dialog, which) -> {
          Log.d(TAG, "User confirmed restore - starting snapshot + restore");
          restoreOrchestrator.restoreWithSnapshot(selectedBackup);
        })
        .setCancelable(false)
        .show();
  }

  private static String formatTimestamp(long timestamp) {
    if (timestamp == 0) return "unknown date";
    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
    return sdf.format(new Date(timestamp));
  }
}

