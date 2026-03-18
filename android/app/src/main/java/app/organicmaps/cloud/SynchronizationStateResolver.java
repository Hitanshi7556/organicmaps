package app.organicmaps.cloud;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Core synchronization logic for bookmarks backup/restore.
 * 
 * This is the heart of the GSoC project - determines what sync action to take
 * based on local vs cloud state. Ported from iOS logic with Android adaptations.
 * 
 * Algorithm:
 * 1. Get local bookmark state (file size, md5, timestamp)
 * 2. Get cloud backup state (list files from Drive)
 * 3. Compare states and return resolution
 * 4. Handles conflicts, incremental updates, and edge cases
 */
public class SynchronizationStateResolver {
  private static final String TAG = "SyncStateResolver";

  /**
   * Resolve synchronization state between local bookmarks and cloud backups.
   * This is the main decision-making function.
   */
  @NonNull
  public static SyncState resolveSyncState(
      @Nullable LocalMetadata localState,
      @Nullable List<GoogleDriveClient.FileMetadata> cloudFiles) {

    // Case 1: Both empty
    if ((localState == null || localState.isEmpty()) && 
        (cloudFiles == null || cloudFiles.isEmpty())) {
      Log.d(TAG, "State: EMPTY (no local backup, no cloud files)");
      return SyncState.EMPTY;
    }

    // Case 2: Only local has bookmarks
    if ((localState != null && !localState.isEmpty()) && 
        (cloudFiles == null || cloudFiles.isEmpty())) {
      Log.d(TAG, "State: LOCAL_ONLY (local exists, cloud empty)");
      return SyncState.LOCAL_ONLY;
    }

    // Case 3: Only cloud has backups
    if ((localState == null || localState.isEmpty()) && 
        (cloudFiles != null && !cloudFiles.isEmpty())) {
      Log.d(TAG, "State: CLOUD_ONLY (local empty, cloud exists)");
      return SyncState.CLOUD_ONLY;
    }

    // Case 4: Both have data - need to compare
    // Get the most recent cloud file
    GoogleDriveClient.FileMetadata mostRecentCloud = getMostRecentCloudFile(cloudFiles);
    if (mostRecentCloud == null) {
      return SyncState.CLOUD_ONLY;
    }

    return compareLocalAndCloud(localState, mostRecentCloud);
  }

  /**
   * Compare local state with most recent cloud backup to determine sync action.
   */
  @NonNull
  private static SyncState compareLocalAndCloud(
      @NonNull LocalMetadata local,
      @NonNull GoogleDriveClient.FileMetadata cloud) {

    // If MD5 checksums match, files are identical
    if (local.getLocalMd5().equals(cloud.md5Checksum)) {
      Log.d(TAG, "State: SYNCED (MD5 checksums match)");
      return SyncState.SYNCED;
    }

    // Files are different - check which is newer by timestamp
    long localTimestamp = local.getLocalTimestamp();
    long cloudTimestamp = cloud.modifiedTime;

    // Cloud file doesn't have valid timestamp
    if (cloudTimestamp <= 0) {
      Log.w(TAG, "Cloud file has invalid timestamp, treating as conflict");
      return SyncState.CONFLICT;
    }

    // Local is newer
    if (localTimestamp > cloudTimestamp) {
      long timeDiffMs = localTimestamp - cloudTimestamp;
      Log.d(TAG, "State: LOCAL_NEWER (local is " + timeDiffMs + "ms newer than cloud)");
      return SyncState.LOCAL_NEWER;
    }

    // Cloud is newer
    if (cloudTimestamp > localTimestamp) {
      long timeDiffMs = cloudTimestamp - localTimestamp;
      Log.d(TAG, "State: CLOUD_NEWER (cloud is " + timeDiffMs + "ms newer than local)");
      return SyncState.CLOUD_NEWER;
    }

    // Same timestamp but different content - conflict
    Log.w(TAG, "State: CONFLICT (same timestamp, different MD5)");
    return SyncState.CONFLICT;
  }

