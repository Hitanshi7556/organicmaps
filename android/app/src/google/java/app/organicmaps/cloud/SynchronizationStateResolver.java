package app.organicmaps.cloud;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Core synchronization logic for bookmarks backup/restore - MVP Phase 2.
 * 
 * Determines what sync action to take based on local vs cloud state:
 * 1. Get local bookmark state (timestamp, md5)
 * 2. Get cloud backup state (from RemoteMetadata)
 * 3. Compare and return resolution (7 possible states)
 * 4. Handles empty cases, timestamp comparison, MD5 conflicts
 * 
 * Future phases will add:
 * - Deletion tracking (Phase 2+)
 * - UI notifications (Phase 3+)
 * - User conflict resolution UI (Phase 3+)
 */
public class SynchronizationStateResolver {
  private static final String TAG = "SyncStateResolver";

  /**
   * Resolve synchronization state between local and cloud bookmarks.
   * This is the main decision-making function for the sync algorithm.
   * 
   * @param localState Local bookmark metadata (null if device has no backup)
   * @param remoteState Cloud bookmark metadata (null if cloud has no backup)
   * @return One of 7 SyncState values
   */
  @NonNull
  public static SyncState resolveSyncState(
      @Nullable LocalMetadata localState,
      @Nullable RemoteMetadata remoteState) {

    // Case 1: Both empty → no sync needed
    if ((localState == null || localState.isEmpty()) && 
        (remoteState == null || remoteState.isEmpty())) {
      Log.d(TAG, "State: EMPTY_LOCAL_EMPTY_CLOUD");
      return SyncState.EMPTY_LOCAL_EMPTY_CLOUD;
    }

    // Case 2: Only local has bookmarks → upload needed
    if ((localState != null && !localState.isEmpty()) && 
        (remoteState == null || remoteState.isEmpty())) {
      Log.d(TAG, "State: LOCAL_ONLY (device has bookmarks, cloud empty)");
      return SyncState.LOCAL_ONLY;
    }

    // Case 3: Only cloud has bookmarks → download needed
    if ((localState == null || localState.isEmpty()) && 
        (remoteState != null && !remoteState.isEmpty())) {
      Log.d(TAG, "State: CLOUD_ONLY (device empty, cloud has bookmarks)");
      return SyncState.CLOUD_ONLY;
    }

    // Case 4: Both have data → compare timestamps and MD5
    return compareLocalAndCloud(localState, remoteState);
  }

  /**
   * Compare local and cloud when both have bookmarks.
   * Uses MD5 first (fast check), then timestamps.
   */
  @NonNull
  private static SyncState compareLocalAndCloud(
      @NonNull LocalMetadata local,
      @NonNull RemoteMetadata remote) {

    String localMd5 = local.getLocalMd5();
    String remoteMd5 = remote.getMd5();

    // FAST CHECK: If MD5 hashes match → content is identical
    if (!localMd5.isEmpty() && !remoteMd5.isEmpty() && localMd5.equals(remoteMd5)) {
      Log.d(TAG, "State: CONFLICT_IDENTICAL_MD5 (same content, no sync needed)");
      return SyncState.CONFLICT_IDENTICAL_MD5;
    }

    // MD5s don't match → content differs, compare timestamps
    long localTimestamp = local.getLocalTimestamp();
    long remoteTimestamp = remote.getUploadTimestamp();

    // Validate timestamps
    if (localTimestamp <= 0 || remoteTimestamp <= 0) {
      Log.w(TAG, "Invalid timestamp(s) - local: " + localTimestamp + 
                 ", remote: " + remoteTimestamp);
      return SyncState.CONFLICT_DIFFERENT_CONTENT;
    }

    // Local is newer → upload local changes
    if (localTimestamp > remoteTimestamp) {
      long timeDiffMs = localTimestamp - remoteTimestamp;
      Log.d(TAG, "State: LOCAL_NEWER (local is " + timeDiffMs + "ms ahead)");
      return SyncState.LOCAL_NEWER;
    }

    // Cloud is newer → download cloud changes
    if (remoteTimestamp > localTimestamp) {
      long timeDiffMs = remoteTimestamp - localTimestamp;
      Log.d(TAG, "State: CLOUD_NEWER (cloud is " + timeDiffMs + "ms ahead)");
      return SyncState.CLOUD_NEWER;
    }

    // Same timestamp AND different content → conflict
    Log.w(TAG, "State: CONFLICT_DIFFERENT_CONTENT (same time, different MD5)");
    return SyncState.CONFLICT_DIFFERENT_CONTENT;
  }

  /**
   * Get user-friendly description of sync state.
   */
  @NonNull
  public static String getRecommendedAction(@NonNull SyncState state) {
    switch (state) {
      case EMPTY_LOCAL_EMPTY_CLOUD:
        return "No bookmarks to sync";
      case LOCAL_ONLY:
        return "Backup bookmarks to Google Drive";
      case CLOUD_ONLY:
        return "Restore bookmarks from Google Drive";
      case CONFLICT_IDENTICAL_MD5:
        return "Bookmarks are in sync";
      case CONFLICT_DIFFERENT_CONTENT:
        return "Bookmarks conflict - please resolve manually";
      case LOCAL_NEWER:
        return "Local bookmarks are newer - backup to cloud";
      case CLOUD_NEWER:
        return "Cloud bookmarks are newer - restore from cloud";
      default:
        return "Unknown sync state";
    }
  }

  /**
   * Create LocalMetadata from current state.
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
   * Create RemoteMetadata from cloud state.
   */
  @NonNull
  public static RemoteMetadata createRemoteMetadata(
      @NonNull String fileId,
      long timestamp,
      @Nullable String md5,
      int bookmarkCount,
      long fileSize) {
    return new RemoteMetadata(
        fileId,
        timestamp,
        md5 != null ? md5 : "",
        bookmarkCount,
        fileSize
    );
  }

  /**
   * Format milliseconds as human-readable time difference.
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