  /**
   * Get the most recent cloud backup file by modification time.
   * Returns null if no valid files found.
   */
  @Nullable
  private static GoogleDriveClient.FileMetadata getMostRecentCloudFile(
      @NonNull List<GoogleDriveClient.FileMetadata> files) {

    if (files.isEmpty()) {
      return null;
    }

    GoogleDriveClient.FileMetadata mostRecent = null;
    long latestTimestamp = -1;

    for (GoogleDriveClient.FileMetadata file : files) {
      if (file.modifiedTime > latestTimestamp) {
        latestTimestamp = file.modifiedTime;
        mostRecent = file;
      }
    }

    return mostRecent;
  }

  /**
   * Determine recommended action for user based on sync state.
   * Used to display messages in settings UI.
   */
  @NonNull
  public static String getRecommendedAction(@NonNull SyncState state) {
    switch (state) {
      case EMPTY:
        return "No bookmarks to backup";
      case LOCAL_ONLY:
        return "Backup bookmarks to Google Drive";
      case CLOUD_ONLY:
        return "Restore bookmarks from Google Drive";
      case SYNCED:
        return "Bookmarks are in sync";
      case LOCAL_NEWER:
        return "Local bookmarks are newer - consider backing up";
      case CLOUD_NEWER:
        return "Cloud bookmarks are newer - consider restoring";
      case CONFLICT:
        return "Bookmarks conflict - manual resolution needed";
      default:
        return "Unknown state";
    }
  }

  /**
   * Create LocalMetadata from current BookmarkManager state.
   * This captures the current local state for future comparisons.
   */
  @NonNull
  public static LocalMetadata createLocalMetadata(
      @Nullable String fileId,
      long timestamp,
      @Nullable String md5,
      long fileSize,
      int bookmarkCount) {

    return new LocalMetadata(
        fileId != null ? fileId : "",
        timestamp,
        md5 != null ? md5 : "",
        fileSize,
        bookmarkCount
    );
  }

  /**
   * Validate that sync states make sense.
   * Used in tests to verify resolver logic.
   */
  public static boolean isValidTransition(
      @NonNull SyncState previousState,
      @NonNull SyncState newState) {

    // SYNCED can transition to any state (files can be modified)
    if (previousState == SyncState.SYNCED) {
      return true;
    }

    // LOCAL_ONLY → SYNCED (after backup completes)
    if (previousState == SyncState.LOCAL_ONLY && newState == SyncState.SYNCED) {
      return true;
    }

    // CLOUD_ONLY → SYNCED (after restore completes)
    if (previousState == SyncState.CLOUD_ONLY && newState == SyncState.SYNCED) {
      return true;
    }

    // LOCAL_NEWER → SYNCED (after backup)
    if (previousState == SyncState.LOCAL_NEWER && newState == SyncState.SYNCED) {
      return true;
    }

    // CLOUD_NEWER → SYNCED (after restore)
    if (previousState == SyncState.CLOUD_NEWER && newState == SyncState.SYNCED) {
      return true;
    }

    // CONFLICT → SYNCED (after manual resolution)
    if (previousState == SyncState.CONFLICT && newState == SyncState.SYNCED) {
      return true;
    }

    // EMPTY → LOCAL_ONLY (bookmarks added locally)
    if (previousState == SyncState.EMPTY && newState == SyncState.LOCAL_ONLY) {
      return true;
    }

    // EMPTY → CLOUD_ONLY (can't happen - means cloud was never empty)
    if (previousState == SyncState.EMPTY && newState == SyncState.CLOUD_ONLY) {
      return false;
    }

    return false;
  }

  /**
   * Calculate time difference in human-readable format.
   */
  @NonNull
  public static String formatTimeDifference(long timeDiffMs) {
    long seconds = timeDiffMs / 1000;
    long minutes = seconds / 60;
    long hours = minutes / 60;
    long days = hours / 24;

    if (days > 0) {
      return days + " day(s) ago";
    } else if (hours > 0) {
      return hours + " hour(s) ago";
    } else if (minutes > 0) {
      return minutes + " minute(s) ago";
    } else {
      return seconds + " second(s) ago";
    }
  }
}

